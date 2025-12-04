package io.dave.snowflake.id.application.port.inbound

import io.dave.snowflake.id.domain.model.GlobalTransactionId

/**
 * 글로벌 트랜잭션 ID 생성을 위한 Inbound Port (UseCase).
 */
interface GenerateGlobalTransactionIdUseCase {
    /**
     * 글로벌 트랜잭션 ID를 생성합니다.
     * 원거래 ID가 주어지면 함께 기록합니다.
     *
     * @param originGlobalTransactionId 원거래 글로벌 트랜잭션 ID (Optional).
     * @return 생성된 [GlobalTransactionId] 도메인 객체.
     */
    suspend fun generate(originGlobalTransactionId: Long?): GlobalTransactionId
}
