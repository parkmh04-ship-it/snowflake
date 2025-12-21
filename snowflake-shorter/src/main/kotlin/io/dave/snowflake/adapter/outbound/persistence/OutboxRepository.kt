package io.dave.snowflake.adapter.outbound.persistence

import io.dave.snowflake.adapter.outbound.persistence.entity.OutboxEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OutboxRepository : JpaRepository<OutboxEntity, Long> {
    @Query("SELECT o FROM OutboxEntity o ORDER BY o.createdAt ASC")
    fun findUnprocessed(pageable: Pageable): List<OutboxEntity>
}
