package io.dave.snowflake.id.adapter.inbound.handler

import io.dave.snowflake.id.adapter.inbound.dto.GenerateIdRequest
import io.dave.snowflake.id.adapter.inbound.dto.GenerateIdResponse
import io.dave.snowflake.id.application.port.inbound.GenerateGlobalTransactionIdUseCase
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBodyOrNull
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class GlobalTransactionIdHandler(
    private val useCase: GenerateGlobalTransactionIdUseCase
) {

    suspend fun generate(request: ServerRequest): ServerResponse {
        val requestBody = request.awaitBodyOrNull<GenerateIdRequest>()
        val originId = requestBody?.originGlobalTransactionId

        val result = useCase.generate(originId)

        return ServerResponse.ok().bodyValueAndAwait(
            GenerateIdResponse(globalTransactionId = result.globalTransactionId)
        )
    }
}
