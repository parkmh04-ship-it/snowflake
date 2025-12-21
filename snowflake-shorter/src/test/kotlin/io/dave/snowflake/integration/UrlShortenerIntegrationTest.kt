package io.dave.snowflake.integration

import io.dave.snowflake.adapter.inbound.dto.ShortenRequest
import io.dave.snowflake.adapter.inbound.dto.ShortenResponse
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.port.outbound.UrlPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * URL 단축 및 조회 기능에 대한 통합 테스트입니다.
 *
 * 실제 MySQL 데이터베이스와 연동하여 전체 흐름을 검증합니다:
 * 1. URL 단축 요청 (POST /shorten)
 * 2. 데이터베이스 저장 확인
 * 3. 단축 URL 조회 및 리다이렉트 (GET /shorten/{shortUrl})
 */
@DisplayName("URL 단축 서비스 통합 테스트")
class UrlShorterIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var webTestClient:
            org.springframework.test.web.reactive.server.WebTestClient

    @Autowired
    private lateinit var urlPort: UrlPort

    @Test
    @DisplayName("URL을 단축하고 데이터베이스에 저장된다")
    fun `should shorten URL and persist to database`() = runBlocking {
        // given
        val originalUrl = "https://www.google.com"
        val request = ShortenRequest(originalUrl)

        // when
        val result =
            webTestClient
                .post()
                .uri("/shorten")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(ShortenResponse::class.java)
                .returnResult()
                .responseBody

        // then
        assertNotNull(result)
        assertNotNull(result?.shortUrl)
        assertTrue(result!!.shortUrl.startsWith("http://"))

        // 이벤트 처리 대기 (비동기 배치 처리)
        delay(200)

        // 데이터베이스 확인
        val savedEntity = shortUrlRepository.findByLongUrl(originalUrl)
        assertTrue(savedEntity != null, "데이터베이스에 저장되어야 합니다")
        assertEquals(originalUrl, savedEntity!!.longUrl)
    }

    @Test
    @DisplayName("단축된 URL로 조회하면 원본 URL로 리다이렉트된다")
    fun `should redirect to original URL when accessing shortened URL`() = runBlocking {
        // given
        val originalUrl = "https://www.example.com/test"
        val request = ShortenRequest(originalUrl)

        // URL 단축
        val shortenResponse =
            webTestClient
                .post()
                .uri("/shorten")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody(ShortenResponse::class.java)
                .returnResult()
                .responseBody!!

        val shortUrl = shortenResponse.shortUrl
        val shortUrlKey = shortUrl.substringAfterLast("/")

        // 이벤트 처리 대기
        delay(200)

        // when - 리다이렉트 확인
        webTestClient
            .get()
            .uri("/shorten/$shortUrlKey")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueEquals("Location", originalUrl)
    }

    @Test
    @DisplayName("존재하지 않는 단축 URL 조회 시 404를 반환한다")
    fun `should return 404 when shortened URL does not exist`() {
        // given
        val nonExistentShortUrl = "nonexistent123"

        // when & then
        webTestClient
            .get()
            .uri("/shorten/$nonExistentShortUrl")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    @DisplayName("동일한 URL을 여러 번 단축하면 매번 새로운 단축 URL이 생성된다 (No-Check Strategy)")
    fun `should create new shortened URL for each request with same original URL`() =
        runBlocking {
            // given
            val originalUrl = "https://www.duplicate-test.com"
            val request = ShortenRequest(originalUrl)

            // when - 동일한 URL을 3번 단축
            val response1 =
                webTestClient
                    .post()
                    .uri("/shorten")
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody(ShortenResponse::class.java)
                    .returnResult()
                    .responseBody!!

            val response2 =
                webTestClient
                    .post()
                    .uri("/shorten")
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody(ShortenResponse::class.java)
                    .returnResult()
                    .responseBody!!

            val response3 =
                webTestClient
                    .post()
                    .uri("/shorten")
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isCreated
                    .expectBody(ShortenResponse::class.java)
                    .returnResult()
                    .responseBody!!

            val shortUrl1 = response1.shortUrl
            val shortUrl2 = response2.shortUrl
            val shortUrl3 = response3.shortUrl

            // then - 각각 다른 단축 URL이 생성되어야 함
            assertNotEquals(shortUrl1, shortUrl2)
            assertNotEquals(shortUrl2, shortUrl3)
            assertNotEquals(shortUrl1, shortUrl3)

            // 이벤트 처리 대기
            delay(300)

            // 데이터베이스에 3개 모두 저장되어야 함
            val savedCount =
                shortUrlRepository.findAll().count { it.longUrl == originalUrl }
            assertTrue(savedCount >= 3, "동일한 원본 URL에 대해 3개 이상의 레코드가 있어야 합니다")
        }

    @Test
    @DisplayName("캐시를 통해 빠르게 조회할 수 있다")
    fun `should retrieve URL from cache after first access`() = runBlocking {
        // given
        val originalUrl = "https://www.cache-test.com"
        val request = ShortenRequest(originalUrl)

        val shortenResponse =
            webTestClient
                .post()
                .uri("/shorten")
                .bodyValue(request)
                .exchange()
                .expectBody(ShortenResponse::class.java)
                .returnResult()
                .responseBody!!

        val shortUrlKey = shortenResponse.shortUrl.substringAfterLast("/")

        // 이벤트 처리 및 캐시 갱신 대기
        delay(200)

        // when - 첫 번째 조회 (DB에서 조회 후 캐시 저장)
        webTestClient
            .get()
            .uri("/shorten/$shortUrlKey")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueEquals("Location", originalUrl)

        // 두 번째 조회 (캐시에서 조회)
        webTestClient
            .get()
            .uri("/shorten/$shortUrlKey")
            .exchange()
            .expectStatus()
            .isFound
            .expectHeader()
            .valueEquals("Location", originalUrl)
    }

    @Test
    @DisplayName("잘못된 URL 형식은 400 Bad Request를 반환한다")
    fun `should return 400 for invalid URL format`() {
        // given
        val invalidRequest = ShortenRequest("invalid-url")

        // when & then
        webTestClient
            .post()
            .uri("/shorten")
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    @DisplayName("UrlPort를 통해 단축 URL 존재 여부를 확인할 수 있다")
    fun `should check if shortened URL exists using UrlPort`() = runBlocking {
        // given
        val originalUrl = "https://www.port-test.com"
        val request = ShortenRequest(originalUrl)

        val shortenResponse =
            webTestClient
                .post()
                .uri("/shorten")
                .bodyValue(request)
                .exchange()
                .expectBody(ShortenResponse::class.java)
                .returnResult()
                .responseBody!!

        val shortUrlKey = shortenResponse.shortUrl.substringAfterLast("/")

        // 이벤트 처리 대기
        delay(200)

        // when
        val exists = urlPort.existsByShortUrl(ShortUrl(shortUrlKey))

        // then
        assertTrue(exists, "단축 URL이 존재해야 합니다")
    }
}
