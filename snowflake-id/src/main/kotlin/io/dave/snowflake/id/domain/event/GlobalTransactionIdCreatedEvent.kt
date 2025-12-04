package io.dave.snowflake.id.domain.event

/**
 * 글로벌 트랜잭션 ID가 생성되었을 때 발생하는 이벤트.
 * 비동기 지속성(Persistence) 처리를 위해 사용됩니다.
 *
 * @property globalTransactionId 생성된 글로벌 트랜잭션 ID.
 * @property originGlobalTransactionId 원거래 글로벌 트랜잭션 ID (Optional).
 */
data class GlobalTransactionIdCreatedEvent(
    val globalTransactionId: Long,
    val originGlobalTransactionId: Long?
)
