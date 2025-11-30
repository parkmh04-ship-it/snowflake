package io.dave.snowflake.domain.generator

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.atomic.AtomicInteger

class PooledIdGenerator(
    private val snowflakeIdGenerators: List<SnowflakeIdGenerator>,
    meterRegistry: MeterRegistry
) {

    private val index = AtomicInteger(0)
    private val timer = Timer.builder("snowflake.id.generation.time")
        .description("Time taken to generate a snowflake ID")
        .register(meterRegistry)

    init {
        require(snowflakeIdGenerators.isNotEmpty()) { "SnowflakeIdGenerator list cannot be empty." }
    }

    suspend fun nextId(): Long {
        // Round-Robin 방식으로 다음 팩토리를 선택합니다.
        val currentIndex = index.getAndIncrement() % snowflakeIdGenerators.size
        val factory = snowflakeIdGenerators[currentIndex]

        // ID 생성 시간을 측정합니다.
        val start = System.nanoTime()
        try {
            return factory.nextId()
        } finally {
            timer.record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS)
        }
    }
}