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
# 1. DB 실행
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun
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

## 🛠️ 기술 스택

*   **Language**: Kotlin 1.9
*   **Framework**: Spring Boot 3.2 (WebFlux)
*   **Database**: MySQL 8.0, H2 (Test)
*   **Concurrency**: Kotlin Coroutines
*   **Build**: Gradle (Kotlin DSL)
