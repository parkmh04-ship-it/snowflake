package io.dave.shortener.adapter.outbound.event

import io.dave.shortener.application.event.ShortUrlCreatedEvent
import io.dave.shortener.domain.port.outbound.UrlPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * ShortUrlCreatedEvent 이벤트를 수신하여 비동기적으로 URL 매핑을 영속화하는 리스너입니다.
 * 이 리스너는 DB 적재 로직을 메인 요청 처리 흐름에서 분리하여 응답 시간을 최소화합니다.
 */
@Component
class UrlPersistenceEventListener(
    private val urlPort: UrlPort
) {
    private val logger = KotlinLogging.logger {}
    private val eventProcessingScope = CoroutineScope(Dispatchers.IO) // 별도의 IO 디스패처에서 이벤트 처리

    @EventListener
    fun handleShortUrlCreatedEvent(event: ShortUrlCreatedEvent) {
        eventProcessingScope.launch {
            logger.debug { "[Event] Received ShortUrlCreatedEvent: ${event.shortUrl.value}" }
            try {
                val urlMapping = urlPort.save(event.toDomain())
                logger.info { "[Event] Successfully persisted short URL: ${urlMapping.shortUrl.value}" }
            } catch (e: Exception) {
                logger.error(e) { "[Event Error] Failed to persist short URL: ${event.shortUrl.value}" }
                // TODO: 실패한 이벤트에 대한 재처리 로직 (Dead-Letter Queue, 메시지 큐 연동 등) 구현 필요
            }
        }
    }

    private fun ShortUrlCreatedEvent.toDomain() = io.dave.shortener.domain.model.UrlMapping(
        shortUrl = this.shortUrl,
        longUrl = this.longUrl,
        createdAt = this.createdAt
    )
}
