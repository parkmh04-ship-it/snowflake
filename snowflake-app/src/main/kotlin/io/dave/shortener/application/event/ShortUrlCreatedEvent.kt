package io.dave.shortener.application.event

import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.ShortUrl
import java.time.LocalDateTime // 이 import는 더 이상 필요 없으므로 제거 예정

data class ShortUrlCreatedEvent(
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: Long // LocalDateTime -> Long 변경
)