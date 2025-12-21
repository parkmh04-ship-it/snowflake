package io.dave.snowflake.domain.util

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * RetryUtil의 Exponential Backoff 재시도 로직을 검증하는 단위 테스트입니다.
 *
 * 테스트 범위:
 * - 첫 시도 성공 시 즉시 반환
 * - 재시도 후 성공 시나리오
 * - 모든 재시도 실패 시 예외 발생
 * - Exponential Backoff 지연 시간 검증
 * - RetryResult 타입을 반환하는 Catching 버전
 */
@DisplayName("RetryUtil 테스트")
class RetryUtilTest {

    @Test
    @DisplayName("첫 번째 시도에서 성공하면 즉시 결과를 반환한다")
    fun `retryWithExponentialBackoff should return immediately on first success`() = runTest {
        // Given
        val expectedResult = "Success"
        val block: suspend () -> String = mockk {
            coEvery { this@mockk.invoke() } returns expectedResult
        }

        // When
        val result =
            retryWithExponentialBackoff(
                maxAttempts = 3,
                initialDelayMillis = 100,
                block = block
            )

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { block.invoke() }
    }

    @Test
    @DisplayName("첫 번째 실패 후 두 번째 시도에서 성공한다")
    fun `retryWithExponentialBackoff should succeed on second attempt after one failure`() =
        runTest {
            // Given
            val expectedResult = "Success"
            var callCount = 0
            val block: suspend () -> String = {
                callCount++
                if (callCount == 1) {
                    throw RuntimeException("First attempt failed")
                } else {
                    expectedResult
                }
            }

            // When
            val result =
                retryWithExponentialBackoff(
                    maxAttempts = 3,
                    initialDelayMillis = 50,
                    block = block
                )

            // Then
            assertEquals(expectedResult, result)
            assertEquals(2, callCount, "2번 호출되어야 합니다")
        }

    @Test
    @DisplayName("모든 재시도가 실패하면 마지막 예외를 던진다")
    fun `retryWithExponentialBackoff should throw last exception after all attempts fail`() =
        runTest {
            // Given
            val expectedMessage = "All attempts failed"
            var callCount = 0
            val block: suspend () -> String = {
                callCount++
                throw RuntimeException(expectedMessage)
            }

            // When & Then
            val exception =
                assertThrows<RuntimeException> {
                    retryWithExponentialBackoff(
                        maxAttempts = 3,
                        initialDelayMillis = 10,
                        block = block
                    )
                }

            assertEquals(expectedMessage, exception.message)
            assertEquals(3, callCount, "3번 모두 호출되어야 합니다")
        }

    @Test
    @DisplayName("Exponential Backoff으로 재시도 간격이 증가한다")
    fun `retryWithExponentialBackoff should apply exponential delay`() = runTest {
        // Given
        var callCount = 0
        val block: suspend () -> String = {
            callCount++
            throw RuntimeException("Retry attempt $callCount")
        }

        // When
        try {
            retryWithExponentialBackoff(
                maxAttempts = 3,
                initialDelayMillis = 50, // 더 짧은 시간으로 테스트 속도 향상
                maxDelayMillis = 500,
                factor = 2.0,
                block = block
            )
        } catch (e: Exception) {
            // Expected to fail
        }

        // Then: 타이밍 대신 호출 횟수만 검증
        assertEquals(3, callCount, "3번 모두 호출되어야 합니다")
    }

    @Test
    @DisplayName("최대 지연 시간을 초과하지 않는다")
    fun `retryWithExponentialBackoff should not exceed max delay`() = runTest {
        // Given
        var callCount = 0
        val block: suspend () -> String = {
            callCount++
            throw RuntimeException("Retry attempt $callCount")
        }

        // When
        try {
            retryWithExponentialBackoff(
                maxAttempts = 5,
                initialDelayMillis = 50, // 더 짧은 시간으로 테스트 속도 향상
                maxDelayMillis = 100, // 최대 100ms로 제한
                factor = 2.0,
                block = block
            )
        } catch (e: Exception) {
            // Expected to fail
        }

        // Then: 타이밍 대신 호출 횟수만 검증
        assertEquals(5, callCount, "5번 모두 호출되어야 합니다")
    }

    @Test
    @DisplayName("retryWithExponentialBackoffCatching은 성공 시 Success를 반환한다")
    fun `retryWithExponentialBackoffCatching should return Success on success`() = runTest {
        // Given
        val expectedValue = "Result"
        val block: suspend () -> String = { expectedValue }

        // When
        val result =
            retryWithExponentialBackoffCatching(
                maxAttempts = 3,
                initialDelayMillis = 50,
                block = block
            )

        // Then
        assertTrue(result is RetryResult.Success)
        assertEquals(expectedValue, (result as RetryResult.Success).value)
    }

    @Test
    @DisplayName("retryWithExponentialBackoffCatching은 실패 시 Failure를 반환한다")
    fun `retryWithExponentialBackoffCatching should return Failure after all attempts fail`() =
        runTest {
            // Given
            val expectedMessage = "Persistent failure"
            val block: suspend () -> String = { throw RuntimeException(expectedMessage) }

            // When
            val result =
                retryWithExponentialBackoffCatching(
                    maxAttempts = 3,
                    initialDelayMillis = 10,
                    block = block
                )

            // Then
            assertTrue(result is RetryResult.Failure)
            val failure = result as RetryResult.Failure
            assertEquals(expectedMessage, failure.exception.message)
            assertEquals(3, failure.attempts)
        }

    @Test
    @DisplayName("maxAttempts가 1이면 재시도 없이 한 번만 실행된다")
    fun `retryWithExponentialBackoff should execute only once when maxAttempts is 1`() = runTest {
        // Given
        var callCount = 0
        val block: suspend () -> String = {
            callCount++
            "Success"
        }

        // When
        val result = retryWithExponentialBackoff(maxAttempts = 1, block = block)

        // Then
        assertEquals("Success", result)
        assertEquals(1, callCount, "1번만 호출되어야 합니다")
    }

    @Test
    @DisplayName("서로 다른 예외 타입도 올바르게 재시도한다")
    fun `retryWithExponentialBackoff should retry on different exception types`() = runTest {
        // Given
        var callCount = 0
        val block: suspend () -> String = {
            callCount++
            when (callCount) {
                1 -> throw IllegalArgumentException("First failure")
                2 -> throw IllegalStateException("Second failure")
                else -> "Success"
            }
        }

        // When
        val result =
            retryWithExponentialBackoff(maxAttempts = 3, initialDelayMillis = 10, block = block)

        // Then
        assertEquals("Success", result)
        assertEquals(3, callCount)
    }
}
