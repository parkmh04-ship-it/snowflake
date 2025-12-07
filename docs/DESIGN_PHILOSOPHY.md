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

### 왜 Reactor(Mono/Flux) 대신 Coroutines인가?
Spring WebFlux는 기본적으로 Reactor를 사용하지만, 우리는 **Kotlin Coroutines**를 선택했습니다.
*   **가독성**: `flatMap`, `zip` 같은 복잡한 연산자 체이닝 없이, 동기 코드처럼 작성하고 비동기로 동작합니다.
*   **생산성**: 명령형 프로그래밍 스타일에 익숙한 개발자가 쉽게 적응할 수 있습니다.
*   **구조화된 동시성**: `CoroutineScope`를 통해 생명주기를 명확하게 관리하고 에러 처리를 일관되게 할 수 있습니다.

### WebFlux 환경에서 왜 R2DBC가 아닌 JPA(JDBC)인가?
완전한 Non-blocking 스택인 R2DBC 대신, **JPA (Hibernate)**를 선택했습니다.
*   **성숙도와 생태계**: JPA의 강력한 ORM 기능, 캐싱, 더티 체킹 등 성숙한 기능을 활용하여 개발 생산성을 높입니다.
*   **Blocking I/O 처리**: JDBC는 Blocking API이므로, **`virtualDispatcher`** 컨텍스트로 감싸서 실행하여 WebFlux의 Event Loop(Netty)가 차단되지 않도록 철저히 격리했습니다.

### 왜 JPA 엔티티에 Data Class를 사용하는가?
*   **간결성**: Boilerplate 코드(Getter/Setter, equals/hashCode) 제거
*   **불변성**: `val` 프로퍼티 사용으로 불변 객체 보장
*   **주의사항**: JPA 스펙상 기본 생성자가 필요하므로, 모든 필드에 기본값을 제공하거나 플러그인 활용 필요

### 왜 Caffeine 대신 Redis를 사용하는가?
초기에는 로컬 캐시인 Caffeine을 사용했으나, **Redis**로 전환했습니다.
*   **분산 환경 지원**: Scale-out 시 모든 인스턴스가 동일한 캐시 데이터를 공유하여 데이터 일관성 보장
*   **지속성**: 애플리케이션 재배포 시에도 캐시 데이터 유지 (Cold Start 방지)
*   **메모리 효율**: JVM 힙 메모리 부담 감소
*   **성능 Trade-off**: 네트워크 I/O 오버헤드가 발생하지만, **Write-Through (Fire-and-Forget)** 패턴과 **Non-blocking I/O**를 통해 지연 시간을 최소화했습니다.
