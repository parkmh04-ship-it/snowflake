package io.dave.snowflake.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
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
    fun virtualThreadExecutor(meterRegistry: MeterRegistry): ExecutorService {
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        // 메트릭 모니터링을 위해 ExecutorServiceMetrics로 래핑
        return ExecutorServiceMetrics.monitor(meterRegistry, executor, "virtual_thread_executor")
    }

    @Bean("virtualThreadDispatcher")
    fun virtualThreadDispatcher(
        @Qualifier("virtualThreadExecutor")
        executor: ExecutorService
    ): CoroutineDispatcher {
        val dispatcher = executor.asCoroutineDispatcher()
        virtualDispatcher = dispatcher
        return dispatcher
    }
}

lateinit var virtualDispatcher: CoroutineDispatcher
