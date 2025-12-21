package io.dave.snowflake.domain.util

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

/** Redis Lua 스크립트를 활용한 고성능 Rate Limiter. 토큰 버킷 알고리즘을 사용합니다. */
class RedisRateLimiter(private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>) {
    // Lua 스크립트: 속도 제한 확인 및 토큰 갱신
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

    /**
     * 특정 키(예: IP)에 대해 요청 허용 여부를 반환합니다.
     * @param key 식별자
     * @param limit 시간당 최대 요청 수
     * @param windowInSeconds 시간 윈도우 (초)
     */
    suspend fun isAllowed(key: String, limit: Int, windowInSeconds: Int): Boolean {
        return reactiveRedisTemplate
            .execute(
                rateLimitScript,
                listOf("ratelimit:$key"),
                listOf(limit.toString(), windowInSeconds.toString())
            ).awaitSingle()
    }
}
