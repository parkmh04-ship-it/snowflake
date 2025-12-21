package io.dave.snowflake.integration

import io.dave.snowflake.application.usecase.RetryFailedEventsUseCase
import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.model.FailedEventStatus
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.port.outbound.DeadLetterQueuePort
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Dead Letter Queue 기능에 대한 통합 테스트입니다.
 *
 * 실제 MySQL 데이터베이스와 연동하여 DLQ의 전체 흐름을 검증합니다:
 * 1. 실패 이벤트 저장
 * 2. 재시도 가능 이벤트 조회
 * 3. 재처리 및 상태 변경
 * 4. 오래된 이벤트 정리
 */
@DisplayName("Dead Letter Queue 통합 테스트")
class DeadLetterQueueIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var deadLetterQueuePort: DeadLetterQueuePort

    @Autowired
    private lateinit var retryFailedEventsUseCase: RetryFailedEventsUseCase

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 failed_events 테이블 초기화
        failedEventRepository.deleteAll()
    }

    @Test
    @DisplayName("실패 이벤트를 DLQ에 저장할 수 있다")
    fun `should save failed event to DLQ`() = runBlocking {
        // given
        val failedEvent =
            FailedEvent(
                shortUrl = ShortUrl("test123"),
                longUrl = LongUrl("https://www.test.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                lastError = "Database connection failed"
            )

        // when
        val savedEvent = deadLetterQueuePort.save(failedEvent)

        // then
        assertNotNull(savedEvent.id)
        assertEquals(failedEvent.shortUrl, savedEvent.shortUrl)
        assertEquals(failedEvent.longUrl, savedEvent.longUrl)
        assertEquals(FailedEventStatus.PENDING, savedEvent.status)

        // 데이터베이스 확인
        val entities = failedEventRepository.findAll()
        assertEquals(1, entities.size)
        assertEquals("test123", entities[0].shortUrl)
    }

    @Test
    @DisplayName("여러 실패 이벤트를 배치로 저장할 수 있다")
    fun `should save multiple failed events in batch`() = runBlocking {
        // given
        val failedEvents =
            listOf(
                FailedEvent(
                    shortUrl = ShortUrl("batch1"),
                    longUrl = LongUrl("https://www.batch1.com"),
                    createdAt = System.currentTimeMillis(),
                    retryCount = 0,
                    lastError = "Error 1"
                ),
                FailedEvent(
                    shortUrl = ShortUrl("batch2"),
                    longUrl = LongUrl("https://www.batch2.com"),
                    createdAt = System.currentTimeMillis(),
                    retryCount = 0,
                    lastError = "Error 2"
                ),
                FailedEvent(
                    shortUrl = ShortUrl("batch3"),
                    longUrl = LongUrl("https://www.batch3.com"),
                    createdAt = System.currentTimeMillis(),
                    retryCount = 0,
                    lastError = "Error 3"
                )
            )

        // when
        val savedEvents = deadLetterQueuePort.saveAll(failedEvents.asFlow()).toList()

        // then
        assertEquals(3, savedEvents.size)
        assertTrue(savedEvents.all { it.id != null })

        // 데이터베이스 확인
        val entities = failedEventRepository.findAll()
        assertEquals(3, entities.size)
    }

    @Test
    @DisplayName("상태별로 실패 이벤트를 조회할 수 있다")
    fun `should find failed events by status`() = runBlocking {
        // given
        val pendingEvent =
            FailedEvent(
                shortUrl = ShortUrl("pending1"),
                longUrl = LongUrl("https://www.pending.com"),
                createdAt = System.currentTimeMillis(),
                status = FailedEventStatus.PENDING
            )

        val resolvedEvent =
            FailedEvent(
                shortUrl = ShortUrl("resolved1"),
                longUrl = LongUrl("https://www.resolved.com"),
                createdAt = System.currentTimeMillis(),
                status = FailedEventStatus.RESOLVED
            )

        deadLetterQueuePort.save(pendingEvent)
        deadLetterQueuePort.save(resolvedEvent)

        // when
        val pendingEvents =
            deadLetterQueuePort.findByStatus(FailedEventStatus.PENDING).toList()
        val resolvedEvents =
            deadLetterQueuePort.findByStatus(FailedEventStatus.RESOLVED).toList()

        // then
        assertEquals(1, pendingEvents.size)
        assertEquals("pending1", pendingEvents[0].shortUrl.value)

        assertEquals(1, resolvedEvents.size)
        assertEquals("resolved1", resolvedEvents[0].shortUrl.value)
    }

    @Test
    @DisplayName("재시도 가능한 이벤트만 조회할 수 있다")
    fun `should find only retryable events`() = runBlocking {
        // given
        // 재시도 가능 (PENDING, retry_count < 3)
        val retryableEvent =
            FailedEvent(
                shortUrl = ShortUrl("retryable1"),
                longUrl = LongUrl("https://www.retryable.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 1,
                status = FailedEventStatus.PENDING
            )

        // 재시도 불가능 (retry_count >= 3)
        val maxRetriedEvent =
            FailedEvent(
                shortUrl = ShortUrl("maxretry1"),
                longUrl = LongUrl("https://www.maxretry.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 3,
                status = FailedEventStatus.PENDING
            )

        // 재시도 불가능 (RESOLVED)
        val resolvedEvent =
            FailedEvent(
                shortUrl = ShortUrl("resolved1"),
                longUrl = LongUrl("https://www.resolved.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                status = FailedEventStatus.RESOLVED
            )

        deadLetterQueuePort.save(retryableEvent)
        deadLetterQueuePort.save(maxRetriedEvent)
        deadLetterQueuePort.save(resolvedEvent)

        // when
        val retryableEvents = deadLetterQueuePort.findRetryableEvents().toList()

        // then
        assertEquals(1, retryableEvents.size)
        assertEquals("retryable1", retryableEvents[0].shortUrl.value)
    }

    @Test
    @DisplayName("실패 이벤트를 업데이트할 수 있다")
    fun `should update failed event`() = runBlocking {
        // given
        val originalEvent =
            FailedEvent(
                shortUrl = ShortUrl("update1"),
                longUrl = LongUrl("https://www.update.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                status = FailedEventStatus.PENDING
            )

        val savedEvent = deadLetterQueuePort.save(originalEvent)

        // when - 재시도 횟수 증가 및 상태 변경
        val updatedEvent =
            savedEvent
                .incrementRetry("New error message")
                .withStatus(FailedEventStatus.PROCESSING)

        val result = deadLetterQueuePort.update(updatedEvent)

        // then
        assertEquals(1, result.retryCount)
        assertEquals(FailedEventStatus.PROCESSING, result.status)
        assertEquals("New error message", result.lastError)

        // 데이터베이스 확인
        val entity = failedEventRepository.findById(savedEvent.id!!).get()
        assertEquals(1, entity.retryCount)
        assertEquals(FailedEventStatus.PROCESSING, entity.status)
    }

    @Test
    @DisplayName("오래된 RESOLVED 이벤트를 삭제할 수 있다")
    fun `should delete old resolved events`() = runBlocking {
        // given
        val currentTime = System.currentTimeMillis()
        val eightDaysAgo = currentTime - (8 * 24 * 60 * 60 * 1000L)

        // 8일 전 RESOLVED 이벤트 (삭제 대상)
        val oldResolvedEvent =
            FailedEvent(
                shortUrl = ShortUrl("old1"),
                longUrl = LongUrl("https://www.old.com"),
                createdAt = eightDaysAgo,
                failedAt = eightDaysAgo,
                status = FailedEventStatus.RESOLVED
            )

        // 최근 RESOLVED 이벤트 (유지)
        val recentResolvedEvent =
            FailedEvent(
                shortUrl = ShortUrl("recent1"),
                longUrl = LongUrl("https://www.recent.com"),
                createdAt = currentTime,
                failedAt = currentTime,
                status = FailedEventStatus.RESOLVED
            )

        // PENDING 이벤트 (유지)
        val pendingEvent =
            FailedEvent(
                shortUrl = ShortUrl("pending1"),
                longUrl = LongUrl("https://www.pending.com"),
                createdAt = eightDaysAgo,
                failedAt = eightDaysAgo,
                status = FailedEventStatus.PENDING
            )

        deadLetterQueuePort.save(oldResolvedEvent)
        deadLetterQueuePort.save(recentResolvedEvent)
        deadLetterQueuePort.save(pendingEvent)

        // when - 7일 이상 된 RESOLVED 이벤트 삭제
        val sevenDaysAgo = currentTime - (7 * 24 * 60 * 60 * 1000L)
        val deletedCount = deadLetterQueuePort.deleteResolvedOlderThan(sevenDaysAgo)

        // then
        assertEquals(1, deletedCount)

        val remainingEvents = failedEventRepository.findAll()
        assertEquals(2, remainingEvents.size)
        assertTrue(remainingEvents.none { it.shortUrl == "old1" })
        assertTrue(remainingEvents.any { it.shortUrl == "recent1" })
        assertTrue(remainingEvents.any { it.shortUrl == "pending1" })
    }

    @Test
    @DisplayName("RetryFailedEventsUseCase를 통해 실패 이벤트를 재처리할 수 있다")
    fun `should retry failed events using use case`() = runBlocking {
        // given
        val failedEvent =
            FailedEvent(
                shortUrl = ShortUrl("retry1"),
                longUrl = LongUrl("https://www.retry.com"),
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                status = FailedEventStatus.PENDING
            )

        deadLetterQueuePort.save(failedEvent)

        // when
        val result = retryFailedEventsUseCase.retryFailedEvents(batchSize = 10)

        // then
        // 재시도 결과 확인 (성공 또는 실패)
        assertTrue(
            result.successCount + result.failureCount + result.permanentFailureCount > 0
        )

        // 데이터베이스에서 상태 변경 확인
        val events = failedEventRepository.findAll()
        assertEquals(1, events.size)

        // PENDING이 아닌 다른 상태로 변경되어야 함
        assertNotEquals(FailedEventStatus.PENDING, events[0].status)
    }
}
