# ❄️ ID Generation & Worker Management

## 🎯 개요
Snowflake URL Shorter는 분산 환경에서 유일하고 정렬 가능한 ID를 생성하기 위해 Twitter의 **Snowflake 알고리즘**을 사용합니다. 이를 통해 데이터베이스의 Auto-Increment에 의존하지 않고 애플리케이션 레벨에서 고유 ID를 고속으로 생성할 수 있습니다.

---

## 🧮 Snowflake ID 구조
생성되는 ID는 **64비트 Long 정수**이며, 다음과 같이 구성됩니다.

| 비트 (Bits) | 설명 (Description) | 범위/용량 |
| :--- | :--- | :--- |
| **1 bit** | Sign Bit (미사용, 항상 0) | 양수 보장 |
| **41 bits** | Timestamp (Epoch Milliseconds) | 약 69년 (2109년까지 사용 가능) |
| **10 bits** | Worker ID (Node ID) | 0 ~ 1023 (최대 1,024개 노드) |
| **12 bits** | Sequence Number | 0 ~ 4095 (밀리초당 4,096개 ID) |

### 생성 로직 (`SnowflakeIdGenerator.kt`)
1.  **Timestamp**: 현재 시간(ms)을 가져옵니다. 이전 생성 시간보다 작으면(Clock moved backwards) 예외를 발생시켜 중복을 방지합니다.
2.  **Sequence**: 동일 밀리초 내에 요청이 들어오면 시퀀스를 1 증가시킵니다. 시퀀스가 최대값(4095)을 초과하면 다음 밀리초가 될 때까지 대기(Spin-lock)합니다.
3.  **Worker ID**: 애플리케이션 시작 시 할당받은 고유 Worker ID를 사용합니다.

---

## 👷 Worker Management (Dynamic Worker ID Allocation)
분산 환경(Auto-scaling)에서 각 인스턴스에 고유한 Worker ID를 자동으로 할당하고 관리하는 메커니즘입니다.

### 1. 동적 할당 (Dynamic Allocation)
*   애플리케이션이 시작될 때 DB(`WorkerRepository`)를 조회하여 **사용 가능한(IDLE) 가장 낮은 Worker ID**를 점유(ACTIVE 상태로 변경)합니다.
*   `AssignedWorkerInfo` 빈에 할당된 ID가 저장됩니다.

### 2. 하트비트 (Heartbeat)
*   **목적**: 자신이 살아있음을 주기적으로 알립니다.
*   **동작**: `WorkerHeartbeatUseCase`가 주기적으로 실행되어 자신의 Worker 레코드의 `updatedAt` 컬럼을 현재 시간으로 갱신합니다.

### 3. 클렌징 (Cleansing/Reclamation)
*   **목적**: 비정상 종료된 인스턴스가 점유했던 Worker ID를 회수하여 재사용합니다.
*   **동작**: `WorkerCleansingUseCase`가 주기적으로 실행됩니다.
*   **정책**: `updatedAt`이 **5분** 이상 갱신되지 않은 `ACTIVE` 상태의 워커를 찾아 상태를 `IDLE`로 변경하고 회수합니다.

---

## 🛑 동시성 제어 (Concurrency Control)
*   **Kotlin Coroutines Mutex**: `nextId()` 메서드는 `Mutex`로 보호되어, 멀티 스레드(코루틴) 환경에서도 원자성(Atomicity)을 보장합니다.
*   **Optimistic Locking**: Worker ID 할당 시 DB의 동시성 제어 메커니즘을 활용합니다.
