package io.dave.snowflake.adapter.outbound.cache

import io.dave.snowflake.domain.port.RateLimiter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/** Redis Lua 스크립트를 활용한 고성능 Rate Limiter 구현체. */
@Component
class RedisRateLimiter(private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>) :
    RateLimiter {

    private val rateLimitScript =
        DefaultRedisScript(
            """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = redis.call('INCR', key)
        if current == 1 then
            redis.call('EXPIRE', key, window)
        end
        return current <= limit
        """.trimIndent(),
            Boolean::class.java
        )

    override suspend fun isAllowed(key: String, limit: Int, windowInSeconds: Int): Boolean {
        return try {
            reactiveRedisTemplate
                .execute(
                    rateLimitScript,
                    listOf("ratelimit:$key"),
                    listOf(limit.toString(), windowInSeconds.toString())
                )
                .awaitFirstOrNull() ?: true
        } catch (e: Exception) {
            // Redis 장애 시 fail-open: 레이트 리미터의 가용성 문제로 정상 요청까지 차단하지 않는다.
            logger.error(e) { "[RateLimit] Redis unavailable, failing open for key=$key" }
            true
        }
    }
}
