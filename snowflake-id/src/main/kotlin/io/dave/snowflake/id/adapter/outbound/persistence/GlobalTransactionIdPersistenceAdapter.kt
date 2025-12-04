package io.dave.snowflake.id.adapter.outbound.persistence

import io.dave.snowflake.id.adapter.outbound.persistence.entity.GlobalTransactionIdEntity
import io.dave.snowflake.id.adapter.outbound.persistence.repository.GlobalTransactionIdRepository
import io.dave.snowflake.id.application.port.outbound.GlobalTransactionIdPort
import io.dave.snowflake.id.domain.model.GlobalTransactionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class GlobalTransactionIdPersistenceAdapter(
    private val repository: GlobalTransactionIdRepository
) : GlobalTransactionIdPort {

    override fun saveAll(mappings: Flow<GlobalTransactionId>): Flow<GlobalTransactionId> {
        return flow {
            val entities = mappings.toList().map { it.toEntity() }
            if (entities.isNotEmpty()) {
                val savedEntities = withContext(Dispatchers.IO) {
                    repository.saveAll(entities)
                }
                savedEntities.forEach { emit(it.toDomain()) }
            }
        }
    }

    private fun GlobalTransactionId.toEntity() = GlobalTransactionIdEntity(
        globalTransactionId = this.globalTransactionId,
        originGlobalTransactionId = this.originGlobalTransactionId,
        createdAt = this.createdAt
    )

    private fun GlobalTransactionIdEntity.toDomain() = GlobalTransactionId(
        id = this.id,
        globalTransactionId = this.globalTransactionId,
        originGlobalTransactionId = this.originGlobalTransactionId,
        createdAt = this.createdAt
    )
}
