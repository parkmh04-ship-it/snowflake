package io.dave.shortener.adapter.outbound.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("shortener_history")
data class ShortUrlEntity(
    @Id
    val id: Long? = null,
    val shortUrl: String,
    val longUrl: String,
    val createdAt: LocalDateTime? = null
)