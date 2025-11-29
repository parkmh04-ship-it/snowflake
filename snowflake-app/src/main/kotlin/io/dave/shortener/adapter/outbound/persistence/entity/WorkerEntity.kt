package io.dave.shortener.adapter.outbound.persistence.entity

import io.dave.shortener.domain.model.WorkerStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("snowflake_workers")
data class WorkerEntity(
    @Id
    val id: Long? = null,
    val workerNum: Long,
    val workerName: String,
    val status: WorkerStatus,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
