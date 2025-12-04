package io.dave.snowflake.id.adapter.inbound.router

import io.dave.snowflake.id.adapter.inbound.dto.GenerateIdResponse
import io.dave.snowflake.id.adapter.inbound.handler.GlobalTransactionIdHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class GlobalTransactionIdRouter(
    private val handler: GlobalTransactionIdHandler
) {

    @Bean
    @RouterOperations(
        RouterOperation(
            path = "/global-transaction-id",
            method = [RequestMethod.POST],
            beanClass = GlobalTransactionIdHandler::class,
            beanMethod = "generate",
            operation = Operation(
                operationId = "generate",
                summary = "Generate Global Transaction Id",
                responses = [
                    ApiResponse(
                        responseCode = "200",
                        description = "Successfully generate transaction id",
                        content = [Content(schema = Schema(implementation = GenerateIdResponse::class))]
                    )
                ]
            )
        )
    )
    fun transactionIdRoutes() = coRouter {
        "/global-transaction-id".nest {
            POST("", handler::generate)
        }
    }
}
