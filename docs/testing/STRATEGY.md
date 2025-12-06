# 🧪 테스트 전략 (Testing Strategy)

## 🎯 개요

Snowflake URL Shortener는 **높은 신뢰성**과 **안정성**을 보장하기 위해 다층적인 테스트 전략을 채택하고 있습니다. 단위 테스트로 비즈니스 로직을 검증하고, 통합 테스트로 전체 시스템의 흐름을 검증합니다.

---

## 🏗️ 테스트 계층 (Test Pyramid)

### 1. 단위 테스트 (Unit Tests)
*   **목적**: 핵심 비즈니스 로직의 정확성 검증
*   **범위**: 도메인 모델, 유스케이스, 유틸리티 클래스
*   **도구**: JUnit 5, Mockk
*   **특징**: 외부 의존성(DB, Network) 없이 고속 실행

### 2. 통합 테스트 (Integration Tests)
*   **목적**: 컴포넌트 간의 상호작용 및 전체 흐름 검증
*   **범위**: Controller -> Service -> Repository -> DB
*   **도구**: Spring Boot Test, H2 Database (MySQL Mode), **WebTestClient**
*   **특징**: WebFlux 환경에 최적화된 비동기/Non-blocking 테스트 클라이언트 사용

---

## 🛠️ 통합 테스트 환경 구성

### 1. 데이터베이스: H2 (MySQL Mode)
Docker 컨테이너 없이 빠른 실행을 위해 H2 인메모리 데이터베이스를 사용하되, `MODE=MySQL` 옵션으로 실제 운영 환경과의 호환성을 유지합니다.

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
```

### 2. 기본 클래스: `IntegrationTestBase`
모든 통합 테스트는 이 클래스를 상속받아 공통 설정을 공유합니다.
*   `@SpringBootTest(webEnvironment = RANDOM_PORT)`: 랜덤 포트에서 서버 실행
*   `@AutoConfigureWebTestClient`: WebTestClient 자동 설정
*   `@ActiveProfiles("test")`: 테스트 프로파일 적용

### 3. HTTP 클라이언트: `WebTestClient`
`TestRestTemplate` 대신 `WebTestClient`를 사용하여 Reactive Stack을 효과적으로 테스트합니다.
*   **Fluent API**: 직관적인 요청/응답 검증 (`expectStatus`, `expectBody` 등)
*   **Non-blocking**: 비동기 요청 처리 검증 용이


---

## ✅ 주요 테스트 시나리오

### URL 단축 (`UrlShortenerIntegrationTest`)
1.  **단축 요청**: POST `/shorten` 요청 시 201 Created 응답 및 단축 URL 반환 확인
2.  **리다이렉트**: 단축 URL 접속 시 302 Found 응답 및 원본 URL `Location` 헤더 확인
3.  **캐시 동작**: 첫 조회 후 두 번째 조회 시 캐시 히트 확인 (응답 속도/로그)
4.  **예외 처리**: 잘못된 URL 형식 요청 시 400 Bad Request 확인

### Dead Letter Queue (`DeadLetterQueueIntegrationTest`)
1.  **저장**: DB 저장 실패 시 `failed_events` 테이블에 저장 확인
2.  **재처리**: `RetryFailedEventsUseCase` 실행 시 상태 전이(PENDING -> RESOLVED) 확인
3.  **영구 실패**: 최대 재시도 횟수 초과 시 FAILED 상태 변경 확인
4.  **정리**: 오래된 RESOLVED 이벤트 삭제 확인
