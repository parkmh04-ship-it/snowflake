# 🔗 URL Shortening Logic

## 🎯 개요
긴 URL을 짧은 URL로 변환하는 핵심 로직에 대해 설명합니다. **Snowflake ID**를 생성한 후 이를 **Base62**로 인코딩하여 사용자에게 제공하는 방식을 사용합니다.

---

## 🔄 단축 프로세스 (Shortening Process)

```mermaid
graph TD
    User -->|1. POST /shorten| Filter[Rate Limit Filter]
    Filter -->|2. Check| RedisLimiter[(Redis Limiter)]
    Filter -->|3. Call| Controller[Shorter Handler]
    Controller --> UseCase[ShortenUrlUseCase]
    UseCase -->|4. Generate| Generator[ShortUrlGenerator]
    Generator -->|Snowflake ID| UseCase
    UseCase -->|5. Save Cache| RedisCache[(Redis Cache)]
    UseCase -->|6. Publish| Event[Event Publisher]
    UseCase -->|7. Return 201| User
    Event -.->|Async Batch Save| DB[(MySQL DB)]
```

### 1. 처리율 제한 (Rate Limiting)
서비스 안정성을 위해 `RateLimitFilter`가 모든 `/shorten` 요청을 선제적으로 차단하거나 허용합니다. Redis Lua 스크립트를 사용하여 원자적(Atomic)으로 카운트를 관리합니다.

### 2. 고유 키 생성 (ID Generation & Encoding)
1.  [Snowflake 알고리즘](ID_GENERATION.md)으로 64비트 유일 정수를 생성합니다.
2.  **Base62** 인코딩을 통해 `http://sh.rt/aB34X`와 같은 짧은 키로 변환합니다.
3.  도메인 서비스(`ShortUrlGenerator`) 내에서 저장소에 키가 존재하는지 확인(Collision Check)하여 충돌을 방지합니다.

### 3. 이중 저장 전략 (Multi-Layer Persistence)
1.  **Write-Through (Cache)**: 생성된 정보는 Redis에 **즉시** 저장(`awaitSingleOrNull`)되어, 이어지는 첫 번째 조회부터 즉각적인 성능을 보장합니다.
2.  **Write-Behind (Database)**: 실제 영구 저장은 이벤트를 통해 비동기로 처리됩니다. 이벤트 리스너가 버퍼링 후 배치(Batch)로 MySQL에 저장하며, 실패 시 [DLQ](DLQ.md)로 이관됩니다.

---

## 🔍 조회 및 리다이렉트 (Retrieval)

1.  사용자가 단축 URL로 접속 (`GET /{shortKey}`)
2.  **Redis Cache** 조회 (Cache Hit 시 즉시 리턴)
3.  Cache Miss 시 **DB** 조회 후 캐시 적재 (Read-Through)
4.  원본 URL로 `302 Found` 리다이렉트 응답 (브라우저 캐싱 방지 또는 301 사용 결정 가능)
