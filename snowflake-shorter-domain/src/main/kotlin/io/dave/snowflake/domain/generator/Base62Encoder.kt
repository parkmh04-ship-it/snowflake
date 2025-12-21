package io.dave.snowflake.domain.generator

object Base62Encoder {
    // 62개의 고유한 문자 (0-9, a-z, A-Z)를 무작위로 섞은 알파벳입니다.
    const val ALPHABET = "UljgibvxYZk9nODAJ3tQ4SaGmPuo0KpVsFqNczICwTdMX2rB5yRH6W8efhL17E"
    private const val BASE = 62L

    /**
     * Long 타입의 ID를 Base62 문자열로 인코딩합니다.
     * 0 또는 음수 입력 시 IllegalArgumentException이 발생합니다.
     */
    fun encode(input: Long): String {
        require(input > 0L) { "Input must be a positive number" }

        // Long.MAX_VALUE는 Base62로 인코딩 시 최대 11자리입니다. (62^11 > 2^63)
        // 넉넉하게 12사이즈의 CharArray를 할당합니다.
        val buf = CharArray(12)
        var charPos = 12
        var n = input

        while (n > 0) {
            val rem = (n % BASE).toInt()
            buf[--charPos] = ALPHABET[rem]
            n /= BASE
        }

        return String(buf, charPos, 12 - charPos)
    }
}
