package io.dave.snowflake.domain.util

/** 로그 기록 시 민감한 정보를 보호하기 위한 마스킹 유틸리티. */
object LogMasker {
    // 단순한 이메일 패턴
    private val EMAIL_REGEX = Regex("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")

    // URL 쿼리 파라미터 중 민감할 수 있는 필드 (예: token, secret, password 등)
    private val SENSITIVE_PARAM_REGEX =
        Regex("(?i)(token|secret|password|credential|apikey)=([^&\\s]+)")

    /** 입력 문자열에서 민감한 패턴을 찾아 마스킹 처리합니다. */
    fun mask(input: String?): String {
        if (input == null) return ""

        return maskQueryParams(maskEmail(input))
    }

    private fun maskEmail(input: String): String {
        return EMAIL_REGEX.replace(input) { matchResult ->
            val user = matchResult.groupValues[1]
            val domain = matchResult.groupValues[2]
            val maskedUser = if (user.length > 2) user.take(2) + "****" else "****"
            "$maskedUser@$domain"
        }
    }

    private fun maskQueryParams(input: String): String {
        return SENSITIVE_PARAM_REGEX.replace(input) { matchResult ->
            val key = matchResult.groupValues[1]
            "$key=********"
        }
    }
}
