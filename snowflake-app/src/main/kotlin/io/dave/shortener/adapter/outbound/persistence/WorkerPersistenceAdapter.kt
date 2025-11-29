package io.dave.shortener.adapter.outbound.persistence

import io.dave.shortener.adapter.outbound.persistence.entity.WorkerEntity
import io.dave.shortener.adapter.outbound.persistence.repository.WorkerRepository
import io.dave.shortener.domain.model.Worker
import io.dave.shortener.domain.model.WorkerStatus
import io.dave.shortener.domain.port.outbound.WorkerPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class WorkerPersistenceAdapter(
    private val workerRepository: WorkerRepository
) : WorkerPort {

    override fun findByWorkerNums(workerNums: List<Long>): Flow<Worker> {
        return workerRepository.findByWorkerNumIn(workerNums)
            .map { it.toDomain() }
    }

    override fun findByStatusAndUpdatedAtBefore(status: WorkerStatus, updatedAt: LocalDateTime): Flow<Worker> {
        return workerRepository.findByStatusAndUpdatedAtBefore(status, updatedAt)
            .map { it.toDomain() }
    }

    override suspend fun updateUpdatedAt(workerNums: List<Long>, updatedAt: LocalDateTime): Int {
        return workerRepository.updateUpdatedAtByWorkerNums(workerNums, updatedAt)
    }

    override suspend fun cleanseWorkers(workerNums: List<Long>, updatedAt: LocalDateTime): Int {
        return workerRepository.cleanseWorkers(workerNums, updatedAt)
    }

    override fun saveAll(workers: Flow<Worker>): Flow<Worker> {
        val entities = workers.map { it.toEntity() }
        return workerRepository.saveAll(entities)
            .map { it.toDomain() }
    }

    companion object {
        /**
         * 도메인 모델(Worker)과 영속성 엔티티(WorkerEntity) 간의 변환을 담당하는 확장 함수들을 제공합니다.
         * Hexagonal Architecture의 Adapter 계층에서 도메인과 인프라 간의 매핑을 처리합니다.
         */

        /**
         * WorkerEntity 영속성 엔티티를 Worker 도메인 모델로 변환합니다.
         * @return Worker 변환된 도메인 모델
         */
        fun WorkerEntity.toDomain(): Worker = Worker(
            id = id,
            workerNum = workerNum,
            workerName = workerName,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        /**
         * Worker 도메인 모델을 WorkerEntity 영속성 엔티티로 변환합니다.
         * @return WorkerEntity 변환된 영속성 엔티티
         */
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
