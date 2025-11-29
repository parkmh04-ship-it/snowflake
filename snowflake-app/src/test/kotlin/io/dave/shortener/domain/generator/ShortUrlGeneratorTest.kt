package io.dave.shortener.domain.generator

import io.dave.shortener.domain.component.ShortUrlGenerator
import io.dave.shortener.domain.model.ShortUrl
import io.dave.shortener.domain.port.outbound.UrlPort
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
@DisplayName("ShortUrlGenerator 테스트")
class ShortUrlGeneratorTest {

    @MockK
    private lateinit var pooledIdGenerator: PooledIdGenerator

    @MockK
    private lateinit var urlPort: UrlPort

    @MockK
    private lateinit var random: Random

    @InjectMockKs
    private lateinit var shortUrlGenerator: ShortUrlGenerator

    @Test
    @DisplayName("generate 호출 시 중복되지 않는 ShortUrl을 생성하여 반환한다")
    fun `generate should return unique ShortUrl on call`() = runTest {
        // Given
        val snowflakeId = 12345L
        val base62EncodedId = Base62Encoder.encode(snowflakeId)
        val expectedShortUrl = ShortUrl(base62EncodedId)

        coEvery { pooledIdGenerator.nextId() } returns snowflakeId
        coEvery { urlPort.existsByShortUrl(expectedShortUrl) } returns false

        // When
        val result = shortUrlGenerator.generate()

        // Then
        assertEquals(expectedShortUrl, result)
    }

    @Test
    @DisplayName("생성된 ShortUrl이 이미 존재하면 재시도한다")
    fun `generate should retry if generated ShortUrl already exists`() = runTest {
        // Given
        val snowflakeId = 67890L
        val base62EncodedId = Base62Encoder.encode(snowflakeId)
        val expectedFirstRetryCharIndex = Base62Encoder.ALPHABET.indexOf('a') // 'a'의 인덱스
        val expectedSecondRetryCharIndex = Base62Encoder.ALPHABET.indexOf('b') // 'b'의 인덱스
        
        val existingShortUrl = ShortUrl(base62EncodedId)
        val retryShortUrl = ShortUrl("${base62EncodedId}${Base62Encoder.ALPHABET[expectedFirstRetryCharIndex]}") // 첫 번째 재시도
        val finalShortUrl = ShortUrl("${base62EncodedId}${Base62Encoder.ALPHABET[expectedFirstRetryCharIndex]}${Base62Encoder.ALPHABET[expectedSecondRetryCharIndex]}") // 두 번째 재시도

        coEvery { pooledIdGenerator.nextId() } returnsMany listOf(snowflakeId, snowflakeId, snowflakeId, snowflakeId)
        every { random.nextInt(any()) } returns expectedFirstRetryCharIndex andThen expectedSecondRetryCharIndex // Random 모킹

        coEvery { urlPort.existsByShortUrl(existingShortUrl) } returns true
        coEvery { urlPort.existsByShortUrl(retryShortUrl) } returns true
        coEvery { urlPort.existsByShortUrl(finalShortUrl) } returns false

        // When
        val result = shortUrlGenerator.generate()

        // Then
        assertEquals(finalShortUrl, result)
    }
}
