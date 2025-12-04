package io.dave.snowflake.id.application.port.outbound

import io.dave.snowflake.id.domain.model.GlobalTransactionId
import kotlinx.coroutines.flow.Flow

interface GlobalTransactionIdPort {
    fun saveAll(mappings: Flow<GlobalTransactionId>): Flow<GlobalTransactionId>
}
