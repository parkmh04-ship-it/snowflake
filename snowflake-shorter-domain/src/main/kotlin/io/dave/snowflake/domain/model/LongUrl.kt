package io.dave.snowflake.domain.model

import kotlinx.serialization.Serializable

/** 원본 URL을 나타내는 값 객체 (Value Object) 타입 안전성을 제공하고 비즈니스 규칙을 강제합니다. */
@Serializable
data class LongUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "URL은 비어있을 수 없습니다" }
        require(value.length <= MAX_URL_LENGTH) { "URL은 ${MAX_URL_LENGTH}자를 초과할 수 없습니다" }
        require(value.startsWith("http://") || value.startsWith("https://")) {
            "URL은 http:// 또는 https://로 시작해야 합니다"
        }
    }

    companion object {
        const val MAX_URL_LENGTH = 2048
    }
}
