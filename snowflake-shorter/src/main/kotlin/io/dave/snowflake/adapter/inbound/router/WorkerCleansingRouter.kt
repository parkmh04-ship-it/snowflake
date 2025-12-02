package io.dave.snowflake.adapter.inbound.router

import io.dave.snowflake.adapter.inbound.handler.WorkerCleansingHandler
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
class WorkerCleansingRouter(
    private val workerCleansingHandler: WorkerCleansingHandler
) {
    @Bean
    @RouterOperations(
        RouterOperation(
            path = "/cleansing-worker",
            method = [RequestMethod.POST],
            beanClass = WorkerCleansingHandler::class,
            beanMethod = "cleanseIdleWorkers",
            operation = Operation(
                operationId = "cleanseIdleWorkers",
                summary = "Cleanse idle workers (older than 1 hour)",
                responses = [
                    ApiResponse(
                        responseCode = "200",
                        description = "Successfully cleansed workers",
                        content = [Content(schema = Schema(implementation = WorkerCleansingHandler.WorkerCleansingResponse::class))]
                    )
                ]
            )
        )
    )
    fun workerCleansingRoutes() = coRouter {
        POST("/cleansing-worker", workerCleansingHandler::cleanseIdleWorkers)
    }
}
