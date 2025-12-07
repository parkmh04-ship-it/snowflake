package io.dave.snowflake.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class VirtualThreadConfig {

    @Bean("virtualThreadExecutor")
    fun virtualThreadExecutor(meterRegistry: MeterRegistry): ExecutorService =
        Executors.newVirtualThreadPerTaskExecutor().also { VirtualThreadMetrics().bindTo(meterRegistry) }

    @Bean("virtualThreadDispatcher")
    fun virtualThreadDispatcher(
        @Qualifier("virtualThreadExecutor")
        executor: ExecutorService
    ): CoroutineDispatcher = executor.asCoroutineDispatcher()
}

