package io.dave.snowflake.config

import io.dave.snowflake.domain.generator.IdGenerator
import io.dave.snowflake.domain.generator.MonitoredIdGenerator
import io.dave.snowflake.domain.generator.PooledIdGenerator
import io.dave.snowflake.domain.generator.SnowflakeIdGenerator
import io.dave.snowflake.domain.port.WorkerIdRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.util.*

@Configuration
@EnableConfigurationProperties(SnowflakeProperties::class)
class IdGeneratorConfig(
    private val properties: SnowflakeProperties,
    private val workerIdRepository: WorkerIdRepository
) {
    // Kotlin 로거 사용
    private val logger = KotlinLogging.logger {}

    /**
     * 애플리케이션 시작 시점에 필요한 워커 ID를 할당받아 AssignedWorkerInfo 빈을 생성합니다.
     * 동기적으로 실행되어야 하는 Configuration 빈 생성을 위해 Repository의 @Transactional 메소드를 직접 호출합니다.
     */
    @Bean
    fun assignedWorkerInfo(): AssignedWorkerInfo {
        val instanceId = "${InetAddress.getLocalHost().hostName}-${UUID.randomUUID()}"
        val requiredCount = properties.workerThreadCount

        // Repository를 통해 워커 ID를 할당받습니다.
        val assignedWorkerIds = workerIdRepository.assignWorkerIds(instanceId, requiredCount)

        logger.info { "Successfully assigned ${assignedWorkerIds.size} worker IDs to instance [$instanceId]: $assignedWorkerIds" }
        return AssignedWorkerInfo(instanceId, assignedWorkerIds)
    }

    /**
     * 할당받은 워커 ID들을 기반으로 PooledIdGenerator 빈을 생성합니다.
     * 각 SnowflakeIdGenerator는 MonitoredIdGenerator로 래핑되어 메트릭을 수집합니다.
     */
    @Bean
    fun pooledIdGenerator(
        assignedWorkerInfo: AssignedWorkerInfo,
        meterRegistry: MeterRegistry
    ): PooledIdGenerator {
        val idGenerators: List<IdGenerator> = assignedWorkerInfo.workerIds.map { workerId ->
            val generator = SnowflakeIdGenerator(workerId)
            MonitoredIdGenerator(generator, meterRegistry, mapOf("workerId" to workerId.toString()))
        }
        return PooledIdGenerator(idGenerators)
    }
}