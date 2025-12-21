package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.adapter.outbound.persistence.entity.OutboxEntity
import io.dave.snowflake.config.IOX
import io.dave.snowflake.domain.model.OutboxEvent
import io.dave.snowflake.domain.port.outbound.OutboxPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class OutboxPersistenceAdapter(private val outboxRepository: OutboxRepository) : OutboxPort {

    override suspend fun save(event: OutboxEvent): OutboxEvent =
        withContext(Dispatchers.IOX) {
            outboxRepository.save(OutboxEntity.fromDomain(event)).toDomain()
        }

    override suspend fun findUnprocessedEvents(limit: Int): List<OutboxEvent> =
        withContext(Dispatchers.IOX) {
            outboxRepository.findUnprocessed(PageRequest.of(0, limit)).map { it.toDomain() }
        }

    override suspend fun deleteEvents(ids: List<Long>) =
        withContext(Dispatchers.IOX) { outboxRepository.deleteAllByIdInBatch(ids) }
}
