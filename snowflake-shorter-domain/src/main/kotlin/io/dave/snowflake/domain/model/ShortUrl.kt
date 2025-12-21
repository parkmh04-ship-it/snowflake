package io.dave.snowflake.domain.model

import kotlinx.serialization.Serializable

/** 단축 URL을 나타내는 값 객체 (Value Object) */
@Serializable
data class ShortUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "단축 URL은 비어있을 수 없습니다" }
    }
}
