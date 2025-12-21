package io.dave.snowflake.adapter.outbound.persistence.repository

import io.dave.snowflake.adapter.outbound.persistence.entity.FailedEventsEntity
import io.dave.snowflake.domain.model.FailedEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** FailedEvent 엔티티에 대한 JPA Repository입니다. */
@Repository
interface FailedEventRepository : JpaRepository<FailedEventsEntity, Long> {

    /** 특정 상태의 실패 이벤트를 조회합니다. */
    fun findByStatusOrderByFailedAtAsc(status: FailedEventStatus): List<FailedEventsEntity>

    /** 재시도 가능한 이벤트를 조회합니다. (PENDING 상태이고 재시도 횟수가 MAX_RETRY_COUNT 미만) */
    @Query(
        """
        SELECT e FROM FailedEventsEntity e 
        WHERE e.status = 'PENDING' 
        AND e.retryCount < :maxRetryCount 
        ORDER BY e.failedAt ASC
    """
    )
    fun findRetryableEvents(@Param("maxRetryCount") maxRetryCount: Int): List<FailedEventsEntity>

    /** RESOLVED 상태의 오래된 이벤트를 삭제합니다. */
    @Modifying
    @Transactional
    @Query(
        """
        DELETE FROM FailedEventsEntity e 
        WHERE e.status = 'RESOLVED' 
        AND e.failedAt < :olderThan
    """
    )
    fun deleteResolvedOlderThan(@Param("olderThan") olderThan: Long): Int
}
