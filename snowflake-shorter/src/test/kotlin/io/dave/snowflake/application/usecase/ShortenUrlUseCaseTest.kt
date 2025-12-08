package io.dave.snowflake.application.usecase

import io.dave.snowflake.application.event.ShortUrlCreatedEvent
import io.dave.snowflake.domain.component.ShortUrlGenerator
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
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

    private val shortUrlGenerator: ShortUrlGenerator = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val useCase = ShortenUrlUseCase(shortUrlGenerator, eventPublisher)

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
    }
}