package io.dave.shortener.adapter.inbound

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
@Order(-1) // Ensures this exception handler is picked up first
class GlobalErrorWebExceptionHandler : ErrorWebExceptionHandler {
    
    @Suppress("kotlin:S6510") // Java interface override requires Mono<Void>
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        logger.error(ex) { "Unhandled exception occurred: ${ex.message}" }
        val response = exchange.response
        response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
        return response.setComplete()
    }
}
