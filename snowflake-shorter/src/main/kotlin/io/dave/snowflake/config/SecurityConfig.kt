package io.dave.snowflake.config

import io.dave.snowflake.adapter.inbound.RateLimitFilter
import io.dave.snowflake.domain.port.RateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {

    @Bean
    fun rateLimitFilter(rateLimiter: RateLimiter): RateLimitFilter {
        return RateLimitFilter(rateLimiter)
    }
}
