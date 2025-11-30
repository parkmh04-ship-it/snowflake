package io.dave.snowflake.adapter.inbound.router

import io.dave.snowflake.adapter.inbound.handler.WorkerInfoHandler
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
class WorkerInfoRouter(
    private val workerInfoHandler: WorkerInfoHandler
) {
    @Bean
    @RouterOperations(
        RouterOperation(
            path = "/active-worker",
            method = [RequestMethod.GET],
            beanClass = WorkerInfoHandler::class,
            beanMethod = "getActiveWorkers",
            operation = Operation(
                operationId = "getActiveWorkers",
                summary = "Get list of active worker IDs",
                responses = [
                    ApiResponse(
                        responseCode = "200",
                        description = "List of active worker IDs",
                        content = [Content(schema = Schema(implementation = List::class))]
                    )
                ]
            )
        )
    )
    fun workerInfoRoutes() = coRouter {
        GET("/active-worker", workerInfoHandler::getActiveWorkers)
    }
}
