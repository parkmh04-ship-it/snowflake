package io.dave.snowflake.adapter.outbound.persistence.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "shortener_history")
@EntityListeners(AuditingEntityListener::class)
class ShortUrlEntity(
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
    protected constructor() : this(null, "", "", null)
}