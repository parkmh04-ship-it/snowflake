package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.ShortUrlEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ShortUrlR2dbcRepository : CoroutineCrudRepository<ShortUrlEntity, Long> {
    suspend fun findByShortUrl(shortUrl: String): ShortUrlEntity?
    suspend fun findByLongUrl(longUrl: String): ShortUrlEntity?
}