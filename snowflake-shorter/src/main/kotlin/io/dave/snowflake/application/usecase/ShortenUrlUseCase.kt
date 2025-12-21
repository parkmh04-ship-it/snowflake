package io.dave.snowflake.application.usecase

import io.dave.snowflake.domain.component.ShortUrlGenerator
import io.dave.snowflake.domain.model.LongUrl
import io.dave.snowflake.domain.model.OutboxEvent
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.OutboundEventPort
import io.dave.snowflake.domain.port.outbound.OutboxPort
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 긴 URL을 단축 URL로 변환하고 저장하는 유스케이스 서비스입니다. */
@Service
class ShortenUrlUseCase(
    private val shortUrlGenerator: ShortUrlGenerator,
    private val outboundEventPort: OutboundEventPort,
    private val outboxPort: OutboxPort
) {

    /** 주어진 긴 URL을 단축하고 Outbox에 기록합니다. */
    @Transactional
    suspend fun shorten(longUrlStr: String): UrlMapping {
        val longUrl = LongUrl(longUrlStr)

        // 1. 새로운 단축 URL 생성
        val shortUrl = shortUrlGenerator.generate()
        val newMapping = UrlMapping(shortUrl, longUrl)

        // 2. Outbox에 이벤트 기록 (트랜잭션 내에서 영속화 보장)
        val payload = Json.encodeToString(newMapping)
        outboxPort.save(
            OutboxEvent(
                aggregateType = "UrlMapping",
                aggregateId = newMapping.shortUrl.value,
                payload = payload
            )
        )

        // 3. (Optional) 캐시 등을 위해 즉시 이벤트 발행 - Outbox Relay가 처리할 때까지 기다리지 않고 성능 최적화
        outboundEventPort.publish(newMapping)

        return newMapping
    }
}
