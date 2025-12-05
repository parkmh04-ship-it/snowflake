package io.dave.snowflake.domain.port.outbound

import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.model.FailedEventStatus
import kotlinx.coroutines.flow.Flow

/** Dead Letter Queue 저장소에 대한 아웃바운드 포트입니다. 실패한 이벤트를 저장하고 조회하는 기능을 제공합니다. */
interface DeadLetterQueuePort {

    /**
     * 실패한 이벤트를 DLQ에 저장합니다.
     *
     * @param failedEvent 저장할 실패 이벤트
     * @return 저장된 이벤트 (ID 포함)
     */
    suspend fun save(failedEvent: FailedEvent): FailedEvent

    /**
     * 여러 실패한 이벤트를 배치로 저장합니다.
     *
     * @param failedEvents 저장할 실패 이벤트 Flow
     * @return 저장된 이벤트 Flow
     */
    fun saveAll(failedEvents: Flow<FailedEvent>): Flow<FailedEvent>

    /**
     * 특정 상태의 실패 이벤트를 조회합니다.
     *
     * @param status 조회할 상태
     * @param limit 최대 조회 개수
     * @return 실패 이벤트 Flow
     */
    fun findByStatus(status: FailedEventStatus, limit: Int = 100): Flow<FailedEvent>

    /**
     * 재시도 가능한 실패 이벤트를 조회합니다. (PENDING 상태이고 MAX_RETRY_COUNT 미만인 이벤트)
     *
     * @param limit 최대 조회 개수
     * @return 실패 이벤트 Flow
     */
    fun findRetryableEvents(limit: Int = 100): Flow<FailedEvent>

    /**
     * 실패 이벤트를 업데이트합니다.
     *
     * @param failedEvent 업데이트할 이벤트
     * @return 업데이트된 이벤트
     */
    suspend fun update(failedEvent: FailedEvent): FailedEvent

    /**
     * 특정 ID의 실패 이벤트를 삭제합니다.
     *
     * @param id 삭제할 이벤트 ID
     */
    suspend fun deleteById(id: Long)

    /**
     * RESOLVED 상태의 오래된 이벤트를 삭제합니다.
     *
     * @param olderThanMillis 이 시간보다 오래된 이벤트 삭제 (밀리초)
     * @return 삭제된 이벤트 개수
     */
    suspend fun deleteResolvedOlderThan(olderThanMillis: Long): Int
}
