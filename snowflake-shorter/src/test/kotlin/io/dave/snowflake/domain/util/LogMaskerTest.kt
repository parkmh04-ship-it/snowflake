package io.dave.snowflake.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** LogMasker의 마스킹 로직을 검증하는 단위 테스트. */
@DisplayName("LogMasker 테스트")
class LogMaskerTest {

    @Test
    @DisplayName("이메일 주소의 아이디 부분이 마스킹되어야 한다")
    fun `should mask email part`() {
        assertEquals("te****@example.com", LogMasker.mask("testuser@example.com"))
        assertEquals("****@short.com", LogMasker.mask("a@short.com"))
    }

    @Test
    @DisplayName("민감한 쿼리 파라미터(token, apikey 등) 값이 마스킹되어야 한다")
    fun `should mask sensitive query params`() {
        val input = "accessing https://api.io?token=abc-123&user=dave&apiKey=secret-key"
        val expected = "accessing https://api.io?token=********&user=dave&apiKey=********"
        assertEquals(expected, LogMasker.mask(input))
    }

    @Test
    @DisplayName("널 입력에 대해 빈 문자열을 반환해야 한다")
    fun `should handle null input`() {
        assertEquals("", LogMasker.mask(null))
    }

    @Test
    @DisplayName("민감 정보가 없는 문자열은 그대로 반환해야 한다")
    fun `should not change normal strings`() {
        val normal = "Processing request for URL: https://google.com"
        assertEquals(normal, LogMasker.mask(normal))
    }
}
