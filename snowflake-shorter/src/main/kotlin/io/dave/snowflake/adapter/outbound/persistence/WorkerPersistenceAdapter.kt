package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.adapter.outbound.persistence.entity.WorkerEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.WorkerRepository
import io.dave.snowflake.domain.model.Worker
import io.dave.snowflake.domain.model.WorkerStatus
import io.dave.snowflake.domain.port.outbound.WorkerPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class WorkerPersistenceAdapter(
    private val workerRepository: WorkerRepository
) : WorkerPort {

    override fun findByWorkerNums(workerNums: List<Long>): Flow<Worker> {
        return kotlinx.coroutines.flow.flow {
            val entities = withContext(Dispatchers.IO) {
                workerRepository.findByWorkerNumIn(workerNums)
            }
            entities.forEach { emit(it.toDomain()) }
        }
    }

    override fun findByStatusAndUpdatedAtBefore(status: WorkerStatus, updatedAt: LocalDateTime): Flow<Worker> {
        return kotlinx.coroutines.flow.flow {
            val entities = withContext(Dispatchers.IO) {
                workerRepository.findByStatusAndUpdatedAtBefore(status, updatedAt)
            }
            entities.forEach { emit(it.toDomain()) }
        }
    }

    override suspend fun updateUpdatedAt(workerNums: List<Long>, updatedAt: LocalDateTime): Int = withContext(Dispatchers.IO) {
        workerRepository.updateUpdatedAtByWorkerNums(workerNums, updatedAt)
    }

    override suspend fun cleanseWorkers(workerNums: List<Long>, updatedAt: LocalDateTime): Int = withContext(Dispatchers.IO) {
        workerRepository.cleanseWorkers(workerNums, updatedAt)
    }

    override fun saveAll(workers: Flow<Worker>): Flow<Worker> {
        return kotlinx.coroutines.flow.flow {
            val entities = workers.toList().map { it.toEntity() }
            if (entities.isNotEmpty()) {
                val savedEntities = withContext(Dispatchers.IO) {
                    workerRepository.saveAll(entities)
                }
                savedEntities.forEach { emit(it.toDomain()) }
            }
        }
    }

    companion object {
        fun WorkerEntity.toDomain(): Worker = Worker(
            id = id,
            workerNum = workerNum,
            workerName = workerName,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        fun Worker.toEntity(): WorkerEntity = WorkerEntity(
            id = id,
            workerNum = workerNum,
            workerName = workerName,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
