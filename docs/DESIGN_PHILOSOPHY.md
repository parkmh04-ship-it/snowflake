# 💡 설계 철학 (Design Philosophy)

## 🎯 핵심 가치 (Core Values)

### 1. 성능 최우선 (Performance First)
Snowflake URL Shortener는 Global Scale의 트래픽을 처리해야 하므로, 모든 설계 단계에서 **Low Latency**와 **High Throughput**을 최우선으로 고려합니다.
*   **Kotlin Coroutines**: 경량 스레드를 통한 고효율 동시성 처리
*   **Non-blocking I/O**: 리소스 효율 극대화

### 2. 가독성과 유지보수성 (Readability & Maintainability)
성능을 해치지 않는 선에서, 코드는 읽기 쉽고 이해하기 쉬워야 합니다.
*   **Idiomatic Kotlin**: 코틀린스러운 코드 작성 (Expression body, Extension functions 등)
*   **Hexagonal Architecture**: 도메인 로직과 인프라의 명확한 분리

### 3. 불변성 지향 (Immutability)
데이터의 상태 변경을 최소화하여 사이드 이펙트를 줄이고 예측 가능성을 높입니다.
*   **Data Class**: 모든 도메인 모델과 DTO는 불변 객체로 설계
*   **Pure Functions**: 가능한 한 순수 함수로 로직 구현

---

## 🏗️ 아키텍처 원칙

### Hexagonal Architecture (Ports and Adapters)
외부 세계(Web, DB)의 변경이 핵심 비즈니스 로직(Domain)에 영향을 주지 않도록 격리합니다.

*   **Domain**: 순수 Kotlin 코드. 프레임워크 의존성 없음.
*   **Port**: 도메인이 외부와 소통하는 인터페이스 (Inbound/Outbound).
*   **Adapter**: Port의 구현체. Spring, JPA, HTTP 등 구체적인 기술 사용.

### Domain-Driven Design (DDD)
*   **Ubiquitous Language**: 코드와 비즈니스 용어의 일치
*   **Rich Domain Model**: 도메인 객체가 스스로 행위를 가지도록 설계 (Anemic Domain Model 지양)

---

## 📝 기술적 의사결정 (Decision Log)

### 왜 Spring WebFlux가 아닌 MVC + Coroutines인가?
*   **생산성**: Spring MVC의 익숙한 프로그래밍 모델 유지
*   **가독성**: Reactive Streams(Reactor)의 복잡한 연산자 체이닝 대신, 명령형 코드처럼 읽히는 Coroutines 사용
*   **성능**: Virtual Threads(Project Loom)와 유사한 경량 스레드 모델로 높은 처리량 달성

### 왜 JPA 엔티티에 Data Class를 사용하는가?
*   **간결성**: Boilerplate 코드(Getter/Setter, equals/hashCode) 제거
*   **불변성**: `val` 프로퍼티 사용으로 불변 객체 보장
*   **주의사항**: JPA 스펙상 기본 생성자가 필요하므로, 모든 필드에 기본값을 제공하거나 플러그인 활용 필요
