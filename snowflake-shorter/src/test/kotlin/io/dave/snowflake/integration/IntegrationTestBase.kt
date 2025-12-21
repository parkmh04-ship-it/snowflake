package io.dave.snowflake.integration

/**
 * H2 인메모리 데이터베이스와 Redis Testcontainer를 사용하는 통합 테스트 기본 클래스입니다.
 *
 * **특징**:
 * - H2 인메모리 데이터베이스 사용 (MySQL 호환 모드)
 * - Redis Testcontainer 사용: 격리된 Redis 환경 제공
 * - 테스트 격리: 각 테스트 실행 시 깨끗한 DB 상태
 * - 테스트용 ID 생성기: Worker 초기화 없이 간단한 순차 ID 사용
 */
import io.dave.snowflake.adapter.outbound.persistence.entity.SnowflakeWorkersEntity
import io.dave.snowflake.adapter.outbound.persistence.repository.FailedEventRepository
import io.dave.snowflake.adapter.outbound.persistence.repository.ShortUrlRepository
import io.dave.snowflake.adapter.outbound.persistence.repository.WorkerRepository
import io.dave.snowflake.domain.model.Worker
import io.dave.snowflake.domain.model.WorkerStatus
import io.dave.snowflake.integration.config.TestSnowflakeConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSnowflakeConfig::class)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
abstract class IntegrationTestBase {

    companion object {
        private var containerId: String? = null
        private var redisPort: Int = 6379

        init {
            setupRedis()
            Runtime.getRuntime().addShutdownHook(Thread {
                stopRedis()
            })
        }

        private fun setupRedis() {
            try {
                // Docker 컨테이너 실행
                val process = ProcessBuilder("/usr/local/bin/docker", "run", "-d", "-P", "redis:7.0.12-alpine")
                    .start()
                containerId = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()

                if (containerId != null) {
                    // 매핑된 포트 확인
                    val portProcess = ProcessBuilder("/usr/local/bin/docker", "port", containerId!!, "6379")
                        .start()
                    val portOutput = BufferedReader(InputStreamReader(portProcess.inputStream)).readLine()?.trim()
                    redisPort = portOutput?.substringAfterLast(":")?.toInt() ?: 6379
                    println("Redis container started: $containerId on port $redisPort")
                }
            } catch (e: Exception) {
                println("Failed to start Redis container via Docker: ${e.message}")
            }
        }

        private fun stopRedis() {
            containerId?.let {
                try {
                    ProcessBuilder("/usr/local/bin/docker", "rm", "-f", it).start().waitFor()
                    println("Redis container stopped: $it")
                } catch (e: Exception) {
                    println("Failed to stop Redis container: ${e.message}")
                }
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { redisPort }
        }
    }

    @Autowired
    protected lateinit var shortUrlRepository: ShortUrlRepository

    @Autowired
    protected lateinit var failedEventRepository: FailedEventRepository

    @Autowired
    protected lateinit var workerRepository: WorkerRepository

    @AfterEach
    fun cleanup() {
        shortUrlRepository.deleteAll()
        failedEventRepository.deleteAll()
        workerRepository.deleteAll()
    }

    @BeforeEach
    fun init() {
        workerRepository.saveAll(
            (0..10).map {
                with(it.toLong()) {
                    SnowflakeWorkersEntity.fromDomain(
                        Worker(
                            workerNum = this,
                            workerName = "NONE",
                            status = WorkerStatus.IDLE,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )
                }
            }
        )
    }
}
