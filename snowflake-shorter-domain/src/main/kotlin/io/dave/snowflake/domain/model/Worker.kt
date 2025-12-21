package io.dave.snowflake.domain.model

import java.time.LocalDateTime

data class Worker(
    val id: Long? = null,
    val workerNum: Long,
    val workerName: String,
    val status: WorkerStatus,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
