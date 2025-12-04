package io.dave.snowflake.adapter.inbound.handler

import com.ninjasquad.springmockk.MockkBean
import io.dave.snowflake.adapter.inbound.router.WorkerInfoRouter
import io.dave.snowflake.core.config.AssignedWorkerInfo
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@WebFluxTest(WorkerInfoHandler::class, WorkerInfoRouter::class)
@DisplayName("WorkerInfoHandler 테스트")
class WorkerInfoHandlerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var assignedWorkerInfo: AssignedWorkerInfo

    @Test
    @DisplayName("GET /active-worker 호출 시 활성 워커 ID 목록을 반환한다")
    fun `getActiveWorkers should return the list of active worker IDs`() {
        // given
        val activeWorkerIds = listOf(1L, 2L, 3L)
        every { assignedWorkerInfo.workerIds } returns activeWorkerIds

        // when & then
        webTestClient.get().uri("/active-worker")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<Long>>()
            .isEqualTo(activeWorkerIds)
    }
}
