# ❄️ Snowflake URL Shortener

![GitHub top language](https://img.shields.io/github/top-language/parkmh04-ship-it/snowflake-url-shortener)
![GitHub licence](https://img.shields.io/github/license/parkmh04-ship-it/snowflake-url-shortener)
![GitHub issues](https://img.shields.io/github/issues/parkmh04-ship-it/snowflake-url-shortener)
![GitHub stars](https://img.shields.io/github/stars/parkmh04-ship-it/snowflake-url-shortener)

## 🚀 프로젝트 소개

**Snowflake URL Shortener**는 고성능, 분산 환경에 최적화된 URL 단축 서비스입니다. Hexagonal Architecture와 DDD(Domain-Driven Design) 원칙을 엄격히 준수하며, Spring WebFlux와 Kotlin Coroutines를 활용하여 완전한 논블로킹(Non-blocking) 비동기 처리를 구현했습니다. Twitter Snowflake 알고리즘 기반의 고유 ID 생성 방식으로 높은 처리량과 효율적인 데이터 관리를 제공합니다.

## ✨ 주요 특징

*   **Hexagonal Architecture & DDD**: 비즈니스 로직과 인프라를 분리하여 높은 유지보수성과 테스트 용이성을 확보했습니다.
*   **Reactive Programming**: Spring WebFlux와 Kotlin Coroutines를 통해 논블로킹 I/O를 구현하여 높은 동시성을 지원합니다.
*   **Snowflake ID Generator**: 분산 환경에서도 충돌 없는 고유하고 연속적인 단축 URL ID를 생성하여 성능과 스토리지 효율을 극대화합니다.
*   **R2DBC**: 비동기 데이터베이스 접속을 통해 전체 스택의 논블로킹 특성을 유지합니다.

## 🛠️ 기술 스택

*   **언어**: Kotlin (JDK 21)
*   **프레임워크**: Spring Boot 3, Spring WebFlux
*   **리액티브**: Kotlin Coroutines, Reactor (내부적으로 사용)
*   **데이터베이스**: MariaDB (R2DBC)
*   **빌드 도구**: Gradle (Kotlin DSL)
*   **테스트**: JUnit 5, Mockk, JaCoCo
*   **코드 품질**: Spotless

## 🚦 시작하기 (Getting Started)

### 📋 요구 사항

*   **Java Development Kit (JDK) 21 이상**
*   **Docker**: MariaDB 데이터베이스 실행용
*   **Git**

### 🐳 데이터베이스 설정 (MariaDB with Docker)

애플리케이션 실행 전에 MariaDB 데이터베이스를 Docker를 통해 실행해야 합니다.
`application.yml`에 기본적으로 `test` 프로파일이 활성화되어 있으며, 이는 Docker Compose 파일 또는 아래 명령어를 통해 실행되는 MariaDB 컨테이너에 연결됩니다.

```bash
docker run --name some-mariadb -e MARIADB_ROOT_PASSWORD=my-secret-pw -e MARIADB_DATABASE=mydatabase -e MARIADB_USER=app_rw -e MARIADB_PASSWORD=app_rw -p 3306:3306 -d mariadb:latest
```

*   **데이터베이스명**: `mydatabase`
*   **사용자**: `app_rw`
*   **비밀번호**: `app_rw`
*   **포트**: `3306`

#### 스키마 초기화

애플리케이션이 시작될 때 `src/main/resources/schema.sql`에 정의된 스키마가 자동으로 적용됩니다.

### ⚙️ 애플리케이션 빌드 및 실행

1.  **프로젝트 클론**:
    ```bash
    git clone https://github.com/parkmh04-ship-it/snowflake-url-shortener.git
    cd snowflake-url-shortener/snowflake-app
    ```
2.  **빌드**:
    ```bash
    ./gradlew clean build
    ```
3.  **애플리케이션 실행**:

    *   **Gradle을 통해 직접 실행**:
        ```bash
        ./gradlew bootRun
        ```
    *   **Jar 파일로 실행**:
        ```bash
        java -jar build/libs/snowflake-app-plain.jar
        ```

애플리케이션은 기본적으로 `8080` 포트로 실행됩니다.

## 💻 API 사용법

### 1. URL 단축 (Shorten URL)

`POST /shorten` 엔드포인트를 사용하여 긴 URL을 단축합니다.

*   **URL**: `/shorten`
*   **메소드**: `POST`
*   **Content-Type**: `application/json`

**요청 예시**:
```json
{
    "originalUrl": "https://www.google.com"
}
```

**응답 예시 (201 Created)**:
```json
{
    "shortUrl": "http://localhost:8080/shorten/AbC12d"
}
```

### 2. 단축 URL 조회 및 리다이렉트 (Retrieve URL)

`GET /shorten/{shortUrl}` 엔드포인트로 단축된 URL에 접근하면 원래 URL로 리다이렉션됩니다.

*   **URL**: `/shorten/{shortUrl_key}` (예: `/shorten/AbC12d`)
*   **메소드**: `GET`

**응답**:
*   **302 Found**: 원래 URL로 리다이렉트
*   **404 Not Found**: 단축 URL이 존재하지 않는 경우

### 3. API 문서 (Swagger UI & OpenAPI Docs)

애플리케이션이 실행 중일 때, 다음 URL에서 OpenAPI 3.0 기반의 API 문서를 확인할 수 있습니다.

*   **Swagger UI**: `http://localhost:8080/swagger-ui.html`
*   **OpenAPI JSON Docs**: `http://localhost:8080/api-docs`

## 📊 모니터링 (Monitoring)

이 애플리케이션은 **Micrometer**와 **Prometheus**를 사용하여 애플리케이션 상태 및 성능 메트릭을 제공합니다.

### 1. Prometheus 엔드포인트
애플리케이션 실행 후 다음 엔드포인트에서 Prometheus 형식의 메트릭을 수집할 수 있습니다.
*   `GET /actuator/prometheus`

### 2. 주요 커스텀 메트릭
*   `snowflake_id_generation_time_seconds`: Snowflake ID 생성에 소요되는 시간을 측정하는 히스토그램입니다. 스레드 풀 경합 여부나 성능 저하를 감지하는 데 유용합니다.

## ⚡ 성능 테스트 (Performance Testing)

**k6**를 사용하여 애플리케이션의 성능 및 부하 테스트를 수행할 수 있습니다.

### 1. k6 설치
[k6 설치 가이드](https://grafana.com/docs/k6/latest/get-started/installation/)를 참고하여 시스템에 k6를 설치합니다.
*   **macOS**: `brew install k6`

### 2. 부하 테스트 실행
프로젝트에는 기본적인 부하 테스트 스크립트가 포함되어 있습니다. 애플리케이션을 실행한 상태에서 다음 명령어를 실행하세요.

```bash
# snowflake-app 디렉토리에서 실행
k6 run docs/k6/load-test.js
```

### 3. 테스트 시나리오
프로젝트는 목적에 맞는 다양한 테스트 스크립트를 제공합니다.

#### 기본 부하 테스트 (Load Test)
일반적인 트래픽 상황을 시뮬레이션합니다.
```bash
k6 run docs/k6/load-test.js
```
*   **Warm-up**: 30초 동안 50 VU까지 증가
*   **Load**: 1분 동안 200 VU 유지
*   **Cooldown**: 30초 동안 0 VU로 감소

#### 한계 테스트 (Stress Test)
시스템의 한계 처리량(Max TPS)과 포화 지점을 찾기 위해 점진적으로 부하를 높입니다.
```bash
k6 run docs/k6/stress-test.js
```
*   2분마다 100 VU씩 증가시켜 최대 400 VU까지 테스트합니다.
*   95%의 요청이 500ms 이내에 처리되는지 검증합니다.

#### 스파이크 테스트 (Spike Test)
갑작스러운 트래픽 폭증에 대한 시스템의 반응과 회복력을 테스트합니다.
```bash
k6 run docs/k6/spike-test.js
```
*   10초 만에 1000 VU까지 급격히 부하를 증가시킵니다.
*   시스템이 고부하 상태에서 생존하는지, 그리고 부하가 줄어들 때 정상적으로 회복하는지 확인합니다.

## ✅ 테스트 실행

모든 단위 및 통합 테스트를 실행하고 JaCoCo 테스트 커버리지 리포트를 생성합니다.

```bash
./gradlew clean test jacocoTestReport
```

테스트 결과 리포트는 `snowflake-app/build/reports/tests/test/index.html`에서, JaCoCo 커버리지 리포트는 `snowflake-app/build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.

## 📚 문서 (Documentation)

*   **아키텍처 문서**: [`ARCHITECTURE.md`](ARCHITECTURE.md)
*   **클래스 다이어그램**: [`docs/class_diagram.puml`](docs/class_diagram.puml)
*   **시퀀스 다이어그램**: [`docs/sequence_diagram.puml`](docs/sequence_diagram.puml)

## 📄 라이선스

이 프로젝트는 MIT 라이선스에 따라 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하십시오.
