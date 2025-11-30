package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.domain.port.WorkerIdRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Repository
class R2dbcWorkerIdRepository(
    private val template: R2dbcEntityTemplate,
    private val operator: TransactionalOperator
) : WorkerIdRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Snowflake 워커 ID 할당 및 업데이트 로직을 수행합니다.
     * 'IDLE' 상태인 워커 ID를 조회하고, 지정된 개수만큼 'ACTIVE' 상태로 변경한 뒤, 할당된 ID 리스트를 반환합니다.
     *
     * @param instanceId 워커를 할당받는 애플리케이션 인스턴스의 고유 식별자.
     * @param requiredCount 필요한 워커 ID의 개수.
     * @return 할당된 워커 ID(Long) 리스트.
     */
    override suspend fun assignWorkerIds(instanceId: String, requiredCount: Int): List<Long> {
        return operator.executeAndAwait {
            // 1. 'IDLE' 상태인 워커 번호 조회 (FOR UPDATE 포함)
            val query = """
                SELECT worker_num FROM snowflake_workers WHERE status = 'IDLE' LIMIT $requiredCount FOR UPDATE
            """.trimIndent()

            val workerNums = template.databaseClient.sql(query)
                .map { row ->
                    @Suppress("kotlin:S6339") // Prefer explicit type check over indexed access
                    row.get("worker_num", Long::class.java)!!
                }
                .all()
                .asFlow()
                .toList()

            // 필요한 개수만큼 워커 ID를 찾지 못한 경우 예외 발생
            if (workerNums.size < requiredCount) {
                log.error("Required worker IDs ($requiredCount) are not available. Found only ${workerNums.size} for instance: $instanceId")
                throw RuntimeException("Required worker IDs ($requiredCount) are not available. Found only ${workerNums.size}.")
            }

            // 2. 조회된 워커들을 'ACTIVE' 상태로 업데이트
            val updateQuery = """
                UPDATE snowflake_workers
                SET status = 'ACTIVE', worker_name = :instanceId
                WHERE worker_num IN (:workerNums)
            """.trimIndent()

            val updatedCount = template.databaseClient.sql(updateQuery)
                .bind("instanceId", instanceId)
                .bind("workerNums", workerNums)
                .fetch()
                .awaitRowsUpdated()
            
            // *** 타입 불일치 문제 해결: updatedCount (Int)와 workerNums.size (Int) 비교 ***
            // awaitRowsUpdated()는 Int를 반환하는 것이 일반적입니다.
            // 오류 메시지가 'Long'과 'Int' 비교라고 나왔다면, 컴파일러가 updatedCount를 Long으로 추론했을 수 있습니다.
            // 명시적으로 Int로 캐스팅하여 비교하거나, workerNums.size를 Long으로 캐스팅하여 비교합니다.
            // 여기서는 workerNums.size를 Long으로 캐스팅하는 방식을 선택합니다.
            if (updatedCount.toLong() != workerNums.size.toLong()) { // <-- 수정된 부분
                // 예상치 못한 업데이트 결과 발생 시 로깅 또는 예외 처리
                log.warn("Expected to update ${workerNums.size} workers, but actually updated $updatedCount for instance: $instanceId")
            }

            log.info("Successfully assigned worker IDs ${workerNums} to instance [$instanceId]. Updated count: $updatedCount")
            workerNums
        }
    }
}