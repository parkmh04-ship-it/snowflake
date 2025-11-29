package io.dave.shortener.adapter.inbound.handler

import io.dave.shortener.application.usecase.worker.WorkerCleansingUseCase
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class WorkerCleansingHandler(
    private val workerCleansingUseCase: WorkerCleansingUseCase
) {

    /**
     * 비활성 상태의 워커들을 정리하는 요청을 처리합니다.
     * 정리된 워커 수를 응답합니다.
     */
    suspend fun cleanseIdleWorkers(request: ServerRequest): ServerResponse {
        return try {
            val cleansedCount = workerCleansingUseCase.cleanseIdleWorkers()
            ServerResponse.ok().bodyValueAndAwait(WorkerCleansingResponse(cleansedCount))
        } catch (e: Exception) {
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValueAndAwait(mapOf("error" to "Error cleansing idle workers: ${e.message}"))
        }
    }

    /**
     * 워커 정리 작업의 결과를 나타내는 데이터 전송 객체(DTO).
     *
     * @property cleanedCount 정리된 워커의 총 개수.
     */
    data class WorkerCleansingResponse(val cleanedCount: Int)
}
