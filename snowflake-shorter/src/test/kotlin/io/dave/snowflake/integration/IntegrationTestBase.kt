package io.dave.snowflake.integration

import io.dave.snowflake.integration.config.TestSnowflakeConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSnowflakeConfig::class)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
abstract class IntegrationTestBase
