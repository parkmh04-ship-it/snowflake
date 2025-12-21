package io.dave.snowflake.adapter.inbound

import io.dave.snowflake.domain.util.LogMasker
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

@Component
@Order(-2) // 기본 핸들러보다 우선순위를 높여 마스킹 보장
class GlobalErrorWebExceptionHandler : ErrorWebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val maskedMessage = LogMasker.mask(ex.message ?: "Unknown Error")

        // 예외 타입에 따른 HTTP 상태 코드 결정
        val status =
            when (ex) {
                is ResponseStatusException -> ex.statusCode
                is IllegalArgumentException -> HttpStatus.BAD_REQUEST
                else -> HttpStatus.INTERNAL_SERVER_ERROR
            }

        val httpStatus = if (status is HttpStatus) status else HttpStatus.valueOf(status.value())
        logger.error(ex) { "[API Error] Status: ${status.value()}, Message: ${maskedMessage}" }

        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON

        val errorBody =
            """
            {
                "status": ${status.value()},
                "error": "${httpStatus.reasonPhrase}",
                "message": "${if (httpStatus.is5xxServerError) "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요." else maskedMessage}"
            }
        """.trimIndent()

        val buffer: DataBuffer =
            response.bufferFactory().wrap(errorBody.toByteArray(StandardCharsets.UTF_8))
        return response.writeWith(Mono.just(buffer))
    }
}
