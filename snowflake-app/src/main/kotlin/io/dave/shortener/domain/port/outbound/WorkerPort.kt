package io.dave.shortener.domain.port.outbound

import io.dave.shortener.domain.model.Worker
import io.dave.shortener.domain.model.WorkerStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface WorkerPort {
    fun findByWorkerNums(workerNums: List<Long>): Flow<Worker>
    fun findByStatusAndUpdatedAtBefore(status: WorkerStatus, updatedAt: LocalDateTime): Flow<Worker>
    suspend fun updateUpdatedAt(workerNums: List<Long>, updatedAt: LocalDateTime): Int
    suspend fun cleanseWorkers(workerNums: List<Long>, updatedAt: LocalDateTime): Int
    fun saveAll(workers: Flow<Worker>): Flow<Worker>
}
