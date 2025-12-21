package io.dave.snowflake.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * LongUrl 값 객체의 비즈니스 규칙과 불변성을 검증하는 단위 테스트입니다.
 *
 * 테스트 범위:
 * - URL 유효성 검증 (빈 값, 프로토콜, 길이 제한)
 * - 불변성 (Value Object 특성)
 * - 동등성 (equals/hashCode)
 */
@DisplayName("LongUrl 도메인 모델 테스트")
class LongUrlTest {

    @Test
    @DisplayName("유효한 HTTP URL로 LongUrl을 생성한다")
    fun `should create LongUrl with valid http url`() {
        // Given
        val validUrl = "http://example.com"

        // When
        val longUrl = LongUrl(validUrl)

        // Then
        assertEquals(validUrl, longUrl.value)
    }

    @Test
    @DisplayName("유효한 HTTPS URL로 LongUrl을 생성한다")
    fun `should create LongUrl with valid https url`() {
        // Given
        val validUrl = "https://example.com/path?query=value"

        // When
        val longUrl = LongUrl(validUrl)

        // Then
        assertEquals(validUrl, longUrl.value)
    }

    @ParameterizedTest(name = "빈 URL: \"{0}\"")
    @ValueSource(strings = ["", " ", "  ", "\t", "\n"])
    @DisplayName("빈 값으로 LongUrl을 생성하면 예외가 발생한다")
    fun `should throw exception when creating LongUrl with blank value`(blankUrl: String) {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> { LongUrl(blankUrl) }
        assertEquals("URL은 비어있을 수 없습니다", exception.message)
    }

    @ParameterizedTest(name = "잘못된 프로토콜: \"{0}\"")
    @ValueSource(
            strings = ["ftp://example.com", "example.com", "www.example.com", "htp://example.com"]
    )
    @DisplayName("http:// 또는 https://로 시작하지 않으면 예외가 발생한다")
    fun `should throw exception when URL does not start with http or https`(invalidUrl: String) {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> { LongUrl(invalidUrl) }
        assertEquals("URL은 http:// 또는 https://로 시작해야 합니다", exception.message)
    }

    @Test
    @DisplayName("최대 길이(2048자)를 초과하는 URL은 예외가 발생한다")
    fun `should throw exception when URL exceeds max length`() {
        // Given
        val longPath = "a".repeat(2050)
        val tooLongUrl = "https://example.com/$longPath"

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { LongUrl(tooLongUrl) }
        assertEquals("URL은 2048자를 초과할 수 없습니다", exception.message)
    }

    @Test
    @DisplayName("최대 길이(2048자) 이내의 URL은 정상 생성된다")
    fun `should create LongUrl when URL is within max length`() {
        // Given
        val longPath = "a".repeat(2000)
        val validLongUrl = "https://example.com/$longPath"

        // When
        val longUrl = LongUrl(validLongUrl)

        // Then
        assertEquals(validLongUrl, longUrl.value)
    }

    @Test
    @DisplayName("동일한 URL 값을 가진 LongUrl은 동등하다 (equals)")
    fun `should be equal when URLs are the same`() {
        // Given
        val url = "https://example.com"
        val longUrl1 = LongUrl(url)
        val longUrl2 = LongUrl(url)

        // When & Then
        assertEquals(longUrl1, longUrl2)
        assertEquals(longUrl1.hashCode(), longUrl2.hashCode())
    }

    @Test
    @DisplayName("서로 다른 URL 값을 가진 LongUrl은 같지 않다")
    fun `should not be equal when URLs are different`() {
        // Given
        val longUrl1 = LongUrl("https://example.com")
        val longUrl2 = LongUrl("https://different.com")

        // When & Then
        assertNotEquals(longUrl1, longUrl2)
    }

    @Test
    @DisplayName("LongUrl은 불변 객체이다 (data class copy)")
    fun `should be immutable value object`() {
        // Given
        val original = LongUrl("https://example.com")

        // When
        val copied = original.copy(value = "https://new.com")

        // Then
        assertNotEquals(original, copied)
        assertEquals("https://example.com", original.value)
        assertEquals("https://new.com", copied.value)
    }

    @Test
    @DisplayName("MAX_URL_LENGTH 상수가 2048로 정의되어 있다")
    fun `MAX_URL_LENGTH should be 2048`() {
        // Then
        assertEquals(2048, LongUrl.MAX_URL_LENGTH)
    }
}
