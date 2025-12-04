package io.dave.snowflake.id.adapter.outbound.persistence.repository

import io.dave.snowflake.id.adapter.outbound.persistence.entity.GlobalTransactionIdEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GlobalTransactionIdRepository : JpaRepository<GlobalTransactionIdEntity, Long>
