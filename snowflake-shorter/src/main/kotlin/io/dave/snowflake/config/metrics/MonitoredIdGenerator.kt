package io.dave.snowflake.config.metrics

import io.dave.snowflake.domain.generator.IdGenerator
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer

/** IdGenerator의 실행 시간을 측정하고 메트릭을 수집하는 Decorator. */
class MonitoredIdGenerator(
        private val delegate: IdGenerator,
        meterRegistry: MeterRegistry,
        tags: Map<String, String> = emptyMap()
) : IdGenerator {

    private val timer =
            Timer.builder("snowflake.id.generation.time")
                    .description("Time taken to generate a Snowflake ID")
                    .tags(tags.map { Tag.of(it.key, it.value) })
                    .register(meterRegistry)

    override suspend fun nextId(): Long {
        val sample = Timer.start()
        try {
            return delegate.nextId()
        } finally {
            sample.stop(timer)
        }
    }
}
