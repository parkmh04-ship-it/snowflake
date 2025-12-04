package io.dave.snowflake.id.adapter.outbound.event

import io.dave.snowflake.id.application.port.outbound.GlobalTransactionIdPort
import io.dave.snowflake.id.domain.event.GlobalTransactionIdCreatedEvent
import io.dave.snowflake.id.domain.model.GlobalTransactionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GlobalTransactionIdPersistenceEventListener(
    private val port: GlobalTransactionIdPort
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val eventProcessingScope = CoroutineScope(Dispatchers.IO)
    private val eventChannel = Channel<GlobalTransactionIdCreatedEvent>(Channel.UNLIMITED)

    init {
        eventProcessingScope.launch {
            val batch = mutableListOf<GlobalTransactionId>()
            val batchSize = 500
            val flushInterval = 100L

            while (true) {
                val event = withTimeoutOrNull(flushInterval) {
                    eventChannel.receive()
                }

                if (event != null) {
                    batch.add(event.toDomain())
                }

                if (batch.size >= batchSize || (event == null && batch.isNotEmpty())) {
                    flush(batch)
                    batch.clear()
                }
            }
        }
    }

    private suspend fun flush(batch: List<GlobalTransactionId>) {
        try {
            val saved = port.saveAll(batch.asFlow()).toList()
            logger.info("[Event] Successfully persisted {} global transaction IDs in batch.", saved.size)
        } catch (e: Exception) {
            logger.error("[Event Error] Failed to persist batch of global transaction IDs. Count: {}", batch.size, e)
        }
    }

    @EventListener
    fun handleGlobalTransactionIdCreatedEvent(event: GlobalTransactionIdCreatedEvent) {
        if (!eventChannel.trySend(event).isSuccess) {
            logger.warn("[Event] Failed to send event to channel, buffer might be full or closed.")
        }
    }

    private fun GlobalTransactionIdCreatedEvent.toDomain() = GlobalTransactionId(
        globalTransactionId = this.globalTransactionId,
        originGlobalTransactionId = this.originGlobalTransactionId
    )
}
