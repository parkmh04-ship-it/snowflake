package io.dave.snowflake.adapter.outbound.persistence.entity

import io.dave.snowflake.domain.model.WorkerStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "snowflake_workers")
@EntityListeners(AuditingEntityListener::class)
class WorkerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val workerNum: Long,

    @Column(nullable = false)
    val workerName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: WorkerStatus,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    protected constructor() : this(null, 0, "", WorkerStatus.ACTIVE, null, null)
}
