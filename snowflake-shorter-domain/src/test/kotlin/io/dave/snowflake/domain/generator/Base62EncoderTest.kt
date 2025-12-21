package io.dave.snowflake.domain.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Base62Encoder의 인코딩 로직을 검증하는 단위 테스트입니다.
 *
 * 테스트 범위:
 * - 기본적인 숫자의 Base62 인코딩 정확성
 * - 엣지 케이스 (0, 음수 입력 시 예외 발생)
 * - Snowflake ID 범위의 인코딩
 * - 인코딩된 문자열의 길이 일관성
 */
@DisplayName("Base62Encoder 테스트")
class Base62EncoderTest {

    @Test
    @DisplayName("0을 인코딩하면 IllegalArgumentException이 발생한다")
    fun `encode zero should throw exception`() {
        assertThrows<IllegalArgumentException> {
            Base62Encoder.encode(0L)
        }
    }

    @Test
    @DisplayName("음수를 인코딩하면 IllegalArgumentException이 발생한다")
    fun `encode negative should throw exception`() {
        assertThrows<IllegalArgumentException> {
            Base62Encoder.encode(-1L)
        }
    }

    @Test
    @DisplayName("1을 인코딩하면 알파벳의 두 번째 문자를 반환한다")
    fun `encode one should return second character of alphabet`() {
        // When
        val result = Base62Encoder.encode(1L)

        // Then
        assertEquals(Base62Encoder.ALPHABET[1].toString(), result)
    }

    @Test
    @DisplayName("특정 숫자들이 올바르게 Base62로 인코딩된다")
    fun `encode specific numbers correctly`() {
        // Given: 실제 Base62Encoder 구현 (62자 알파벳)에 따른 인코딩 결과
        val testCases =
            mapOf(
                1L to Base62Encoder.ALPHABET[1].toString(),
                10L to Base62Encoder.ALPHABET[10].toString(),
                61L to Base62Encoder.ALPHABET[61].toString(),
            )

        // When & Then
        testCases.forEach { (input, expected) ->
            val result = Base62Encoder.encode(input)
            assertEquals(expected, result, "ID $input 의 인코딩 결과가 올바르지 않습니다")
        }
    }

    @Test
    @DisplayName("Snowflake ID 범위의 큰 숫자를 인코딩한다")
    fun `encode large snowflake ID successfully`() {
        // Given: 실제 Snowflake ID 범위 (41bit timestamp + 10bit workerId + 12bit sequence)
        val snowflakeId = 1234567890123456L

        // When
        val result = Base62Encoder.encode(snowflakeId)

        // Then
        assertTrue(result.isNotEmpty(), "인코딩된 결과는 비어있지 않아야 합니다")
        assertTrue(result.length <= 11, "Snowflake ID는 Base62로 최대 11자리로 인코딩됩니다")
        assertTrue(result.all { it in Base62Encoder.ALPHABET }, "모든 문자는 알파벳에 포함되어야 합니다")
    }

    @Test
    @DisplayName("Long.MAX_VALUE를 인코딩한다")
    fun `encode max long value successfully`() {
        // Given
        val maxLong = Long.MAX_VALUE

        // When
        val result = Base62Encoder.encode(maxLong)

        // Then
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it in Base62Encoder.ALPHABET })
        // Long.MAX_VALUE는 Base62로 약 11자리
        assertTrue(result.length <= 12, "Long.MAX_VALUE는 Base62로 최대 12자리입니다")
    }

    @Test
    @DisplayName("동일한 입력은 항상 동일한 출력을 생성한다 (멱등성)")
    fun `encode is idempotent`() {
        // Given
        val input = 99999L

        // When
        val result1 = Base62Encoder.encode(input)
        val result2 = Base62Encoder.encode(input)

        // Then
        assertEquals(result1, result2)
    }

    @Test
    @DisplayName("인코딩된 문자열은 알파벳 문자만으로 구성된다")
    fun `encoded string contains only alphabet characters`() {
        // Given
        val testInputs = listOf(1L, 123L, 9999L, 123456789L)

        // When & Then
        testInputs.forEach { input ->
            val result = Base62Encoder.encode(input)
            assertTrue(
                result.all { it in Base62Encoder.ALPHABET },
                "입력 $input 의 인코딩 결과 '$result' 는 알파벳 문자만 포함해야 합니다"
            )
        }
    }

    @Test
    @DisplayName("서로 다른 입력은 서로 다른 출력을 생성한다 (유일성)")
    fun `different inputs produce different outputs`() {
        // Given: Base62이므로 62 미만의 숫자는 모두 다른 단일 문자로 인코딩됨
        // 0은 예외가 발생하므로 1부터 시작
        val inputs = (1L..62L).toList()

        // When
        val encodedSet = inputs.map { Base62Encoder.encode(it) }.toSet()

        // Then
        assertEquals(inputs.size, encodedSet.size, "모든 인코딩 결과는 유일해야 합니다")
    }

    @Test
    @DisplayName("알파벳이 62자인지 검증한다")
    fun `alphabet should have 62 characters`() {
        // Then: 실제 구현은 Base62 (62자 알파벳)
        assertEquals(62, Base62Encoder.ALPHABET.length, "알파벳은 62자여야 합니다")
    }

    @Test
    @DisplayName("알파벳에 중복 문자가 없는지 검증한다")
    fun `alphabet should not have duplicate characters`() {
        // Given
        val alphabet = Base62Encoder.ALPHABET

        // Then
        val uniqueChars = alphabet.toSet()
        assertEquals(alphabet.length, uniqueChars.size, "알파벳에 중복 문자가 없어야 합니다")
    }
}
