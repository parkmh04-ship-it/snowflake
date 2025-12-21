package io.dave.snowflake.domain.model

import java.time.LocalDateTime

/** Transactional Outbox 패턴을 위한 이벤트 객체. */
data class OutboxEvent(
    val id: Long? = null,
    val aggregateType: String,
    val aggregateId: String,
    val payload: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
