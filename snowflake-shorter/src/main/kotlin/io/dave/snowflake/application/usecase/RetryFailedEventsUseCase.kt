package io.dave.snowflake.application.usecase

import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.model.FailedEventStatus
import io.dave.snowflake.domain.port.outbound.DeadLetterQueuePort
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.dave.snowflake.domain.util.retryWithExponentialBackoffCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

/**
 * Dead Letter Queue에 저장된 실패 이벤트를 재처리하는 유스케이스입니다.
 *
 * 이 서비스는 주기적으로 호출되어 재시도 가능한 이벤트를 조회하고, Exponential Backoff 전략으로 재시도합니다.
 */
@Service
class RetryFailedEventsUseCase(
    private val deadLetterQueuePort: DeadLetterQueuePort,
    private val urlPort: UrlPort
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 재시도 가능한 실패 이벤트를 조회하여 재처리합니다.
     *
     * @param batchSize 한 번에 처리할 최대 이벤트 개수
     * @return 재처리 결과 (성공 개수, 실패 개수)
     */
    suspend fun retryFailedEvents(batchSize: Int = 100): RetryResult {
        logger.info { "[DLQ Retry] Starting retry process for failed events..." }

        val retryableEvents = deadLetterQueuePort.findRetryableEvents(batchSize).toList()

        if (retryableEvents.isEmpty()) {
            logger.debug { "[DLQ Retry] No retryable events found." }
            return RetryResult(successCount = 0, failureCount = 0, permanentFailureCount = 0)
        }

        logger.info { "[DLQ Retry] Found ${retryableEvents.size} retryable events." }

        var successCount = 0
        var failureCount = 0
        var permanentFailureCount = 0

        for (failedEvent in retryableEvents) {
            // 이벤트 상태를 PROCESSING으로 변경
            val processingEvent = failedEvent.withStatus(FailedEventStatus.PROCESSING)
            deadLetterQueuePort.update(processingEvent)

            // 재시도 실행
            val result =
                retryWithExponentialBackoffCatching(
                    maxAttempts = 2, // DLQ 재처리는 2회만 시도 (이미 3회 실패한 이벤트)
                    initialDelayMillis = 200,
                    maxDelayMillis = 5000
                ) {
                    // 단일 이벤트를 UrlMapping으로 변환하여 저장
                    val mapping = failedEvent.toUrlMapping()
                    urlPort.saveAll(listOf(mapping).asFlow()).toList()
                }

            when (result) {
                is io.dave.snowflake.domain.util.RetryResult.Success -> {
                    // 성공: RESOLVED 상태로 변경
                    val resolvedEvent = processingEvent.withStatus(FailedEventStatus.RESOLVED)
                    deadLetterQueuePort.update(resolvedEvent)

                    successCount++
                    logger.info { "[DLQ Retry] Successfully retried event ID: ${failedEvent.id}" }
                }

                is io.dave.snowflake.domain.util.RetryResult.Failure -> {
                    // 실패: 재시도 횟수 증가
                    val updatedEvent =
                        processingEvent.incrementRetry(
                            result.exception.message ?: "Unknown error"
                        )

                    // 최대 재시도 횟수 초과 시 FAILED 상태로 변경
                    val finalEvent =
                        if (updatedEvent.retryCount >= FailedEvent.MAX_RETRY_COUNT) {
                            permanentFailureCount++
                            logger.error {
                                "[DLQ Retry] Event ID ${failedEvent.id} exceeded max retry count. Marking as FAILED."
                            }
                            updatedEvent.withStatus(FailedEventStatus.FAILED)
                        } else {
                            failureCount++
                            logger.warn {
                                "[DLQ Retry] Event ID ${failedEvent.id} retry failed. Retry count: ${updatedEvent.retryCount}"
                            }
                            updatedEvent.withStatus(FailedEventStatus.PENDING)
                        }

                    deadLetterQueuePort.update(finalEvent)
                }
            }
        }

        logger.info {
            "[DLQ Retry] Retry process completed. Success: $successCount, Failure: $failureCount, Permanent Failure: $permanentFailureCount"
        }

        return RetryResult(successCount, failureCount, permanentFailureCount)
    }

    /**
     * 오래된 RESOLVED 이벤트를 정리합니다.
     *
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 이벤트 개수
     */
    suspend fun cleanupResolvedEvents(retentionDays: Int = 7): Int {
        val olderThanMillis = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val deletedCount = deadLetterQueuePort.deleteResolvedOlderThan(olderThanMillis)

        if (deletedCount > 0) {
            logger.info {
                "[DLQ Cleanup] Deleted $deletedCount resolved events older than $retentionDays days."
            }
        }

        return deletedCount
    }

    private fun FailedEvent.toUrlMapping() =
        io.dave.snowflake.domain.model.UrlMapping(
            shortUrl = this.shortUrl,
            longUrl = this.longUrl,
            createdAt = this.createdAt
        )

    /** 재처리 결과를 나타내는 데이터 클래스 */
    data class RetryResult(
        val successCount: Int,
        val failureCount: Int,
        val permanentFailureCount: Int
    )
}
