package io.dave.shortener.adapter.inbound.handler

import com.ninjasquad.springmockk.MockkBean
import io.dave.shortener.adapter.inbound.router.WorkerCleansingRouter
import io.dave.shortener.application.usecase.worker.WorkerCleansingUseCase
import io.mockk.coEvery
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(WorkerCleansingRouter::class, WorkerCleansingHandler::class)
@DisplayName("WorkerCleansingHandler 테스트")
class WorkerCleansingHandlerTest {

    @MockkBean
    private lateinit var workerCleansingUseCase: WorkerCleansingUseCase

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    @DisplayName("POST /cleansing-worker 호출 시 정비된 워커 수를 반환한다")
    fun `cleanse should return cleaned count`() {
        // Given
        coEvery { workerCleansingUseCase.cleanseIdleWorkers() } returns 5

        // When & Then
        webTestClient.post()
            .uri("/cleansing-worker")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.cleanedCount").isEqualTo(5)
    }

    @Test
    @DisplayName("워커 정비 중 예외 발생 시 500 응답을 반환한다")
    fun `cleanse should return 500 on exception`() {
        // Given
        val errorMessage = "Database error"
        coEvery { workerCleansingUseCase.cleanseIdleWorkers() } throws RuntimeException(errorMessage)

        // When & Then
        webTestClient.post()
            .uri("/cleansing-worker")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("Error cleansing idle workers: $errorMessage")
    }
}
