package io.dave.shortener.adapter.inbound.router

import io.dave.shortener.adapter.inbound.dto.ShortenRequest
import io.dave.shortener.adapter.inbound.dto.ShortenResponse
import io.dave.shortener.adapter.inbound.handler.ShortenerHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class ShortenerRouter {

    @Bean
    @RouterOperations(
        value = [
            RouterOperation(
                path = "/shorten",
                method = [RequestMethod.POST],
                beanClass = ShortenerHandler::class,
                beanMethod = "shorten",
                operation = Operation(
                    operationId = "shortenUrl",
                    summary = "새로운 단축 URL 생성",
                    requestBody = RequestBody(
                        required = true,
                        content = [Content(schema = Schema(implementation = ShortenRequest::class))]
                    ),
                    responses = [
                        ApiResponse(
                            responseCode = "200",
                            description = "단축 URL이 성공적으로 생성되었습니다",
                            content = [Content(schema = Schema(implementation = ShortenResponse::class))]
                        )
                    ]
                )
            ),
            RouterOperation(
                path = "/shorten/{shortUrl}",
                method = [RequestMethod.GET],
                beanClass = ShortenerHandler::class,
                beanMethod = "redirect",
                operation = Operation(
                    operationId = "redirectUrl",
                    summary = "원본 긴 URL로 리다이렉트",
                    parameters = [
                        Parameter(
                            name = "shortUrl",
                            `in` = ParameterIn.PATH,
                            required = true,
                            description = "단축 URL ID"
                        )
                    ],
                    responses = [
                        ApiResponse(
                            responseCode = "302",
                            description = "찾음 - 원본 긴 URL로 리다이렉트"
                        )
                    ]
                )
            ),
            RouterOperation(
                path = "/ping",
                method = [RequestMethod.GET],
                beanClass = ShortenerHandler::class,
                beanMethod = "ping",
                operation = Operation(
                    operationId = "ping",
                    summary = "상태 확인 엔드포인트",
                    responses = [
                        ApiResponse(responseCode = "200", description = "응답: pong",
                                    content = [Content(schema = Schema(implementation = String::class))])
                    ]
                )
            ),
            RouterOperation(
                path = "/health",
                method = [RequestMethod.GET],
                beanClass = ShortenerHandler::class,
                beanMethod = "health",
                operation = Operation(
                    operationId = "health",
                    summary = "애플리케이션 상태 확인",
                    responses = [
                        ApiResponse(responseCode = "200", description = "애플리케이션이 정상적으로 실행 중입니다")
                    ]
                )
            )
        ]
    )
    fun shortenerRoutes(handler: ShortenerHandler) = coRouter {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("/shorten", handler::shorten)
        }
        GET("/shorten/{shortUrl}", handler::redirect)
        
        // Health check endpoints
        GET("/ping", handler::ping)
        GET("/health", handler::health)
    }
}
