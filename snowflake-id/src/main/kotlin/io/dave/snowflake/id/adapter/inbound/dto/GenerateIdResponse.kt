package io.dave.snowflake.id.adapter.inbound.dto

/**
 * 글로벌 트랜잭션 ID 생성 응답 DTO.
 */
data class GenerateIdResponse(
    val globalTransactionId: Long
)
