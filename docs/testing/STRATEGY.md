# 🧪 테스트 전략 (Testing Strategy)

## 🎯 개요

Snowflake URL Shorter는 **높은 신뢰성**과 **안정성**을 보장하기 위해 다층적인 테스트 전략을 채택하고 있습니다. 단위 테스트로 비즈니스 로직을 검증하고, 통합 테스트로 전체 시스템의 흐름을
검증합니다.

---

## 🏗️ 테스트 계층 (Test Pyramid)

### 1. 단위 테스트 (Unit Tests)

* **목적**: 핵심 비즈니스 로직의 정확성 검증
* **범위**: 도메인 모델, 유스케이스, 유틸리티 클래스
* **도구**: JUnit 5, Mockk
* **특징**: 외부 의존성(DB, Network) 없이 고속 실행

### 2. 통합 테스트 (Integration Tests)

* **목적**: 컴포넌트 간의 상호작용 및 전체 흐름 검증
* **범위**: Controller -> Service -> Repository -> DB
* **도구**: Spring Boot Test, MySQL 8.0 (Docker), Redis (Docker), WebTestClient
* **특징**: 실제 스프링 컨텍스트와 실제 데이터베이스를 사용하여 운영 환경과 동일한 조건에서 테스트

---

## 🛠️ 통합 테스트 환경 구성

### 1. 인프라스트럭처: MySQL & Redis (Docker Direct Control)

통합 테스트의 신뢰성을 높이기 위해 H2나 Embedded Redis 대신 **실제 MySQL 8.0과 Redis Docker 컨테이너**를 사용합니다.

* **방식**: `IntegrationTestBase` 클래스에서 `ProcessBuilder`를 사용하여 Docker 컨테이너를 직접 실행하고 관리합니다.
* **이유**: `Testcontainers` 라이브러리의 의존성을 제거하고, 환경 변수나 Docker 소켓 설정 문제 없이 확실하게 컨테이너를 제어하기 위함입니다.
* **동작 방식**:
    1. 테스트 시작 시 (`init` 블록): `docker run` 명령어로 MySQL과 Redis 컨테이너 실행.
    2. 포트 매핑: 동적으로 할당된 호스트 포트를 파싱하여 Spring Boot 설정(`spring.datasource.url` 등)에 주입.
    3. Health Check: `mysql -e "SELECT 1"` 등의 명령어로 서비스가 완전히 준비될 때까지 대기.
    4. 테스트 종료 시 (`ShutdownHook`): `docker rm -f` 명령어로 컨테이너 강제 삭제.

### 2. 기본 클래스: `IntegrationTestBase`

모든 통합 테스트는 이 클래스를 상속받아 공통 설정을 공유합니다.

* `@SpringBootTest(webEnvironment = RANDOM_PORT)`: 랜덤 포트에서 서버 실행
* `@ActiveProfiles("test")`: 테스트 프로파일 적용
* `@Import(TestSnowflakeConfig::class)`: 테스트 전용 설정 로드

### 3. HTTP 클라이언트: `WebTestClient`

Spring WebFlux의 비동기 논블로킹 특성을 테스트하기 위해 `WebTestClient`를 사용합니다.

* API 엔드포인트 호출 및 응답 검증 (Status Code, Body, Header)
* 비동기 처리 흐름 검증

---

## ✅ 주요 테스트 시나리오

### URL 단축 (`UrlShorterIntegrationTest`)

1. **단축 요청**: POST `/shorten` 요청 시 201 Created 응답 및 단축 URL 반환 확인
2. **리다이렉트**: 단축 URL 접속 시 302 Found 응답 및 원본 URL `Location` 헤더 확인
3. **캐시 동작**: 첫 조회 후 두 번째 조회 시 캐시 히트 확인 (Redis 연동 검증)
4. **중복 정책**: 동일 URL 요청 시 매번 새로운 단축 URL 생성 확인 (No-Check Strategy)

### Dead Letter Queue (`DeadLetterQueueIntegrationTest`)

1. **저장**: DB 저장 실패 시 `failed_events` 테이블에 저장 확인
2. **재처리**: `RetryFailedEventsUseCase` 실행 시 상태 전이(PENDING -> RESOLVED) 확인
3. **영구 실패**: 최대 재시도 횟수 초과 시 FAILED 상태 변경 확인
4. **정리**: 오래된 RESOLVED 이벤트 삭제 확인