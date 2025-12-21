package io.dave.snowflake.adapter.inbound

import io.dave.snowflake.domain.util.RedisRateLimiter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/** IP 기반 요청 처리율 제한(Rate Limiting) 필터. */
@Order(1) // ExceptionHandler(Order -1) 뒤, 실제 라우터 이전에 실행
class RateLimitFilter(private val redisRateLimiter: RedisRateLimiter) : WebFilter {

    companion object {
        private const val LIMIT = 100 // 예: IP당 1분간 100회 요청
        private const val WINDOW_SECONDS = 60
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
        val path = exchange.request.path.value()

        // /shorten 엔드포인트에 대해 우선적으로 제안
        if (path.startsWith("/shorten")) {
            return mono { redisRateLimiter.isAllowed(clientIp, LIMIT, WINDOW_SECONDS) }.flatMap {
                    isAllowed ->
                if (!isAllowed) {
                    logger.warn { "[RateLimit] Request rejected for IP: $clientIp" }
                    val response = exchange.response
                    response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                    response.setComplete()
                } else {
                    chain.filter(exchange)
                }
            }
        }

        return chain.filter(exchange)
    }
}
