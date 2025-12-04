package io.dave.snowflake.id.adapter.inbound.handler

import com.ninjasquad.springmockk.MockkBean
import io.dave.snowflake.id.adapter.inbound.dto.GenerateIdRequest
import io.dave.snowflake.id.adapter.inbound.dto.GenerateIdResponse
import io.dave.snowflake.id.adapter.inbound.router.GlobalTransactionIdRouter
import io.dave.snowflake.id.application.port.inbound.GenerateGlobalTransactionIdUseCase
import io.dave.snowflake.id.domain.model.GlobalTransactionId
import io.mockk.coEvery
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(GlobalTransactionIdHandler::class, GlobalTransactionIdRouter::class)
@DisplayName("GlobalTransactionIdHandler 테스트")
class GlobalTransactionIdHandlerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var useCase: GenerateGlobalTransactionIdUseCase

    @Test
    @DisplayName("ID 생성 요청 시 200 OK와 생성된 ID를 반환해야 한다")
    fun `generate should return 200 and id response`() {
        // given
        val request = GenerateIdRequest(originGlobalTransactionId = null)
        val generatedId = 12345L
        val domainModel = GlobalTransactionId(globalTransactionId = generatedId)

        coEvery { useCase.generate(null) } returns domainModel

        // when & then
        webTestClient.post()
            .uri("/global-transaction-id")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(GenerateIdResponse::class.java)
            .consumeWith { result ->
                val response = result.responseBody!!
                assert(response.globalTransactionId == generatedId)
            }
    }

    @Test
    @DisplayName("원거래 ID 포함 요청 시 200 OK와 생성된 ID를 반환해야 한다")
    fun `generate with origin id should return 200`() {
        // given
        val originId = 999L
        val request = GenerateIdRequest(originGlobalTransactionId = originId)
        val generatedId = 12345L
        val domainModel = GlobalTransactionId(
            globalTransactionId = generatedId,
            originGlobalTransactionId = originId
        )

        coEvery { useCase.generate(originId) } returns domainModel

        // when & then
        webTestClient.post()
            .uri("/global-transaction-id")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(GenerateIdResponse::class.java)
            .consumeWith { result ->
                val response = result.responseBody!!
                assert(response.globalTransactionId == generatedId)
            }
    }
}
