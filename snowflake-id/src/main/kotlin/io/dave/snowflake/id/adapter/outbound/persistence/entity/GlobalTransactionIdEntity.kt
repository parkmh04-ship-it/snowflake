package io.dave.snowflake.id.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "global_transaction",
    indexes = [
        Index(name = "idx_global_trx_id", columnList = "global_transaction_id"),
        Index(name = "idx_origin_global_trx_id", columnList = "origin_global_transaction_id")
    ]
)
class GlobalTransactionIdEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "global_transaction_id", nullable = false)
    val globalTransactionId: Long,

    @Column(name = "origin_global_transaction_id")
    val originGlobalTransactionId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    protected constructor() : this(id = null, globalTransactionId = 0L, originGlobalTransactionId = null, createdAt = LocalDateTime.now())
}
