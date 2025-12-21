package io.dave.snowflake.adapter.inbound.handler

import com.ninjasquad.springmockk.MockkBean
import io.dave.snowflake.adapter.inbound.GlobalErrorWebExceptionHandler
import io.dave.snowflake.adapter.inbound.dto.ShortenRequest
import io.dave.snowflake.adapter.inbound.dto.ShortenResponse
import io.dave.snowflake.adapter.inbound.router.ShorterRouter
import io.dave.snowflake.application.usecase.RetrieveUrlUseCase
import io.dave.snowflake.application.usecase.ShortenUrlUseCase
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.RateLimiter
import io.dave.snowflake.domain.util.LogMasker
import io.mockk.coEvery
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(
    ShorterHandler::class,
    ShorterRouter::class,
    GlobalErrorWebExceptionHandler::class,
    LogMasker::class
)
@DisplayName("ShorterHandler 테스트")
@TestPropertySource(
    properties = ["app.base-url=http://localhost:8080", "app.shorten-path-prefix=/shorten"]
)
class ShorterHandlerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var shortenUrlUseCase: ShortenUrlUseCase

    @MockkBean
    private lateinit var retrieveUrlUseCase: RetrieveUrlUseCase

    @MockkBean(relaxed = true)
    private lateinit var rateLimiter: RateLimiter

    @Test
    @DisplayName("단축 URL 생성 요청 시 201 응답과 단축된 URL 정보를 반환한다")
    fun `shorten should return 201 and shorten response`() {
        // given
        val longUrlStr = "https://example.com"
        val request = ShortenRequest(longUrlStr)
        val shortUrlStr = "abc"
        val mapping = UrlMapping(ShortUrl(shortUrlStr), LongUrl(longUrlStr))

        coEvery { shortenUrlUseCase.shorten(longUrlStr) } returns mapping

        // when & then
        webTestClient
            .post()
            .uri("/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(ShortenResponse::class.java)
            .consumeWith { result ->
                val response = result.responseBody!!
                assert(response.originalUrl == longUrlStr)
                assert(
                    response.shortUrl ==
                            "http://localhost:8080/shorten/$shortUrlStr"
                )
            }
    }

    @Test
    @DisplayName("단축 URL로 조회 시 302 응답과 원본 URL로 리다이렉트한다")
    fun `redirect should return 302 and location header`() {
        // given
        val shortUrlStr = "abc"
        val longUrlStr = "https://example.com"
        val mapping = UrlMapping(ShortUrl(shortUrlStr), LongUrl(longUrlStr))

        coEvery { retrieveUrlUseCase.retrieve(shortUrlStr) } returns mapping

        // when & then
        webTestClient
            .get()
            .uri("/shorten/$shortUrlStr")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .location(longUrlStr)
    }

    @Test
    @DisplayName("존재하지 않는 단축 URL 조회 시 404 응답을 반환한다")
    fun `redirect should return 404 if not found`() {
        // given
        val shortUrlStr = "unknown"

        coEvery { retrieveUrlUseCase.retrieve(shortUrlStr) } returns null

        // when & then
        webTestClient
            .get()
            .uri("/shorten/$shortUrlStr")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    @DisplayName("단축 URL 생성 중 IllegalArgumentException 발생 시 400 응답을 반환한다")
    fun `shorten should return 400 on IllegalArgumentException`() {
        // given
        val request = ShortenRequest("invalid-url")
        val exceptionMessage = "Invalid URL format"

        coEvery { shortenUrlUseCase.shorten(any()) } throws
                IllegalArgumentException(exceptionMessage)

        // when & then
        webTestClient
            .post()
            .uri("/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo(exceptionMessage)
    }

    @Test
    @DisplayName("단축 URL 생성 중 예기치 않은 예외 발생 시 500 응답을 반환한다")
    fun `shorten should return 500 on unexpected Exception`() {
        // given
        val request = ShortenRequest("https://example.com")

        coEvery { shortenUrlUseCase.shorten(any()) } throws
                RuntimeException("Unexpected error")

        // when & then
        webTestClient
            .post()
            .uri("/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
    }

    @Test
    @DisplayName("단축 URL 조회 중 IllegalArgumentException 발생 시 400 응답을 반환한다")
    fun `redirect should return 400 on IllegalArgumentException`() {
        // given
        val shortUrlStr = "invalid"
        val exceptionMessage = "Invalid short URL"

        coEvery { retrieveUrlUseCase.retrieve(shortUrlStr) } throws
                IllegalArgumentException(exceptionMessage)

        // when & then
        webTestClient
            .get()
            .uri("/shorten/$shortUrlStr")
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo(exceptionMessage)
    }

    @Test
    @DisplayName("단축 URL 조회 중 예기치 않은 예외 발생 시 500 응답을 반환한다")
    fun `redirect should return 500 on unexpected Exception`() {
        // given
        val shortUrlStr = "abc"

        coEvery { retrieveUrlUseCase.retrieve(shortUrlStr) } throws
                RuntimeException("Unexpected error")

        // when & then
        webTestClient
            .get()
            .uri("/shorten/$shortUrlStr")
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
    }
}
