package io.dave.snowflake.config

import io.dave.snowflake.adapter.inbound.RateLimitFilter
import io.dave.snowflake.domain.util.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveRedisTemplate

@Configuration
class SecurityConfig {

    @Bean
    fun redisRateLimiter(
            reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
    ): RedisRateLimiter {
        return RedisRateLimiter(reactiveRedisTemplate)
    }

    @Bean
    fun rateLimitFilter(redisRateLimiter: RedisRateLimiter): RateLimitFilter {
        return RateLimitFilter(redisRateLimiter)
    }
}
