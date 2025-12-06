package io.dave.snowflake.integration

import io.dave.snowflake.adapter.inbound.dto.ShortenRequest
import io.dave.snowflake.adapter.inbound.dto.ShortenResponse
import io.dave.snowflake.adapter.outbound.persistence.repository.ShortUrlRepository
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.port.outbound.UrlPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * URL 단축 및 조회 기능에 대한 통합 테스트입니다.
 *
 * 실제 MySQL 데이터베이스와 연동하여 전체 흐름을 검증합니다:
 * 1. URL 단축 요청 (POST /shorten)
 * 2. 데이터베이스 저장 확인
 * 3. 단축 URL 조회 및 리다이렉트 (GET /shorten/{shortUrl})
 */
@DisplayName("URL 단축 서비스 통합 테스트")
class UrlShortenerIntegrationTest : IntegrationTestBase() {

    @Autowired private lateinit var restTemplate: TestRestTemplate

    @Autowired private lateinit var shortUrlRepository: ShortUrlRepository

    @Autowired private lateinit var urlPort: UrlPort

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        restTemplate.restTemplate.requestFactory = NoRedirectClientHttpRequestFactory()
    }

    class NoRedirectClientHttpRequestFactory :
            org.springframework.http.client.SimpleClientHttpRequestFactory() {
        override fun prepareConnection(connection: java.net.HttpURLConnection, httpMethod: String) {
            super.prepareConnection(connection, httpMethod)
            connection.instanceFollowRedirects = false
        }
    }

    @Test
    @DisplayName("URL을 단축하고 데이터베이스에 저장된다")
    fun `should shorten URL and persist to database`() = runBlocking {
        // given
        val originalUrl = "https://www.google.com"
        val request = ShortenRequest(originalUrl)

        // when
        val response: ResponseEntity<ShortenResponse> =
                restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)

        // then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertNotNull(response.body?.shortUrl)
        assertTrue(response.body!!.shortUrl.startsWith("http://"))

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
        val shortenResponse: ResponseEntity<ShortenResponse> =
                restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)

        assertNotNull(shortenResponse.body)
        val shortUrl = shortenResponse.body!!.shortUrl
        val shortUrlKey = shortUrl.substringAfterLast("/")

        // 이벤트 처리 대기
        delay(200)

        // when - 리다이렉트 확인 (TestRestTemplate은 기본적으로 리다이렉트를 따라가지 않음)
        val redirectResponse: ResponseEntity<String> =
                restTemplate.getForEntity("/shorten/$shortUrlKey", String::class.java)

        // then
        assertEquals(HttpStatus.FOUND, redirectResponse.statusCode)
        assertEquals(originalUrl, redirectResponse.headers.location?.toString())
    }

    @Test
    @DisplayName("존재하지 않는 단축 URL 조회 시 404를 반환한다")
    fun `should return 404 when shortened URL does not exist`() {
        // given
        val nonExistentShortUrl = "nonexistent123"

        // when
        val response: ResponseEntity<String> =
                restTemplate.getForEntity("/shorten/$nonExistentShortUrl", String::class.java)

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    @DisplayName("동일한 URL을 여러 번 단축하면 매번 새로운 단축 URL이 생성된다 (No-Check Strategy)")
    fun `should create new shortened URL for each request with same original URL`() = runBlocking {
        // given
        val originalUrl = "https://www.duplicate-test.com"
        val request = ShortenRequest(originalUrl)

        // when - 동일한 URL을 3번 단축
        val response1 = restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)
        val response2 = restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)
        val response3 = restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)

        // then
        assertEquals(HttpStatus.CREATED, response1.statusCode)
        assertEquals(HttpStatus.CREATED, response2.statusCode)
        assertEquals(HttpStatus.CREATED, response3.statusCode)

        val shortUrl1 = response1.body!!.shortUrl
        val shortUrl2 = response2.body!!.shortUrl
        val shortUrl3 = response3.body!!.shortUrl

        // 각각 다른 단축 URL이 생성되어야 함
        assertNotEquals(shortUrl1, shortUrl2)
        assertNotEquals(shortUrl2, shortUrl3)
        assertNotEquals(shortUrl1, shortUrl3)

        // 이벤트 처리 대기
        delay(300)

        // 데이터베이스에 3개 모두 저장되어야 함
        val savedCount = shortUrlRepository.findAll().count { it.longUrl == originalUrl }
        assertTrue(savedCount >= 3, "동일한 원본 URL에 대해 3개 이상의 레코드가 있어야 합니다")
    }

    @Test
    @DisplayName("캐시를 통해 빠르게 조회할 수 있다")
    fun `should retrieve URL from cache after first access`() = runBlocking {
        // given
        val originalUrl = "https://www.cache-test.com"
        val request = ShortenRequest(originalUrl)

        val shortenResponse =
                restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)

        val shortUrlKey = shortenResponse.body!!.shortUrl.substringAfterLast("/")

        // 이벤트 처리 및 캐시 갱신 대기
        delay(200)

        // when - 첫 번째 조회 (DB에서 조회 후 캐시 저장)
        val firstAccess = restTemplate.getForEntity("/shorten/$shortUrlKey", String::class.java)

        // 두 번째 조회 (캐시에서 조회)
        val secondAccess = restTemplate.getForEntity("/shorten/$shortUrlKey", String::class.java)

        // then
        assertEquals(HttpStatus.FOUND, firstAccess.statusCode)
        assertEquals(HttpStatus.FOUND, secondAccess.statusCode)
        assertEquals(originalUrl, firstAccess.headers.location?.toString())
        assertEquals(originalUrl, secondAccess.headers.location?.toString())
    }

    @Test
    @DisplayName("잘못된 URL 형식은 400 Bad Request를 반환한다")
    fun `should return 400 for invalid URL format`() {
        // given
        val invalidRequest = ShortenRequest("invalid-url")

        // when
        val response: ResponseEntity<String> =
                restTemplate.postForEntity("/shorten", invalidRequest, String::class.java)

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    @DisplayName("UrlPort를 통해 단축 URL 존재 여부를 확인할 수 있다")
    fun `should check if shortened URL exists using UrlPort`() = runBlocking {
        // given
        val originalUrl = "https://www.port-test.com"
        val request = ShortenRequest(originalUrl)

        val shortenResponse =
                restTemplate.postForEntity("/shorten", request, ShortenResponse::class.java)

        val shortUrlKey = shortenResponse.body!!.shortUrl.substringAfterLast("/")

        // 이벤트 처리 대기
        delay(200)

        // when
        val exists = urlPort.existsByShortUrl(ShortUrl(shortUrlKey))

        // then
        assertTrue(exists, "단축 URL이 존재해야 합니다")
    }
}
