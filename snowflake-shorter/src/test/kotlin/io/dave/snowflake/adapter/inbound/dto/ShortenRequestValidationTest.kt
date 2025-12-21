package io.dave.snowflake.adapter.inbound.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ShortenRequest Validation 테스트")
class ShortenRequestValidationTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    @DisplayName("올바른 URL 형식은 검증을 통과한다")
    fun `valid url should pass validation`() {
        val request = ShortenRequest("https://google.com")
        val violations = validator.validate(request)
        assertTrue(violations.isEmpty())
    }

    @Test
    @DisplayName("http 또는 https로 시작하지 않는 URL은 실패한다")
    fun `invalid protocol should fail`() {
        val request = ShortenRequest("ftp://malicious.com")
        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("http:// 또는 https://") })
    }

    @Test
    @DisplayName("빈 URL은 실패한다")
    fun `blank url should fail`() {
        val request = ShortenRequest("  ")
        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("필수 항목") })
    }

    @Test
    @DisplayName("2048자를 초과하는 URL은 실패한다")
    fun `too long url should fail`() {
        val longUrl = "https://example.com/" + "a".repeat(2050)
        val request = ShortenRequest(longUrl)
        val violations = validator.validate(request)
        assertTrue(violations.any { it.message.contains("2048자를 초과") })
    }
}
