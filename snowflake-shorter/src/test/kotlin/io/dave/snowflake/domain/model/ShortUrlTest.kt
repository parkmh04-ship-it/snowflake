package io.dave.snowflake.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * ShortUrl 값 객체의 비즈니스 규칙과 불변성을 검증하는 단위 테스트입니다.
 *
 * 테스트 범위:
 * - 단축 URL 유효성 검증 (빈 값 검사)
 * - 불변성 (Value Object 특성)
 * - 동등성 (equals/hashCode)
 */
@DisplayName("ShortUrl 도메인 모델 테스트")
class ShortUrlTest {

    @Test
    @DisplayName("유효한 값으로 ShortUrl을 생성한다")
    fun `should create ShortUrl with valid value`() {
        // Given
        val validValue = "abc123"

        // When
        val shortUrl = ShortUrl(validValue)

        // Then
        assertEquals(validValue, shortUrl.value)
    }

    @ParameterizedTest(name = "빈 값: \"{0}\"")
    @ValueSource(strings = ["", " ", "  ", "\t", "\n"])
    @DisplayName("빈 값으로 ShortUrl을 생성하면 예외가 발생한다")
    fun `should throw exception when creating ShortUrl with blank value`(blankValue: String) {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> { ShortUrl(blankValue) }
        assertEquals("단축 URL은 비어있을 수 없습니다", exception.message)
    }

    @Test
    @DisplayName("Base62 인코딩된 문자열로 ShortUrl을 생성한다")
    fun `should create ShortUrl with base62 encoded string`() {
        // Given
        val base62Value = "wFjR2pTqYx"

        // When
        val shortUrl = ShortUrl(base62Value)

        // Then
        assertEquals(base62Value, shortUrl.value)
    }

    @Test
    @DisplayName("동일한 값을 가진 ShortUrl은 동등하다 (equals)")
    fun `should be equal when values are the same`() {
        // Given
        val value = "abc123"
        val shortUrl1 = ShortUrl(value)
        val shortUrl2 = ShortUrl(value)

        // When & Then
        assertEquals(shortUrl1, shortUrl2)
        assertEquals(shortUrl1.hashCode(), shortUrl2.hashCode())
    }

    @Test
    @DisplayName("서로 다른 값을 가진 ShortUrl은 같지 않다")
    fun `should not be equal when values are different`() {
        // Given
        val shortUrl1 = ShortUrl("abc123")
        val shortUrl2 = ShortUrl("xyz789")

        // When & Then
        assertNotEquals(shortUrl1, shortUrl2)
    }

    @Test
    @DisplayName("ShortUrl은 불변 객체이다 (data class copy)")
    fun `should be immutable value object`() {
        // Given
        val original = ShortUrl("abc123")

        // When
        val copied = original.copy(value = "xyz789")

        // Then
        assertNotEquals(original, copied)
        assertEquals("abc123", original.value)
        assertEquals("xyz789", copied.value)
    }

    @Test
    @DisplayName("ShortUrl은 toString()으로 값을 표현한다")
    fun `should represent as string via toString`() {
        // Given
        val shortUrl = ShortUrl("testUrl")

        // When
        val stringRepresentation = shortUrl.toString()

        // Then
        assertTrue(stringRepresentation.contains("testUrl"))
    }
}
