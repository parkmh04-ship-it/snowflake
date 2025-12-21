package io.dave.snowflake.domain.port.outbound

import io.dave.snowflake.domain.model.OutboxEvent

/** Outbox 이벤트를 저장하고 관리하기 위한 포트. */
interface OutboxPort {
    suspend fun save(event: OutboxEvent): OutboxEvent

    suspend fun findUnprocessedEvents(limit: Int): List<OutboxEvent>

    suspend fun deleteEvents(ids: List<Long>)
}
