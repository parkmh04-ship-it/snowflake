package io.dave.snowflake.adapter.outbound.event

import io.dave.snowflake.application.event.ShortUrlCreatedEvent
import io.dave.snowflake.domain.model.UrlMapping
import io.dave.snowflake.domain.port.outbound.OutboundEventPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringOutboundEventAdapter(private val eventPublisher: ApplicationEventPublisher) :
    OutboundEventPort {

    override suspend fun publish(mapping: UrlMapping) {
        eventPublisher.publishEvent(
            ShortUrlCreatedEvent(
                shortUrl = mapping.shortUrl,
                longUrl = mapping.longUrl,
                createdAt = mapping.createdAt
            )
        )
    }
}
