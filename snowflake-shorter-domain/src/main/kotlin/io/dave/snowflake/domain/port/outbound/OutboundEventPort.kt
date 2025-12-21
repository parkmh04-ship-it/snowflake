package io.dave.snowflake.domain.port.outbound

import io.dave.snowflake.domain.model.UrlMapping

/** 외부로 이벤트를 발행하기 위한 포트. */
fun interface OutboundEventPort {
    suspend fun publish(mapping: UrlMapping)
}
