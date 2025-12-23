package io.dave.snowflake.adapter.inbound.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * URL 단축 요청을 나타내는 데이터 전송 객체(DTO).
 *
 * @property url 단축하고자 하는 원본 URL (필수, http/https 프로토콜 준수, 최대 2048자).
 */
data class ShortenRequest(
    @field:Schema(description = "단축하고자 하는 원본 URL", example = "https://www.google.com")
    @field:NotBlank(message = "URL은 필수 항목입니다.")
    @field:Size(max = 2048, message = "URL은 2048자를 초과할 수 없습니다.")
    @field:Pattern(
        regexp = "^(http|https)://.*",
        message = "URL은 http:// 또는 https://로 시작해야 합니다."
    )
    val url: String
)
