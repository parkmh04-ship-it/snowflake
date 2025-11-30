package io.dave.snowflake.application.usecase

import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RetrieveUrlUseCase 테스트")
class RetrieveUrlUseCaseTest {

    private val urlPort: UrlPort = mockk()
    private val useCase = RetrieveUrlUseCase(urlPort)

    @Test
    @DisplayName("단축 URL이 존재하면 매핑 정보를 반환한다")
    fun `should return mapping if short url exists`() = runTest {
        // given
        val shortUrlStr = "abc"
        val shortUrl = ShortUrl(shortUrlStr)
        val mapping = UrlMapping(shortUrl, LongUrl("https://example.com"))

        coEvery { urlPort.findByShortUrl(shortUrl) } returns mapping

        // when
        val result = useCase.retrieve(shortUrlStr)

        // then
        assertEquals(mapping, result)
    }

    @Test
    @DisplayName("단축 URL이 존재하지 않으면 null을 반환한다")
    fun `should return null if short url does not exist`() = runTest {
        // given
        val shortUrlStr = "unknown"
        val shortUrl = ShortUrl(shortUrlStr)

        coEvery { urlPort.findByShortUrl(shortUrl) } returns null

        // when
        val result = useCase.retrieve(shortUrlStr)

        // then
        assertNull(result)
    }
}