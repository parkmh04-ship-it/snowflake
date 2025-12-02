package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.ShortUrlEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ShortUrlRepository : JpaRepository<ShortUrlEntity, Long> {
    fun findByShortUrl(shortUrl: String): ShortUrlEntity?
    fun findByLongUrl(longUrl: String): ShortUrlEntity?
}
