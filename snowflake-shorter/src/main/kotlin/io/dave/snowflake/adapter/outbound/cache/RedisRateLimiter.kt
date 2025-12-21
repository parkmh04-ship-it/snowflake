package io.dave.snowflake.adapter.outbound.cache

import io.dave.snowflake.domain.port.RateLimiter
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

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
        return reactiveRedisTemplate
                .execute(
                        rateLimitScript,
                        listOf("ratelimit:$key"),
                        listOf(limit.toString(), windowInSeconds.toString())
                )
                .awaitSingle()
    }
}
