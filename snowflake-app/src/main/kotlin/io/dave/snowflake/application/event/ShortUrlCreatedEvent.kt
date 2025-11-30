package io.dave.snowflake.application.event

import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl

data class ShortUrlCreatedEvent(
    val shortUrl: ShortUrl,
    val longUrl: LongUrl,
    val createdAt: Long // LocalDateTime -> Long 변경
)