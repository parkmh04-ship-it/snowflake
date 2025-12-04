package io.dave.snowflake.id.adapter.inbound.dto

/**
 * 글로벌 트랜잭션 ID 생성 요청 DTO.
 */
data class GenerateIdRequest(
    val originGlobalTransactionId: Long? = null
)
