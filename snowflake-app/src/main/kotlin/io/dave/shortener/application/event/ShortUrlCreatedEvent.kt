package io.dave.shortener.application.event

import io.dave.shortener.domain.model.LongUrl
import io.dave.shortener.domain.model.ShortUrl
import java.time.LocalDateTime

data class ShortUrlCreatedEvent(
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
