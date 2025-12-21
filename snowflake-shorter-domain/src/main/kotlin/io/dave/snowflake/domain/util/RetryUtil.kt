package io.dave.snowflake.domain.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Exponential Backoff 전략을 사용한 재시도 유틸리티입니다.
 *
 * 재시도 간격은 지수적으로 증가하며, 최대 지연 시간을 초과하지 않습니다.
 *
 * @param maxAttempts 최대 재시도 횟수
 * @param initialDelayMillis 초기 지연 시간 (밀리초)
 * @param maxDelayMillis 최대 지연 시간 (밀리초)
 * @param factor 지수 증가 계수 (기본값: 2.0)
 * @param block 재시도할 작업
 * @return 작업 결과
 * @throws Exception 모든 재시도가 실패한 경우 마지막 예외를 던집니다
 */
suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 100,
        maxDelayMillis: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
): T {
    val logger = KotlinLogging.logger {}
    var currentDelay = initialDelayMillis
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e

            if (attempt < maxAttempts - 1) {
                logger.warn(e) {
                    "[Retry] Attempt ${attempt + 1}/$maxAttempts failed. Retrying in ${currentDelay}ms..."
                }

                delay(currentDelay)

                // 다음 지연 시간 계산 (지수적 증가, 최대값 제한)
                currentDelay = min((currentDelay * factor).toLong(), maxDelayMillis)
            } else {
                logger.error(e) { "[Retry] All $maxAttempts attempts failed." }
            }
        }
    }

    // 모든 재시도 실패 시 마지막 예외를 던짐
    throw lastException ?: IllegalStateException("Retry failed without exception")
}

/** 재시도 결과를 나타내는 sealed class입니다. */
sealed class RetryResult<out T> {
    data class Success<T>(val value: T) : RetryResult<T>()
    data class Failure(val exception: Exception, val attempts: Int) : RetryResult<Nothing>()
}

/**
 * 예외를 던지지 않고 재시도 결과를 반환하는 버전입니다.
 *
 * @param maxAttempts 최대 재시도 횟수
 * @param initialDelayMillis 초기 지연 시간 (밀리초)
 * @param maxDelayMillis 최대 지연 시간 (밀리초)
 * @param factor 지수 증가 계수
 * @param block 재시도할 작업
 * @return RetryResult (Success 또는 Failure)
 */
suspend fun <T> retryWithExponentialBackoffCatching(
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 100,
        maxDelayMillis: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
): RetryResult<T> {
    return try {
        val result =
                retryWithExponentialBackoff(
                        maxAttempts = maxAttempts,
                        initialDelayMillis = initialDelayMillis,
                        maxDelayMillis = maxDelayMillis,
                        factor = factor,
                        block = block
                )
        RetryResult.Success(result)
    } catch (e: Exception) {
        RetryResult.Failure(e, maxAttempts)
    }
}
