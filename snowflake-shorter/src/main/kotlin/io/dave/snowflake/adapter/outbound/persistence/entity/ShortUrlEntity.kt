package io.dave.snowflake.adapter.outbound.persistence.entity

import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.model.UrlMapping
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "shortener_history")
@EntityListeners(AuditingEntityListener::class)
data class ShortUrlEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val shortUrl: String,

    @Column(nullable = false, length = 2048)
    val longUrl: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
) {
    fun toDomain(): UrlMapping {
        return UrlMapping(
            shortUrl = ShortUrl(this.shortUrl),
            longUrl = LongUrl(this.longUrl),
            createdAt = this.createdAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
        )
    }

    companion object {
        fun fromDomain(urlMapping: UrlMapping): ShortUrlEntity {
            return ShortUrlEntity(
                shortUrl = urlMapping.shortUrl.value,
                longUrl = urlMapping.longUrl.value
            )
        }
    }
}