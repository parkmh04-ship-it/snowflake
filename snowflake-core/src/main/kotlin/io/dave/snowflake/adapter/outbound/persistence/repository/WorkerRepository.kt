package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.WorkerEntity
import io.dave.snowflake.domain.model.WorkerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface WorkerRepository : JpaRepository<WorkerEntity, Long> {

    fun findByWorkerNumIn(workerNums: List<Long>): List<WorkerEntity>

    fun findByStatusAndUpdatedAtBefore(status: WorkerStatus, updatedAt: LocalDateTime): List<WorkerEntity>

    @Modifying
    @Query("UPDATE WorkerEntity w SET w.updatedAt = :updatedAt WHERE w.workerNum IN :workerNums")
    fun updateUpdatedAtByWorkerNums(@Param("workerNums") workerNums: List<Long>, @Param("updatedAt") updatedAt: LocalDateTime): Int

    @Modifying
    @Query("UPDATE WorkerEntity w SET w.status = 'IDLE', w.workerName = 'NONE', w.updatedAt = :updatedAt WHERE w.workerNum IN :workerNums")
    fun cleanseWorkers(@Param("workerNums") workerNums: List<Long>, @Param("updatedAt") updatedAt: LocalDateTime): Int
}