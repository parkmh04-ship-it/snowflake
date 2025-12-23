package io.dave.snowflake.adapter.inbound.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * URL 단축 성공 응답을 나타내는 데이터 전송 객체(DTO).
 *
 * @property shortUrl 생성된 단축 URL.
 * @property originalUrl 요청된 원본 URL.
 * @property createdAt 단축 URL이 생성된 시간 (타임스탬프).
 */
data class ShortenResponse(
    @field:Schema(description = "생성된 단축 URL", example = "http://localhost:8080/s/A1b2C")
    val shortUrl: String,

    @field:Schema(description = "요청된 원본 URL", example = "https://www.google.com")
    val originalUrl: String,

    @field:Schema(description = "생성 일시 (Timestamp)", example = "1704067200000")
    val createdAt: Long
)