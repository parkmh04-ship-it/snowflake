package io.dave.snowflake.application.worker

import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.OutboundEventPort
import io.dave.snowflake.domain.port.outbound.OutboxPort
import io.dave.snowflake.domain.port.outbound.UrlPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Outbox 테이블을 폴링하여 처리되지 않은 이벤트를 실제 DB(short_urls)로 이관하는 Relay Worker입니다. 이 워커는 애플리케이션 장애 시에도 데이터
 * 유실을 방지하고 "최소 한 번 이상의 처리(At-least-once)"를 보장합니다.
 */
@Component
class OutboxRelayWorker(
    private val outboxPort: OutboxPort,
    private val urlPort: UrlPort,
    private val outboundEventPort: OutboundEventPort
) {
    private val logger = KotlinLogging.logger {}
    private val isRunning = AtomicBoolean(false)

    /** 주기적으로 처리되지 않은 Outbox 이벤트를 가져와 처리합니다. */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    fun processOutboxEvents() {
        if (!isRunning.compareAndSet(false, true)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = outboxPort.findUnprocessedEvents(limit = 100)
                if (events.isEmpty()) return@launch

                logger.info { "[Outbox] Found ${events.size} unprocessed events. Relaying..." }

                val mappings =
                    events.mapNotNull { event ->
                        try {
                            Json.decodeFromString<UrlMapping>(event.payload)
                        } catch (e: Exception) {
                            logger.error(e) {
                                "[Outbox] Failed to decode event payload: ${event.id}"
                            }
                            null
                        }
                    }

                if (mappings.isNotEmpty()) {
                    // 1. 실제 DB 저장 (Batch)
                    urlPort.saveAll(mappings.asFlow()).toList()

                    // 2. Outbox 이벤트 삭제 (성공 시)
                    outboxPort.deleteEvents(events.mapNotNull { it.id })

                    logger.info {
                        "[Outbox] Successfully relayed ${mappings.size} events to short_urls table."
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "[Outbox Error] Failed to relay outbox events." }
            } finally {
                isRunning.set(false)
            }
        }
    }
}
