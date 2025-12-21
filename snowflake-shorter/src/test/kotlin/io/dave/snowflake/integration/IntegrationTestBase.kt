package io.dave.snowflake.integration

/**
 * MySQL과 Redis Docker 컨테이너를 직접 관리하는 통합 테스트 기본 클래스입니다.
 *
 * **특징**:
 * - MySQL & Redis Docker 컨테이너 직접 실행 (ProcessBuilder 사용)
 * - 테스트 격리: 각 테스트 실행 시 깨끗한 DB 상태 유지 (deleteAll)
 * - 실제 환경과 유사한 통합 테스트 환경 제공
 * - Testcontainers 의존성 없이 Docker만 있으면 실행 가능
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
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSnowflakeConfig::class)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
abstract class IntegrationTestBase {

    companion object {
        private var redisContainerId: String? = null
        private var redisPort: Int = 6379

        private var mysqlContainerId: String? = null
        private var mysqlPort: Int = 3306

        init {
            setupRedis()
            setupMySQL()
            Runtime.getRuntime().addShutdownHook(Thread {
                stopRedis()
                stopMySQL()
            })
        }

        private fun setupRedis() {
            try {
                // Redis Docker 컨테이너 실행
                val process = ProcessBuilder("/usr/local/bin/docker", "run", "-d", "-P", "redis:7.0.12-alpine")
                    .start()
                redisContainerId = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()

                if (redisContainerId != null) {
                    val portProcess = ProcessBuilder("/usr/local/bin/docker", "port", redisContainerId!!, "6379")
                        .start()
                    val portOutput = BufferedReader(InputStreamReader(portProcess.inputStream)).readLine()?.trim()
                    redisPort = portOutput?.substringAfterLast(":")?.toInt() ?: 6379
                    println("Redis container started: $redisContainerId on port $redisPort")
                }
            } catch (e: Exception) {
                println("Failed to start Redis container: ${e.message}")
            }
        }

        private fun setupMySQL() {
            try {
                // MySQL Docker 컨테이너 실행
                val process = ProcessBuilder(
                    "/usr/local/bin/docker", "run", "-d", "-P",
                    "-e", "MYSQL_ROOT_PASSWORD=root",
                    "-e", "MYSQL_DATABASE=snowflake_test",
                    "mysql:8.0",
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci"
                ).start()
                mysqlContainerId = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()

                if (mysqlContainerId != null) {
                    val portProcess = ProcessBuilder("/usr/local/bin/docker", "port", mysqlContainerId!!, "3306")
                        .start()
                    val portOutput = BufferedReader(InputStreamReader(portProcess.inputStream)).readLine()?.trim()
                    mysqlPort = portOutput?.substringAfterLast(":")?.toInt() ?: 3306
                    println("MySQL container started: $mysqlContainerId on port $mysqlPort")

                    waitForMySQL()
                }
            } catch (e: Exception) {
                println("Failed to start MySQL container: ${e.message}")
            }
        }

        private fun waitForMySQL() {
            println("Waiting for MySQL to be ready...")
            val start = System.currentTimeMillis()
            val timeout = TimeUnit.SECONDS.toMillis(60) // 60초 대기

            while (System.currentTimeMillis() - start < timeout) {
                try {
                    // 실제 쿼리를 실행하여 DB 초기화 완료 확인
                    val process = ProcessBuilder(
                        "/usr/local/bin/docker", "exec", mysqlContainerId!!,
                        "mysql", "-u", "root", "-proot", "-e", "SELECT 1"
                    ).start()
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        println("MySQL is ready!")
                        return
                    }
                } catch (e: Exception) {
                    // ignore
                }
                Thread.sleep(1000)
            }
            throw RuntimeException("MySQL failed to start within timeout")
        }

        private fun stopRedis() {
            redisContainerId?.let {
                try {
                    ProcessBuilder("/usr/local/bin/docker", "rm", "-f", it).start().waitFor()
                    println("Redis container stopped: $it")
                } catch (e: Exception) {
                    println("Failed to stop Redis container: ${e.message}")
                }
            }
        }

        private fun stopMySQL() {
            mysqlContainerId?.let {
                try {
                    ProcessBuilder("/usr/local/bin/docker", "rm", "-f", it).start().waitFor()
                    println("MySQL container stopped: $it")
                } catch (e: Exception) {
                    println("Failed to stop MySQL container: ${e.message}")
                }
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // Redis 설정
            registry.add("spring.data.redis.host") { "localhost" }
            registry.add("spring.data.redis.port") { redisPort }

            // MySQL 설정 (H2 덮어쓰기)
            registry.add("spring.datasource.url") { "jdbc:mysql://localhost:$mysqlPort/snowflake_test?allowPublicKeyRetrieval=true&useSSL=false" }
            registry.add("spring.datasource.username") { "root" }
            registry.add("spring.datasource.password") { "root" }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" } // 테스트용 DB 재생성
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
