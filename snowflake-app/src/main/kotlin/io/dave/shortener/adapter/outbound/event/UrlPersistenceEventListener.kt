package io.dave.shortener.adapter.outbound.event

import io.dave.shortener.application.event.ShortUrlCreatedEvent
import io.dave.shortener.domain.port.outbound.UrlPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * ShortUrlCreatedEvent 이벤트를 수신하여 비동기적으로 URL 매핑을 영속화하는 리스너입니다.
 * 이 리스너는 DB 적재 로직을 메인 요청 처리 흐름에서 분리하여 응답 시간을 최소화하고, 배치 처리를 통해 DB 부하를 줄입니다.
 */
@Component
class UrlPersistenceEventListener(
    private val urlPort: UrlPort,
    private val cacheManager: CacheManager
) {
    private val logger = KotlinLogging.logger {}
    private val eventProcessingScope = CoroutineScope(Dispatchers.IO) // 별도의 IO 디스패처에서 이벤트 처리
    private val eventChannel = Channel<ShortUrlCreatedEvent>(Channel.UNLIMITED) // 무제한 버퍼를 가진 채널

    init {
        eventProcessingScope.launch {
            val batch = mutableListOf<io.dave.shortener.domain.model.UrlMapping>()
            val batchSize = 500
            val flushInterval = 100L // 100ms

            while (true) {
                // 1. 이벤트 수신 시도 (타임아웃 적용)
                val event = withTimeoutOrNull(flushInterval) {
                    eventChannel.receive()
                }

                // 2. 이벤트가 있으면 배치에 추가
                if (event != null) {
                    batch.add(event.toDomain())
                }

                // 3. 배치가 가득 찼거나, 타임아웃이 발생했는데 처리할 데이터가 있는 경우 저장
                if (batch.size >= batchSize || (event == null && batch.isNotEmpty())) {
                    flush(batch)
                    batch.clear()
                }
            }
        }
    }

    private suspend fun flush(batch: List<io.dave.shortener.domain.model.UrlMapping>) {
        try {
            // saveAll은 Flow를 받아 Flow를 반환하므로, toList()로 수집(실행)해야 함
            val savedMappings = urlPort.saveAll(batch.asFlow()).toList()
            logger.info { "[Event] Successfully persisted ${savedMappings.size} short URLs in batch." }
            
            // DB 저장 성공 후 캐시 갱신
            savedMappings.forEach { mapping ->
                cacheManager.getCache("shortUrlCache")?.put(mapping.shortUrl.value, mapping)
                cacheManager.getCache("longUrlCache")?.put(mapping.longUrl.value, mapping)
            }

        } catch (e: Exception) {
            logger.error(e) { "[Event Error] Failed to persist batch of short URLs. Count: ${batch.size}" }
            // TODO: 실패한 이벤트에 대한 재처리 로직 구현 (Dead-Letter Queue 등)
        }
    }

    @EventListener
    fun handleShortUrlCreatedEvent(event: ShortUrlCreatedEvent) {
        // 이벤트 채널로 전송. 채널 버퍼가 가득 차면 일시적으로 백프레셔 발생 (여기서는 UNLIMITED이므로 거의 발생 안 함)
        if (!eventChannel.trySend(event).isSuccess) {
            logger.warn { "[Event] Failed to send event to channel, buffer might be full or closed." }
        }
    }

    private fun ShortUrlCreatedEvent.toDomain() = io.dave.shortener.domain.model.UrlMapping(
        shortUrl = this.shortUrl,
        longUrl = this.longUrl,
        createdAt = this.createdAt
    )
}
