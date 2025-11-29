package io.dave.shortener.domain.model

/**
 * URL 매핑을 나타내는 도메인 엔티티
 * 단축 URL과 원본 URL의 관계를 표현합니다.
 */
data class UrlMapping(
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: Long = System.currentTimeMillis()
)
