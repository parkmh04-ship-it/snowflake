package io.dave.snowflake.application.usecase

import io.dave.snowflake.domain.model.*
import io.dave.snowflake.domain.port.outbound.DeadLetterQueuePort
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager

@DisplayName("RetryFailedEventsUseCase 테스트")
class RetryFailedEventsUseCaseTest {

    private lateinit var deadLetterQueuePort: DeadLetterQueuePort
    private lateinit var urlPort: UrlPort
    private lateinit var cacheManager: CacheManager
    private lateinit var retryFailedEventsUseCase: RetryFailedEventsUseCase

    @BeforeEach
    fun setUp() {
        deadLetterQueuePort = mockk()
        urlPort = mockk()
        cacheManager = mockk(relaxed = true)
        retryFailedEventsUseCase =
                RetryFailedEventsUseCase(deadLetterQueuePort, urlPort, cacheManager)
    }

    @Test
    @DisplayName("재시도 가능한 이벤트가 없으면 성공 카운트 0을 반환한다")
    fun `should return zero counts when no retryable events`() = runTest {
        // given
        coEvery { deadLetterQueuePort.findRetryableEvents(any()) } returns flowOf()

        // when
        val result = retryFailedEventsUseCase.retryFailedEvents()

        // then
        assertEquals(0, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(0, result.permanentFailureCount)
    }

    @Test
    @DisplayName("재시도가 성공하면 이벤트 상태를 RESOLVED로 변경한다")
    fun `should mark event as RESOLVED when retry succeeds`() = runTest {
        // given
        val failedEvent =
                FailedEvent(
                        id = 1L,
                        shortUrl = ShortUrl("abc123"),
                        longUrl = LongUrl("https://example.com"),
                        createdAt = System.currentTimeMillis(),
                        retryCount = 1,
                        status = FailedEventStatus.PENDING
                )

        val mapping = UrlMapping(failedEvent.shortUrl, failedEvent.longUrl, failedEvent.createdAt)

        coEvery { deadLetterQueuePort.findRetryableEvents(any()) } returns flowOf(failedEvent)
        coEvery { deadLetterQueuePort.update(any()) } returnsArgument 0
        coEvery { urlPort.saveAll(any()) } returns flowOf(mapping)

        // when
        val result = retryFailedEventsUseCase.retryFailedEvents()

        // then
        assertEquals(1, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(0, result.permanentFailureCount)

        // PROCESSING 상태로 변경 확인
        coVerify { deadLetterQueuePort.update(match { it.status == FailedEventStatus.PROCESSING }) }

        // RESOLVED 상태로 변경 확인
        coVerify { deadLetterQueuePort.update(match { it.status == FailedEventStatus.RESOLVED }) }
    }

    @Test
    @DisplayName("재시도가 실패하면 재시도 횟수를 증가시킨다")
    fun `should increment retry count when retry fails`() = runTest {
        // given
        val failedEvent =
                FailedEvent(
                        id = 1L,
                        shortUrl = ShortUrl("abc123"),
                        longUrl = LongUrl("https://example.com"),
                        createdAt = System.currentTimeMillis(),
                        retryCount = 1,
                        status = FailedEventStatus.PENDING
                )

        coEvery { deadLetterQueuePort.findRetryableEvents(any()) } returns flowOf(failedEvent)
        coEvery { deadLetterQueuePort.update(any()) } returnsArgument 0
        coEvery { urlPort.saveAll(any()) } throws RuntimeException("DB connection failed")

        // when
        val result = retryFailedEventsUseCase.retryFailedEvents()

        // then
        assertEquals(0, result.successCount)
        assertTrue(result.failureCount > 0 || result.permanentFailureCount > 0)

        // 재시도 횟수 증가 확인
        coVerify { deadLetterQueuePort.update(match { it.retryCount == 2 }) }
    }

    @Test
    @DisplayName("최대 재시도 횟수를 초과하면 FAILED 상태로 변경한다")
    fun `should mark as FAILED when max retry count exceeded`() = runTest {
        // given
        val failedEvent =
                FailedEvent(
                        id = 1L,
                        shortUrl = ShortUrl("abc123"),
                        longUrl = LongUrl("https://example.com"),
                        createdAt = System.currentTimeMillis(),
                        retryCount = FailedEvent.MAX_RETRY_COUNT - 1, // 마지막 재시도
                        status = FailedEventStatus.PENDING
                )

        coEvery { deadLetterQueuePort.findRetryableEvents(any()) } returns flowOf(failedEvent)
        coEvery { deadLetterQueuePort.update(any()) } returnsArgument 0
        coEvery { urlPort.saveAll(any()) } throws RuntimeException("DB connection failed")

        // when
        val result = retryFailedEventsUseCase.retryFailedEvents()

        // then
        assertEquals(0, result.successCount)
        assertEquals(1, result.permanentFailureCount)

        // FAILED 상태로 변경 확인
        coVerify { deadLetterQueuePort.update(match { it.status == FailedEventStatus.FAILED }) }
    }

    @Test
    @DisplayName("오래된 RESOLVED 이벤트를 정리한다")
    fun `should cleanup old resolved events`() = runTest {
        // given
        val deletedCount = 10
        coEvery { deadLetterQueuePort.deleteResolvedOlderThan(any()) } returns deletedCount

        // when
        val result = retryFailedEventsUseCase.cleanupResolvedEvents(retentionDays = 7)

        // then
        assertEquals(deletedCount, result)
        coVerify { deadLetterQueuePort.deleteResolvedOlderThan(any()) }
    }
}
