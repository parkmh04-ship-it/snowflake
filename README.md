# ❄️ Snowflake URL Shorter

> **Global Scale을 지향하는 고성능, 고가용성 URL 단축 서비스**

Snowflake URL Shorter는 Twitter Snowflake 알고리즘과 Hexagonal Architecture를 기반으로 설계된 URL 단축 서비스입니다. 대규모 트래픽 처리를 위한 비동기 아키텍처와 강력한 장애 격리 메커니즘을 갖추고 있습니다.

---

## 🚀 Quick Start

### 요구 사항
*   Java 21+
*   Docker (for MySQL)

### 실행 방법

```bash
# 1. 인프라 실행 (MySQL, Redis, Metrics)
docker-compose up -d

# 2. 애플리케이션 빌드 및 실행
./gradlew :snowflake-shorter:bootRun
```

---

## 📚 문서 (Documentation)

이 프로젝트의 모든 기술적 상세 내용은 `docs/` 디렉토리에 정리되어 있습니다.

### 🏛 아키텍처 & 설계
*   **[ARCHITECTURE.md](docs/ARCHITECTURE.md)**: 전체 시스템 구조 및 데이터 흐름 조감도
*   **[DESIGN_PHILOSOPHY.md](docs/DESIGN_PHILOSOPHY.md)**: 핵심 가치 및 기술적 의사결정 배경 (Why Kotlin? Why Coroutines?)

### 🧩 기능 명세 (Features)
*   **[DLQ.md](docs/features/DLQ.md)**: 장애 격리 및 재처리 메커니즘 (Dead Letter Queue)
*   **[ID_GENERATION.md](docs/features/ID_GENERATION.md)**: Snowflake ID 생성 및 Worker 관리
*   **[URL_SHORTENING.md](docs/features/URL_SHORTENING.md)**: Base62 인코딩 로직

### 🧪 품질 & 테스트
*   **[STRATEGY.md](docs/testing/STRATEGY.md)**: 테스트 전략 및 환경 구성

---

## ️ 보안 기능 (Security)

본 서비스는 안정적인 운영과 데이터 보호를 위해 다음과 같은 보안 계층을 갖추고 있습니다.

*   **Rate Limiting**: Redis Lua Script 기반의 IP별 요청 제한 (1분당 100회)으로 DDoS 및 무분별한 ID 스캐닝 방지.
*   **Log Masking**: PII(개인정보) 및 API Key, Token 등 민감 정보가 로그 파일에 노출되지 않도록 자동 마스킹 처리.
*   **Strict Validation**: Bean Validation을 이용한 URL 형식(http/https) 및 길이(2048자) 상시 검증.
*   **Error Abstraction**: 내부 스택트레이스를 외부에 노출하지 않도록 추상화된 에러 응답 제공.

---

## ⚙️ 환경 설정 (Configuration)

보안 강화를 위해 인프라 계정 정보를 환경 변수로 관리합니다. 프로젝트 루트의 `.env.example` 파일을 참고하여 `.env` 파일을 생성하세요.

```bash
# .env 설정 예시
MYSQL_ROOT_PASSWORD=your_strong_password
GRAFANA_ADMIN_PASSWORD=your_admin_password
```

---

## 🛠️ 기술 스택

*   **Language**: Kotlin 2.1, Java 21 (Virtual Threads Enabled)
*   **Framework**: Spring Boot 3.5.3 (WebFlux)
*   **Database**: MySQL 8.0, Redis (Reactive)
*   **Concurrency**: Kotlin Coroutines (Structured Concurrency)
*   **Build**: Gradle Kotlin DSL (Multi-Module)
