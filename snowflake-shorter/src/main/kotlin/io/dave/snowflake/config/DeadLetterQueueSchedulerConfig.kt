package io.dave.snowflake.config

import io.dave.snowflake.application.usecase.RetryFailedEventsUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

/**
 * Dead Letter Queue 재처리 스케줄러 설정입니다.
 *
 * 주기적으로 DLQ에 저장된 실패 이벤트를 재처리하고, 오래된 RESOLVED 이벤트를 정리합니다.
 */
@Configuration
@EnableScheduling
class DeadLetterQueueSchedulerConfig(
    private val retryFailedEventsUseCase: RetryFailedEventsUseCase
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 5분마다 실패 이벤트를 재처리합니다.
     *
     * 초기 지연: 1분 (애플리케이션 시작 후 안정화 시간 확보) 실행 주기: 5분
     */
    @Scheduled(
        initialDelayString = $$"${snowflake.dlq.retry.initial-delay:60000}", // 기본값: 1분
        fixedDelayString = $$"${snowflake.dlq.retry.fixed-delay:300000}" // 기본값: 5분
    )
    fun retryFailedEvents() {
        logger.debug { "[DLQ Scheduler] Starting scheduled retry of failed events..." }

        runBlocking {
            try {
                val result = retryFailedEventsUseCase.retryFailedEvents(batchSize = 100)

                if (result.successCount > 0 ||
                    result.failureCount > 0 ||
                    result.permanentFailureCount > 0
                ) {
                    logger.info {
                        "[DLQ Scheduler] Retry completed - Success: ${result.successCount}, " +
                                "Failure: ${result.failureCount}, Permanent Failure: ${result.permanentFailureCount}"
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "[DLQ Scheduler] Error during scheduled retry" }
            }
        }
    }

    /**
     * 매일 자정에 오래된 RESOLVED 이벤트를 정리합니다.
     *
     * 기본 보관 기간: 7일
     */
    @Scheduled(cron = $$"${snowflake.dlq.cleanup.cron:0 0 0 * * ?}") // 기본값: 매일 자정
    fun cleanupResolvedEvents() {
        logger.debug { "[DLQ Scheduler] Starting cleanup of resolved events..." }

        runBlocking {
            try {
                val deletedCount = retryFailedEventsUseCase.cleanupResolvedEvents(retentionDays = 7)

                if (deletedCount > 0) {
                    logger.info {
                        "[DLQ Scheduler] Cleanup completed - Deleted $deletedCount resolved events"
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "[DLQ Scheduler] Error during cleanup" }
            }
        }
    }
}
