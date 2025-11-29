package io.dave.shortener.application.usecase

import io.dave.shortener.domain.component.ShortUrlGenerator
import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.ShortUrl
import io.dave.shortener.domain.model.UrlMapping
import io.dave.shortener.domain.port.outbound.UrlPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ShortenUrlUseCase 테스트")
class ShortenUrlUseCaseTest {

    private val urlPort: UrlPort = mockk()
    private val shortUrlGenerator: ShortUrlGenerator = mockk()
    private val useCase = ShortenUrlUseCase(urlPort, shortUrlGenerator)

    @Test
    @DisplayName("이미 존재하는 긴 URL이면 기존 매핑 정보를 반환한다")
    fun `should return existing mapping if long url already exists`() = runTest {
        // given
        val longUrlStr = "https://www.google.com"
        val longUrl = LongUrl(longUrlStr)
        val existingMapping = UrlMapping(ShortUrl("abc"), longUrl)

        coEvery { urlPort.findByLongUrl(longUrl) } returns existingMapping

        // when
        val result = useCase.shorten(longUrlStr)

        // then
        assertEquals(existingMapping, result)
        coVerify(exactly = 0) { shortUrlGenerator.generate() }
        coVerify(exactly = 0) { urlPort.save(any()) }
    }

    @Test
    @DisplayName("새로운 긴 URL이면 새로운 단축 URL을 생성하여 반환한다")
    fun `should generate new mapping if long url does not exist`() = runTest {
        // given
        val longUrlStr = "https://www.naver.com"
        val longUrl = LongUrl(longUrlStr)
        val newShortUrl = ShortUrl("xyz")
        val newMapping = UrlMapping(newShortUrl, longUrl)

        coEvery { urlPort.findByLongUrl(longUrl) } returns null
        coEvery { shortUrlGenerator.generate() } returns newShortUrl
        coEvery { urlPort.save(any()) } returns newMapping

        // when
        val result = useCase.shorten(longUrlStr)

        // then
        assertEquals(newMapping, result)
        coVerify(exactly = 1) { shortUrlGenerator.generate() }
        coVerify(exactly = 1) { urlPort.save(any()) }
    }
}