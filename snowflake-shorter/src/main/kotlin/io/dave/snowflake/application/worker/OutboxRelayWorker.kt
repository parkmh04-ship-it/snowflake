package io.dave.snowflake.application.worker

import io.dave.snowflake.config.IOX
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.OutboundEventPort
import io.dave.snowflake.domain.port.outbound.OutboxPort
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Outbox 테이블을 폴링하여 처리되지 않은 이벤트를 실제 DB(short_urls)로 이관하는 Relay Worker입니다. 이 워커는 애플리케이션 장애 시에도 데이터
 * 유실을 방지하고 "최소 한 번 이상의 처리(At-least-once)"를 보장합니다.
 */
@Component
class OutboxRelayWorker(
    private val outboxPort: OutboxPort,
    private val urlPort: UrlPort,
    private val outboundEventPort: OutboundEventPort
) {
    private val logger = KotlinLogging.logger {}
    private val isRunning = AtomicBoolean(false)

    // 매 틱 새 CoroutineScope를 만들면 Job이 누적되므로, 워커 생명주기에 묶인 단일 스코프를 재사용한다.
    private val scope = CoroutineScope(Dispatchers.IOX + SupervisorJob())

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    /** 주기적으로 처리되지 않은 Outbox 이벤트를 가져와 처리합니다. */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    fun processOutboxEvents() {
        if (!isRunning.compareAndSet(false, true)) return

        scope.launch {
            try {
                val events = outboxPort.findUnprocessedEvents(limit = 100)
                if (events.isEmpty()) return@launch

                logger.info { "[Outbox] Found ${events.size} unprocessed events. Relaying..." }

                // 디코딩 가능한 이벤트와 손상된(poison) 이벤트를 분리한다.
                val decodable = mutableListOf<UrlMapping>()
                val poisonIds = mutableListOf<Long>()
                for (event in events) {
                    val mapping =
                        try {
                            Json.decodeFromString<UrlMapping>(event.payload)
                        } catch (e: Exception) {
                            logger.error(e) {
                                "[Outbox] Failed to decode event payload (poison message): ${event.id}"
                            }
                            null
                        }
                    if (mapping != null) decodable.add(mapping)
                    else event.id?.let { poisonIds.add(it) }
                }

                // 1. 디코딩된 이벤트만 실제 DB에 멱등 저장한다. 저장이 실패하면 예외가 전파되어
                //    아래 삭제를 건너뛰므로 다음 틱에서 재시도된다(at-least-once).
                if (decodable.isNotEmpty()) {
                    urlPort.saveAll(decodable.asFlow()).toList()
                }

                // 2. 저장에 성공한 이벤트와, 재시도가 무의미한 poison 이벤트를 함께 삭제한다.
                //    (모든 이벤트는 디코딩 성공 또는 poison 중 하나이므로 전체 ID가 처리 대상이다.)
                //    poison 이벤트를 남겨두면 매 틱 디코딩 실패가 무한 반복되기 때문이다.
                val processedIds = events.mapNotNull { it.id }
                if (processedIds.isNotEmpty()) {
                    outboxPort.deleteEvents(processedIds)
                }

                logger.info {
                    "[Outbox] Relayed ${decodable.size} events, discarded ${poisonIds.size} poison events."
                }
            } catch (e: Exception) {
                logger.error(e) { "[Outbox Error] Failed to relay outbox events." }
            } finally {
                isRunning.set(false)
            }
        }
    }
}
