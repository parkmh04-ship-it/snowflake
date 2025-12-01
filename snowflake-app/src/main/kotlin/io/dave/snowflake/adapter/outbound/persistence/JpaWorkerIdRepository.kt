package io.dave.snowflake.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import io.dave.snowflake.adapter.outbound.persistence.entity.QWorkerEntity
import io.dave.snowflake.domain.model.WorkerStatus
import io.dave.snowflake.domain.port.WorkerIdRepository
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaWorkerIdRepository(
    private val queryFactory: JPAQueryFactory
) : WorkerIdRepository {

    @Transactional
    override fun assignWorkerIds(instanceId: String, requiredCount: Int): List<Long> {
        val qWorker = QWorkerEntity.workerEntity

        // 1. Fetch IDs to lock
        val workerNums = queryFactory
            .select(qWorker.workerNum)
            .from(qWorker)
            .where(qWorker.status.eq(WorkerStatus.IDLE))
            .limit(requiredCount.toLong())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetch()

        if (workerNums.size < requiredCount) {
             throw RuntimeException("Required worker IDs ($requiredCount) are not available. Found only ${workerNums.size}.")
        }

        // 2. Update workers
        queryFactory.update(qWorker)
            .set(qWorker.status, WorkerStatus.ACTIVE)
            .set(qWorker.workerName, instanceId)
            .where(qWorker.workerNum.`in`(workerNums))
            .execute()
            
        return workerNums
    }
}
