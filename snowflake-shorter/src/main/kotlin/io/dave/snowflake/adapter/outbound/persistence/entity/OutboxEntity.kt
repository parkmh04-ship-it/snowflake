package io.dave.snowflake.adapter.outbound.persistence.entity

import io.dave.snowflake.domain.model.OutboxEvent
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "outbox")
class OutboxEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @Column(nullable = false) val aggregateType: String,
    @Column(nullable = false) val aggregateId: String,
    @Column(nullable = false, columnDefinition = "TEXT") val payload: String,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): OutboxEvent =
        OutboxEvent(
            id = id,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payload = payload,
            createdAt = createdAt
        )

    companion object {
        fun fromDomain(event: OutboxEvent): OutboxEntity =
            OutboxEntity(
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                payload = event.payload,
                createdAt = event.createdAt
            )
    }
}
