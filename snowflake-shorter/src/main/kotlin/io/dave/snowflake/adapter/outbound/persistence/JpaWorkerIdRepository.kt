package io.dave.snowflake.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import io.dave.snowflake.adapter.outbound.persistence.entity.QSnowflakeWorkersEntity
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
        val snowflakeWorker = QSnowflakeWorkersEntity.snowflakeWorkersEntity

        // 1. Fetch IDs to lock
        val workerNums = queryFactory
            .select(snowflakeWorker.workerNum)
            .from(snowflakeWorker)
            .where(snowflakeWorker.status.eq(WorkerStatus.IDLE))
            .limit(requiredCount.toLong())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetch()

        if (workerNums.size < requiredCount) {
            throw RuntimeException("Required worker IDs ($requiredCount) are not available. Found only ${workerNums.size}.")
        }

        // 2. Update workers
        queryFactory.update(snowflakeWorker)
            .set(snowflakeWorker.status, WorkerStatus.ACTIVE)
            .set(snowflakeWorker.workerName, instanceId)
            .where(snowflakeWorker.workerNum.`in`(workerNums))
            .execute()

        return workerNums
    }
}
