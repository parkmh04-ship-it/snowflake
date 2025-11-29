package io.dave.shortener.application.usecase

import io.dave.shortener.application.event.ShortUrlCreatedEvent
import io.dave.shortener.domain.component.ShortUrlGenerator
import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.ShortUrl
import io.dave.shortener.domain.model.UrlMapping
import io.dave.shortener.domain.port.outbound.UrlPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

@DisplayName("ShortenUrlUseCase 테스트")
class ShortenUrlUseCaseTest {

    private val urlPort: UrlPort = mockk()
    private val shortUrlGenerator: ShortUrlGenerator = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val useCase = ShortenUrlUseCase(urlPort, shortUrlGenerator, eventPublisher)

    @Test
    @DisplayName("새로운 단축 URL을 생성하고 이벤트를 발행한 후 매핑 정보를 반환한다")
    fun `should generate new mapping, publish event and return mapping`() = runTest {
        // given
        val longUrlStr = "https://www.naver.com"
        val longUrl = LongUrl(longUrlStr)
        val newShortUrl = ShortUrl("xyz")
        val newMapping = UrlMapping(newShortUrl, longUrl, System.currentTimeMillis())

        coEvery { shortUrlGenerator.generate() } returns newShortUrl
        
        // when
        val result = useCase.shorten(longUrlStr)

        // then
        assertEquals(newMapping.shortUrl, result.shortUrl)
        assertEquals(newMapping.longUrl, result.longUrl)
        coVerify(exactly = 1) { shortUrlGenerator.generate() }
        verify(exactly = 1) { eventPublisher.publishEvent(any<ShortUrlCreatedEvent>()) }
        coVerify(exactly = 0) { urlPort.save(any()) } // save는 호출되지 않음
        coVerify(exactly = 0) { urlPort.findByLongUrl(any()) } // findByLongUrl도 호출되지 않음
    }
}