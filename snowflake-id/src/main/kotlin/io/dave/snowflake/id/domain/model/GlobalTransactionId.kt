package io.dave.snowflake.id.domain.model

import java.time.LocalDateTime

/**
 * 글로벌 트랜잭션 ID 도메인 모델.
 *
 * @property id 식별자 (DB PK).
 * @property globalTransactionId 생성된 글로벌 트랜잭션 ID.
 * @property originGlobalTransactionId 원거래 글로벌 트랜잭션 ID (취소 거래인 경우).
 * @property createdAt 생성 일시.
 */
data class GlobalTransactionId(
    val id: Long? = null,
    val globalTransactionId: Long,
    val originGlobalTransactionId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
