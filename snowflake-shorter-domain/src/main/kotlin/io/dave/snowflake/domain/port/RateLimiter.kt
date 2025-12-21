package io.dave.snowflake.domain.port

/** 요청 처리율 제한(Rate Limiting)을 위한 포트 인터페이스. */
interface RateLimiter {
    /** 특정 식별자(예: IP)에 대해 요청 허용 여부를 반환합니다. */
    suspend fun isAllowed(key: String, limit: Int, windowInSeconds: Int): Boolean
}
