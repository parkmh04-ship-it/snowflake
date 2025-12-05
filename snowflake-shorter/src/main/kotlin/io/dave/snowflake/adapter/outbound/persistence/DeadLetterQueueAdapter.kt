package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.adapter.outbound.persistence.entity.FailedEventEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.FailedEventRepository
import io.dave.snowflake.domain.model.FailedEvent
import io.dave.snowflake.domain.model.FailedEventStatus
import io.dave.snowflake.domain.port.outbound.DeadLetterQueuePort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** DeadLetterQueuePort의 JPA 기반 구현체입니다. JDBC 블로킹 호출을 Dispatchers.IO로 격리하여 논블로킹 특성을 유지합니다. */
@Component
class DeadLetterQueueAdapter(private val failedEventRepository: FailedEventRepository) :
        DeadLetterQueuePort {

    @Transactional
    override suspend fun save(failedEvent: FailedEvent): FailedEvent =
            withContext(Dispatchers.IO) {
                val entity = FailedEventEntity.fromDomain(failedEvent)
                val savedEntity = failedEventRepository.save(entity)
                savedEntity.toDomain()
            }

    @Transactional
    override fun saveAll(failedEvents: Flow<FailedEvent>): Flow<FailedEvent> {
        return failedEvents.map { failedEvent ->
            withContext(Dispatchers.IO) {
                val entity = FailedEventEntity.fromDomain(failedEvent)
                val savedEntity = failedEventRepository.save(entity)
                savedEntity.toDomain()
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findByStatus(status: FailedEventStatus, limit: Int): Flow<FailedEvent> {
        return failedEventRepository
                .findByStatusOrderByFailedAtAsc(status)
                .take(limit)
                .map { it.toDomain() }
                .asFlow()
    }

    @Transactional(readOnly = true)
    override fun findRetryableEvents(limit: Int): Flow<FailedEvent> {
        return failedEventRepository
                .findRetryableEvents(FailedEvent.MAX_RETRY_COUNT)
                .take(limit)
                .map { it.toDomain() }
                .asFlow()
    }

    @Transactional
    override suspend fun update(failedEvent: FailedEvent): FailedEvent =
            withContext(Dispatchers.IO) {
                require(failedEvent.id != null) { "FailedEvent ID must not be null for update" }
                val entity = FailedEventEntity.fromDomain(failedEvent)
                val updatedEntity = failedEventRepository.save(entity)
                updatedEntity.toDomain()
            }

    @Transactional
    override suspend fun deleteById(id: Long): Unit =
            withContext(Dispatchers.IO) { failedEventRepository.deleteById(id) }

    @Transactional
    override suspend fun deleteResolvedOlderThan(olderThanMillis: Long): Int =
            withContext(Dispatchers.IO) {
                failedEventRepository.deleteResolvedOlderThan(olderThanMillis)
            }
}
