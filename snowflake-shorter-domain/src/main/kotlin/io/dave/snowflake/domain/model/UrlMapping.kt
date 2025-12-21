package io.dave.snowflake.domain.model

import kotlinx.serialization.Serializable

/** URL 매핑을 나타내는 도메인 엔티티 단축 URL과 원본 URL의 관계를 표현합니다. */
@Serializable
data class UrlMapping(
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: Long = System.currentTimeMillis()
)
