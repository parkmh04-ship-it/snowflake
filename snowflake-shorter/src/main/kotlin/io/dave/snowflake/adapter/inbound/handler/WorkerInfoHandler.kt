package io.dave.snowflake.adapter.inbound.handler

import io.dave.snowflake.core.config.AssignedWorkerInfo
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class WorkerInfoHandler(
    private val assignedWorkerInfo: AssignedWorkerInfo
) {
    suspend fun getActiveWorkers(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait(assignedWorkerInfo.workerIds)
    }
}
