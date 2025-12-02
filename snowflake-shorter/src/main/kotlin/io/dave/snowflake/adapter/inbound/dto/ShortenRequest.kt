package io.dave.snowflake.adapter.inbound.dto

/**
 * URL 단축 요청을 나타내는 데이터 전송 객체(DTO).
 *
 * @property url 단축하고자 하는 원본 URL (필수, 최대 2048자).
 */
data class ShortenRequest(
    val url: String
)