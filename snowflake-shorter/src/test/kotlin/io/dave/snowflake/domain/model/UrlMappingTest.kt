package io.dave.snowflake.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * UrlMapping 도메인 엔티티의 불변성과 구조를 검증하는 단위 테스트입니다.
 *
 * 테스트 범위:
 * - UrlMapping 생성 및 속성 검증
 * - 불변성 (data class 특성)
 * - 타임스탬프 기본값 검증
 */
@DisplayName("UrlMapping 도메인 모델 테스트")
class UrlMappingTest {

    @Test
    @DisplayName("유효한 ShortUrl과 LongUrl로 UrlMapping을 생성한다")
    fun `should create UrlMapping with valid ShortUrl and LongUrl`() {
        // Given
        val shortUrl = ShortUrl("abc123")
        val longUrl = LongUrl("https://example.com")

        // When
        val urlMapping = UrlMapping(shortUrl, longUrl)

        // Then
        assertEquals(shortUrl, urlMapping.shortUrl)
        assertEquals(longUrl, urlMapping.longUrl)
        assertTrue(urlMapping.createdAt > 0, "createdAt은 양수여야 합니다")
    }

    @Test
    @DisplayName("명시적인 createdAt 값으로 UrlMapping을 생성한다")
    fun `should create UrlMapping with explicit createdAt`() {
        // Given
        val shortUrl = ShortUrl("xyz789")
        val longUrl = LongUrl("https://example.com/page")
        val createdAt = 1234567890L

        // When
        val urlMapping = UrlMapping(shortUrl, longUrl, createdAt)

        // Then
        assertEquals(shortUrl, urlMapping.shortUrl)
        assertEquals(longUrl, urlMapping.longUrl)
        assertEquals(createdAt, urlMapping.createdAt)
    }

    @Test
    @DisplayName("createdAt의 기본값은 현재 시간이다")
    fun `should have default createdAt as current time`() {
        // Given
        val shortUrl = ShortUrl("test")
        val longUrl = LongUrl("https://test.com")
        val beforeCreation = System.currentTimeMillis()

        // When
        val urlMapping = UrlMapping(shortUrl, longUrl)
        val afterCreation = System.currentTimeMillis()

        // Then
        assertTrue(urlMapping.createdAt >= beforeCreation, "createdAt은 생성 전 시간보다 크거나 같아야 합니다")
        assertTrue(urlMapping.createdAt <= afterCreation, "createdAt은 생성 후 시간보다 작거나 같아야 합니다")
    }

    @Test
    @DisplayName("동일한 속성을 가진 UrlMapping은 동등하다 (equals)")
    fun `should be equal when all properties are the same`() {
        // Given
        val shortUrl = ShortUrl("abc")
        val longUrl = LongUrl("https://example.com")
        val createdAt = 1000L
        val mapping1 = UrlMapping(shortUrl, longUrl, createdAt)
        val mapping2 = UrlMapping(shortUrl, longUrl, createdAt)

        // When & Then
        assertEquals(mapping1, mapping2)
        assertEquals(mapping1.hashCode(), mapping2.hashCode())
    }

    @Test
    @DisplayName("createdAt이 다르면 UrlMapping은 같지 않다")
    fun `should not be equal when createdAt is different`() {
        // Given
        val shortUrl = ShortUrl("abc")
        val longUrl = LongUrl("https://example.com")
        val mapping1 = UrlMapping(shortUrl, longUrl, 1000L)
        val mapping2 = UrlMapping(shortUrl, longUrl, 2000L)

        // When & Then
        assertNotEquals(mapping1, mapping2)
    }

    @Test
    @DisplayName("UrlMapping은 불변 객체이다 (data class copy)")
    fun `should be immutable value object`() {
        // Given
        val original = UrlMapping(ShortUrl("original"), LongUrl("https://original.com"), 1000L)

        // When
        val copied = original.copy(shortUrl = ShortUrl("modified"))

        // Then
        assertNotEquals(original, copied)
        assertEquals("original", original.shortUrl.value)
        assertEquals("modified", copied.shortUrl.value)
        assertEquals(original.longUrl, copied.longUrl)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    @DisplayName("UrlMapping의 모든 속성을 변경하여 복사할 수 있다")
    fun `should copy UrlMapping with all properties changed`() {
        // Given
        val original = UrlMapping(ShortUrl("abc"), LongUrl("https://old.com"), 1000L)

        // When
        val copied =
                original.copy(
                        shortUrl = ShortUrl("xyz"),
                        longUrl = LongUrl("https://new.com"),
                        createdAt = 2000L
                )

        // Then
        assertEquals(ShortUrl("xyz"), copied.shortUrl)
        assertEquals(LongUrl("https://new.com"), copied.longUrl)
        assertEquals(2000L, copied.createdAt)
    }
}
