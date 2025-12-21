# 🛡️ 보안 설계 및 지침 (Security Guide)

Snowflake URL Shorter는 고가용성과 데이터 보호를 위해 다층적인 보안 전략을 채택하고 있습니다. 본 문서는 구현된 보안 기능의 상세 동작 원리와 설정 방법을 설명합니다.

---

## 1. 요청 처리율 제한 (Rate Limiting)

DDoS 공격 방지 및 악의적인 ID 스캐닝 작업을 차단하기 위해 IP 기반의 Rate Limiting을 수행합니다.

### ⚙️ 동작 원리

* **알고리즘**: 토큰 버킷 (Token Bucket) 알고리즘 사용.
* **기술 스택**: Redis + Lua Script (원자성 보장).
* **임계치**: 기본 IP당 1분간 100회 요청 허용.
* **작동 계층**: WebFilter (`RateLimitFilter`) 계층에서 라우터 진입 전 차단.

### 📝 Lua Script 상세

Redis의 원자적 연산을 위해 다음과 같은 Lua 스크립트를 사용합니다:

1. 버킷에 남은 토큰 양 확인.
2. 마지막 보충 시간 이후 경과 시간에 따라 토큰 자동 보충.
3. 요청 수락 시 토큰 1개 감소 및 성공 반환, 부족 시 실패 반환.

---

## 2. 로그 마스킹 (Log Masking)

로그 파일에 개인정보(PII)나 비밀번호, API Key 등의 민감 정보가 평문으로 남지 않도록 합니다.

### 🔍 마스킹 규칙

* **Email**: `user@example.com` -> `u***@example.com`
* **Query Parameters**: URL에 포함된 `token`, `apiKey`, `secret` 등의 값을 `******`로 치환.
* **Exception Message**: 전역 에러 핸들러(`GlobalErrorWebExceptionHandler`)를 통해 외부로 나가는 에러 메시지에도 동일 마스킹 적용.

---

## 3. 입력 유효성 검증 (Strict Validation)

잘못된 형식의 데이터가 시스템 내부로 유입되어 성능 저하나 오작동을 일으키는 것을 방지합니다.

### ✅ 검증 항목 (`ShortenRequest`)

* **@NotBlank**: URL은 필수값입니다.
* **@Size(max = 2048)**: URL 최대 길이를 제한하여 메모리 기반 공격 방지.
* **@Pattern**: `http://` 또는 `https://`로 시작하는 올바른 프로토콜 형식 강제.

---

## 4. 에러 추상화 및 정보 은닉 (Error Abstraction)

시스템의 내부 구조(스택트레이스, DB 쿼리 문구 등)가 외부에 노출되는 것을 차단합니다.

* **HTTP 500**: 내부 오류 발생 시 고정된 메시지("서버 내부 오류가 발생했습니다...")만 반환.
* **표준 포맷**: RFC 7807 스타일을 준수한 JSON 응답 제공.
  ```json
  {
    "status": 500,
    "error": "Internal Server Error",
    "message": "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
  }
  ```

---

## 5. 보안 구성 관리 (Secret Management)

민감한 인프라 설정 정보(DB 비밀번호, Redis 주소 등)는 코드에 하드코딩하지 않습니다.

* **Environment Variables**: `docker-compose.yml` 및 `application.yml`에서 `${VARIABLE_NAME}` 형식을 통해 주입받습니다.
* **Dotenv**: 로컬 개발 환경에서는 `.env.example`을 복사한 `.env` 파일을 통해 로드할 수 있습니다.
