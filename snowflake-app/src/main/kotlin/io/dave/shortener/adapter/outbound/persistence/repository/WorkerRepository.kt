package io.dave.shortener.adapter.outbound.persistence.repository

import io.dave.shortener.adapter.outbound.persistence.entity.WorkerEntity
import io.dave.shortener.domain.model.WorkerStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface WorkerRepository : CoroutineCrudRepository<WorkerEntity, Long> {

    fun findByWorkerNumIn(workerNums: List<Long>): Flow<WorkerEntity>

    fun findByStatusAndUpdatedAtBefore(status: WorkerStatus, updatedAt: LocalDateTime): Flow<WorkerEntity>

    @Modifying
    @Query("UPDATE snowflake_workers SET updated_at = :updatedAt WHERE worker_num IN (:workerNums)")
    suspend fun updateUpdatedAtByWorkerNums(workerNums: List<Long>, updatedAt: LocalDateTime): Int

    @Modifying
    @Query("UPDATE snowflake_workers SET status = 'IDLE', worker_name = 'NONE', updated_at = :updatedAt WHERE worker_num IN (:workerNums)")
    suspend fun cleanseWorkers(workerNums: List<Long>, updatedAt: LocalDateTime): Int
}
