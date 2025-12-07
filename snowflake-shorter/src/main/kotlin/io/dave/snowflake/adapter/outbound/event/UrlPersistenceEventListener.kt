package io.dave.snowflake.adapter.outbound.event

import io.dave.snowflake.application.event.ShortUrlCreatedEvent
import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.port.outbound.DeadLetterQueuePort
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.dave.snowflake.domain.util.RetryResult
import io.dave.snowflake.domain.util.retryWithExponentialBackoffCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * ShortUrlCreatedEvent 이벤트를 수신하여 비동기적으로 URL 매핑을 영속화하는 리스너입니다. 이 리스너는 DB 적재 로직을 메인 요청 처리 흐름에서 분리하여
 * 응답 시간을 최소화하고, 배치 처리를 통해 DB 부하를 줄입니다.
 *
 * **개선 사항**:
 * - Exponential Backoff Retry: 일시적 장애에 대한 자동 재시도
 * - Dead Letter Queue: 재시도 실패 시 DLQ에 저장하여 데이터 손실 방지
 * - 메트릭: 성공/실패 카운터로 모니터링 강화
 */
@Component
class UrlPersistenceEventListener(
        private val urlPort: UrlPort,
        private val deadLetterQueuePort: DeadLetterQueuePort,
        private val meterRegistry: MeterRegistry,
        @Qualifier("virtualThreadDispatcher") private val dispatcher: CoroutineDispatcher
) {
    private val logger = KotlinLogging.logger {}
    private val eventProcessingScope = CoroutineScope(dispatcher) // Virtual Thread 디스패처에서 이벤트 처리
    private val eventChannel = Channel<ShortUrlCreatedEvent>(Channel.UNLIMITED) // 무제한 버퍼를 가진 채널

    // 메트릭 카운터
    private val successCounter: Counter =
            Counter.builder("url.persistence.success")
                    .description("Number of successfully persisted URL batches")
                    .register(meterRegistry)

    private val failureCounter: Counter =
            Counter.builder("url.persistence.failure")
                    .description("Number of failed URL batch persistence attempts")
                    .register(meterRegistry)

    private val dlqCounter: Counter =
            Counter.builder("url.persistence.dlq")
                    .description("Number of events sent to Dead Letter Queue")
                    .register(meterRegistry)

    init {
        eventProcessingScope.launch {
            val batch = mutableListOf<io.dave.snowflake.domain.model.UrlMapping>()
            val batchSize = 500
            val flushInterval = 100L // 100ms

            while (true) {
                // 1. 이벤트 수신 시도 (타임아웃 적용)
                val event = withTimeoutOrNull(flushInterval) { eventChannel.receive() }

                // 2. 이벤트가 있으면 배치에 추가
                if (event != null) {
                    batch.add(event.toDomain())
                }

                // 3. 배치가 가득 찼거나, 타임아웃이 발생했는데 처리할 데이터가 있는 경우 저장
                if (batch.size >= batchSize || (event == null && batch.isNotEmpty())) {
                    flush(batch)
                    batch.clear()
                }
            }
        }
    }

    /**
     * 배치를 DB에 저장합니다. Exponential Backoff Retry를 사용하여 일시적 장애에 대응하고, 모든 재시도가 실패하면 Dead Letter Queue에
     * 저장합니다.
     */
    private suspend fun flush(batch: List<io.dave.snowflake.domain.model.UrlMapping>) {
        // Exponential Backoff Retry 적용 (최대 3회, 초기 지연 100ms, 최대 지연 5초)
        val result =
                retryWithExponentialBackoffCatching(
                        maxAttempts = 3,
                        initialDelayMillis = 100,
                        maxDelayMillis = 5000,
                        factor = 2.0
                ) {
                    // saveAll은 Flow를 받아 Flow를 반환하므로, toList()로 수집(실행)해야 함
                    urlPort.saveAll(batch.asFlow()).toList()
                }

        when (result) {
            is RetryResult.Success -> {
                val savedMappings = result.value
                logger.info {
                    "[Event] Successfully persisted ${savedMappings.size} short URLs in batch."
                }
                successCounter.increment()
            }
            is RetryResult.Failure -> {
                logger.error(result.exception) {
                    "[Event Error] Failed to persist batch after ${result.attempts} attempts. Count: ${batch.size}. Sending to DLQ..."
                }
                failureCounter.increment()

                // 실패한 이벤트들을 Dead Letter Queue에 저장
                sendToDeadLetterQueue(batch, result.exception)
            }
        }
    }

    /** 실패한 배치를 Dead Letter Queue에 저장합니다. */
    private suspend fun sendToDeadLetterQueue(
            batch: List<io.dave.snowflake.domain.model.UrlMapping>,
            exception: Exception
    ) {
        try {
            val failedEvents =
                    batch.map { mapping ->
                        FailedEvent(
                                shortUrl = mapping.shortUrl,
                                longUrl = mapping.longUrl,
                                createdAt = mapping.createdAt,
                                failedAt = System.currentTimeMillis(),
                                retryCount = 0,
                                lastError = exception.message ?: "Unknown error"
                        )
                    }

            // DLQ에 배치 저장
            deadLetterQueuePort.saveAll(failedEvents.asFlow()).toList()

            dlqCounter.increment(batch.size.toDouble())
            logger.info { "[DLQ] Saved ${batch.size} failed events to Dead Letter Queue." }
        } catch (dlqException: Exception) {
            // DLQ 저장마저 실패한 경우 - 심각한 상황
            logger.error(dlqException) {
                "[DLQ Error] CRITICAL: Failed to save events to Dead Letter Queue. Data may be lost! Count: ${batch.size}"
            }
            // 여기서는 로깅만 하고, 추가적인 알림 시스템(예: Slack, PagerDuty)으로 에스컬레이션 필요
        }
    }

    @EventListener
    fun handleShortUrlCreatedEvent(event: ShortUrlCreatedEvent) {
        // 이벤트 채널로 전송. 채널 버퍼가 가득 차면 일시적으로 백프레셔 발생 (여기서는 UNLIMITED이므로 거의 발생 안 함)
        if (!eventChannel.trySend(event).isSuccess) {
            logger.warn {
                "[Event] Failed to send event to channel, buffer might be full or closed."
            }
        }
    }

    private fun ShortUrlCreatedEvent.toDomain() =
            io.dave.snowflake.domain.model.UrlMapping(
                    shortUrl = this.shortUrl,
                    longUrl = this.longUrl,
                    createdAt = this.createdAt
            )
}
