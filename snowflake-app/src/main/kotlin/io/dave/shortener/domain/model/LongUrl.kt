package io.dave.shortener.domain.model

/** 원본 URL을 나타내는 값 객체 (Value Object) 타입 안전성을 제공하고 비즈니스 규칙을 강제합니다. */
data class LongUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "URL은 비어있을 수 없습니다" }
        require(value.startsWith("http://") || value.startsWith("https://")) {
            "URL은 http:// 또는 https://로 시작해야 합니다"
        }
    }
}
