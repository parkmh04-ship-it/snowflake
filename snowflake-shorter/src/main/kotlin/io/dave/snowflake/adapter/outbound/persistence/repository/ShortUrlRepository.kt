package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.ShorterHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ShortUrlRepository : JpaRepository<ShorterHistoryEntity, Long> {
    fun findByShortUrl(shortUrl: String): ShorterHistoryEntity?
    fun findByLongUrl(longUrl: String): ShorterHistoryEntity?
}
