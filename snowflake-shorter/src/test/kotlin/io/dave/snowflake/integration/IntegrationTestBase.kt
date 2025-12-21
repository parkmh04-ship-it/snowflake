package io.dave.snowflake.integration

/**
 * H2 인메모리 데이터베이스를 사용하는 통합 테스트 기본 클래스입니다.
 *
 * **특징**:
 * - H2 인메모리 데이터베이스 사용 (Docker 불필요)
 * - 빠른 실행: 컨테이너 시작 오버헤드 없음
 * - 테스트 격리: 각 테스트 실행 시 깨끗한 DB 상태
 * - CI/CD 친화적: 추가 인프라 없이 실행 가능
 * - MySQL 호환 모드: MODE=MySQL로 실제 환경과 유사한 동작
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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSnowflakeConfig::class)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
abstract class IntegrationTestBase {

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
