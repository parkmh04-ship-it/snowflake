package io.dave.snowflake.adapter.outbound.persistence.entity

import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.model.FailedEventStatus
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.ShortUrl
import jakarta.persistence.*

/** Dead Letter Queue의 실패 이벤트를 저장하는 JPA 엔티티입니다. */
@Entity
@Table(
        name = "failed_events",
        indexes =
                [
                        Index(name = "idx_status", columnList = "status"),
                        Index(name = "idx_failed_at", columnList = "failed_at"),
                        Index(name = "idx_status_retry_count", columnList = "status,retry_count")]
)
class FailedEventEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(name = "short_url", nullable = false, length = 255) val shortUrl: String,
        @Column(name = "long_url", nullable = false, columnDefinition = "TEXT") val longUrl: String,
        @Column(name = "created_at", nullable = false) val createdAt: Long,
        @Column(name = "failed_at", nullable = false) val failedAt: Long,
        @Column(name = "retry_count", nullable = false) val retryCount: Int = 0,
        @Column(name = "last_error", columnDefinition = "TEXT") val lastError: String? = null,
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 50)
        val status: FailedEventStatus = FailedEventStatus.PENDING
) {
    /** 엔티티를 도메인 모델로 변환합니다. */
    fun toDomain(): FailedEvent {
        return FailedEvent(
                id = id,
                shortUrl = ShortUrl(shortUrl),
                longUrl = LongUrl(longUrl),
                createdAt = createdAt,
                failedAt = failedAt,
                retryCount = retryCount,
                lastError = lastError,
                status = status
        )
    }

    companion object {
        /** 도메인 모델로부터 엔티티를 생성합니다. */
        fun fromDomain(failedEvent: FailedEvent): FailedEventEntity {
            return FailedEventEntity(
                    id = failedEvent.id,
                    shortUrl = failedEvent.shortUrl.value,
                    longUrl = failedEvent.longUrl.value,
                    createdAt = failedEvent.createdAt,
                    failedAt = failedEvent.failedAt,
                    retryCount = failedEvent.retryCount,
                    lastError = failedEvent.lastError,
                    status = failedEvent.status
            )
        }
    }
}
