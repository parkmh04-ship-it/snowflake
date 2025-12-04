# ❄️ Snowflake URL Shortener

![GitHub top language](https://img.shields.io/github/top-language/parkmh04-ship-it/snowflake)
![GitHub licence](https://img.shields.io/github/license/parkmh04-ship-it/snowflake)
![GitHub issues](https://img.shields.io/github/issues/parkmh04-ship-it/snowflake)
![GitHub stars](https://img.shields.io/github/stars/parkmh04-ship-it/snowflake)

## 🚀 프로젝트 소개

**Snowflake URL Shortener**는 고성능, 분산 환경에 최적화된 URL 단축 서비스입니다. Hexagonal Architecture와 DDD(Domain-Driven Design) 원칙을 엄격히 준수하며, Spring WebFlux와 Kotlin Coroutines를 활용하여 비동기 처리 및 JDBC 접근을 `Dispatchers.IO`로 격리하여 논블로킹 특성을 유지합니다. Twitter Snowflake 알고리즘 기반의 고유 ID 생성 방식으로 높은 처리량과 효율적인 데이터 관리를 제공합니다.

프로젝트는 다음과 같은 모듈로 구성됩니다:
*   **snowflake-core**: Snowflake ID 생성 로직을 담은 코어 라이브러리.
*   **snowflake-shorter**: 단축 URL 서비스 애플리케이션.
*   **snowflake-id**: 글로벌 트랜잭션 ID 생성 서비스.

## ✨ 주요 특징

*   **Hexagonal Architecture & DDD**: 비즈니스 로직과 인프라를 분리하여 높은 유지보수성과 테스트 용이성을 확보했습니다.
*   **Reactive Programming**: Spring WebFlux와 Kotlin Coroutines를 통해 논블로킹 웹 계층을 유지하며, JDBC 블로킹 I/O는 코루틴 `Dispatchers.IO`로 격리하여 높은 동시성을 지원합니다.
*   **Snowflake ID Generator**: 분산 환경에서도 충돌 없는 고유하고 연속적인 단축 URL ID를 생성하여 성능과 스토리지 효율을 극대화합니다。
*   **JPA & QueryDSL**: 성숙한 JPA(Hibernate)와 QueryDSL을 사용하여 안정적이고 강력한 데이터 접근 계층을 제공합니다.

## 🛠️ 기술 스택

*   **언어**: Kotlin (JDK 21)
*   **프레임워크**: Spring Boot 3, Spring WebFlux
*   **리액티브**: Kotlin Coroutines, Reactor (내부적으로 사용)
*   **데이터베이스**: MySQL (JDBC/JPA)
*   **빌드 도구**: Gradle (Kotlin DSL)
*   **테스트**: JUnit 5, Mockk, JaCoCo
*   **코드 품질**: Spotless

## 🚦 시작하기 (Getting Started)

### 📋 요구 사항

*   **Java Development Kit (JDK) 21 이상**
*   **Docker**: MySQL 데이터베이스 실행용
*   **Git**

### 🐳 데이터베이스 설정 (MySQL with Docker)

애플리케이션 실행 전에 MySQL 데이터베이스를 Docker를 통해 실행해야 합니다.
`application.yml`에 기본적으로 `test` 프로파일이 활성화되어 있으며, 이는 Docker Compose 파일 또는 아래 명령어를 통해 실행되는 MySQL 컨테이너에 연결됩니다.

```bash
docker run --name some-mysql -e MYSQL_ROOT_PASSWORD=my-secret-pw -e MYSQL_DATABASE=mydatabase -e MYSQL_USER=app_rw -e MYSQL_PASSWORD=app_rw -p 3306:3306 -d mysql:latest
```

*   **데이터베이스명**: `mydatabase`
*   **사용자**: `app_rw`
*   **비밀번호**: `app_rw`
*   **포트**: `3306`

#### 스키마 초기화

애플리케이션이 시작될 때 `src/main/resources/schema.sql`에 정의된 스키마가 자동으로 적용됩니다. (JPA `ddl-auto` 설정에 따라 동작)

### ⚙️ 애플리케이션 빌드 및 실행

1.  **프로젝트 클론**:
    ```bash
    git clone https://github.com/parkmh04-ship-it/snowflake.git
    cd snowflake
    ```
2.  **빌드**:
    ```bash
    ./gradlew :snowflake-shorter:clean :snowflake-shorter:build
    ```
3.  **애플리케이션 실행**:

    *   **Gradle을 통해 직접 실행** (snowflake-shorter 모듈 실행):
    ```bash
    ./gradlew :snowflake-shorter:bootRun
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
  # snowflake-shorter 디렉토리에서 실행 (또는 프로젝트 루트에서 실행)
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

### 4. 성능 최적화 과정 및 결과 (Performance Optimization & Results)

애플리케이션의 `shorten` API는 초기 구현 단계에서 성능 병목이 있었습니다. 여러 차례의 튜닝과 아키텍처 개선을 통해 다음과 같은 최적의 성능을 달성했습니다.

#### 주요 최적화 포인트:

1.  **Event-Driven Persistence & Batch Processing**: `ShortenUrlUseCase`가 ID 생성 후 `ShortUrlCreatedEvent`를 발행하고 즉시 응답하도록 변경했습니다. `UrlPersistenceEventListener`는 이벤트를 비동기적으로 배치 처리(500개 또는 100ms)하여 DB 적재 부하를 획기적으로 줄였습니다.
2.  **No-Check Strategy**: `shorten` API 호출 시 `longUrl` 중복 검사를 위한 DB 조회(`UrlPort.findByLongUrl`)를 제거했습니다. 이는 매번 새로운 단축 URL을 생성하게 하지만, 동기 DB 조회로 인한 가장 큰 병목을 제거하여 응답 시간을 극적으로 단축했습니다.
3.  **Database Indexing**: `shortener_history` 테이블의 `long_url` 컬럼에 인덱스(`idx_long_url`)를 추가하여, `retrieve` API나 (만약 중복 검사를 다시 도입할 경우) `findByLongUrl`의 조회 성능을 향상시켰습니다。
4.  **JDBC Connection Pool Tuning**: `application-database.yml`에서 HikariCP 연결 풀의 `maximum-pool-size`와 `minimum-idle` 등을 조정하여 충분한 DB 연결 리소스를 확보했습니다.
5.  **Coroutines `Dispatchers.IO` Isolation**: JDBC 블로킹 호출을 `Dispatchers.IO` 컨텍스트로 격리하여 WebFlux의 논블로킹 특성을 유지했습니다。

#### 최종 k6 Load Test 결과 (200 VU, 2분): 

**k6 Output:**
```
  █ TOTAL RESULTS                                                                                                                
                                                                                                                                 
    checks_total.......: 1017978 8482.971858/s                                                                                   
    checks_succeeded...: 100.00% 1017978 out of 1017978                                                                          
    checks_failed......: 0.00%   0 out of 1017978                                                                                
                                                                                                                                 
    ✓ is status 201                                                                                                              
    ✓ response time < 200ms                                                                                                      
                                                                                                                                 
    HTTP                                                                                                                         
    http_req_duration..............: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
      { expected_response:true }...: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
    http_req_failed................: 0.00%  0 out of 508989                                                                    
    http_reqs......................: 508989 4241.485929/s                                                                      
                                                                                                                                 
    EXECUTION                                                                                                                  
    iteration_duration.............: avg=22.04ms min=10.96ms med=19.75ms max=183.57ms p(90)=33.26ms p(95)=39.07ms              
    iterations.....................: 508989 4241.485929/s                                                                      
    vus............................: 1      min=1           max=199                                                            
    vus_max........................: 200    min=200         max=200                                                            
                                                                                                                                 
    NETWORK                                                                                                                    
    data_received..................: 112 MB 933 kB/s                                                                           
    data_sent......................: 94 MB  780 kB/s                                                                           
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
running (2m00.0s), 000/200 VUs, 508989 complete and 0 interrupted iterations                                                   
default ✓ [======================================] 000/200 VUs  2m0s                                                           
                                                                                                                                 
                                                                                                                                 
  █ TOTAL RESULTS                                                                                                              
                                                                                                                                 
    checks_total.......: 1017978 8482.971858/s                                                                                 
    checks_succeeded...: 100.00% 1017978 out of 1017978                                                                        
    checks_failed......: 0.00%   0 out of 1017978                                                                              
                                                                                                                                 
    ✓ is status 201                                                                                                            
    ✓ response time < 200ms                                                                                                    
                                                                                                                                 
    HTTP                                                                                                                       
    http_req_duration..............: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
      { expected_response:true }...: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
    http_req_failed................: 0.00%  0 out of 508989                                                                    
    http_reqs......................: 508989 4241.485929/s                                                                      
                                                                                                                                 
    EXECUTION                                                                                                                  
    iteration_duration.............: avg=22.04ms min=10.96ms med=19.75ms max=183.57ms p(90)=33.26ms p(95)=39.07ms              
    iterations.....................: 508989 4241.485929/s                                                                      
    vus............................: 1      min=1           max=199                                                            
    vus_max........................: 200    min=200         max=200                                                            
                                                                                                                                 
    NETWORK                                                                                                                    
    data_received..................: 112 MB 933 kB/s                                                                           
    data_sent......................: 94 MB  780 kB/s                                                                           
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
running (2m00.0s), 000/200 VUs, 508989 complete and 0 interrupted iterations                                                   
default ✓ [======================================] 000/200 VUs  2m0s                                                           
                                                                                                                                 
                                                                                                                                 
  █ TOTAL RESULTS                                                                                                              
                                                                                                                                 
    checks_total.......: 1017978 8482.971858/s                                                                                 
    checks_succeeded...: 100.00% 1017978 out of 1017978                                                                        
    checks_failed......: 0.00%   0 out of 1017978                                                                              
                                                                                                                                 
    ✓ is status 201                                                                                                            
    ✓ response time < 200ms                                                                                                    
                                                                                                                                 
    HTTP                                                                                                                       
    http_req_duration..............: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
      { expected_response:true }...: avg=11.16ms min=852µs   med=8.83ms  max=171.94ms p(90)=21.85ms p(95)=27.5ms               
    http_req_failed................: 0.00%  0 out of 508989                                                                    
    http_reqs......................: 508989 4241.485929/s                                                                      
                                                                                                                                 
    EXECUTION                                                                                                                  
    iteration_duration.............: avg=22.04ms min=10.96ms med=19.75ms max=183.57ms p(90)=33.26ms p(95)=39.07ms              
    iterations.....................: 508989 4241.485929/s                                                                      
    vus............................: 1      min=1           max=199                                                            
    vus_max........................: 200    min=200         max=200                                                            
                                                                                                                                 
    NETWORK                                                                                                                    
    data_received..................: 112 MB 933 kB/s                                                                           
    data_sent......................: 94 MB  780 kB/s                                                                           
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
                                                                                                                                 
running (2m00.0s), 000/200 VUs, 508989 complete and 0 interrupted iterations                                                   
default ✓ [======================================] 000/200 VUs  2m0s                                                           
                                                                                                                                 
running (2m00.0s), 000/200 VUs, 508989 complete and 0 interrupted iterations                                                   
default ✓ [======================================] 000/200 VUs  2m0s

**핵심 지표 요약:**
*   **평균 응답 시간**: `12.58ms`
*   **95% 응답 시간**: `29.16ms`
*   **초당 처리량 (TPS)**: `3980+ req/s`
*   **성공률**: `100%`

이 결과는 애플리케이션이 높은 부하 상황에서도 매우 빠르고 안정적으로 동작함을 보여줍니다.

## ✅ 테스트 실행

모든 단위 및 통합 테스트를 실행하고 JaCoCo 테스트 커버리지 리포트를 생성합니다.

```bash
./gradlew :snowflake-shorter:clean :snowflake-shorter:test :snowflake-shorter:jacocoTestReport
```

테스트 결과 리포트는 `snowflake-shorter/build/reports/tests/test/index.html`에서, JaCoCo 커버리지 리포트는 `snowflake-shorter/build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.

## 📚 문서 (Documentation)

*   **아키텍처 문서**: [ARCHITECTURE.md](docs/ARCHITECTURE.md)
*   **클래스 다이어그램**: ![class_diagram-Snowflake_URL_Shortener___Class_Diagram__Hexagonal___DDD_.png](docs/class_diagram-Snowflake_URL_Shortener___Class_Diagram__Hexagonal___DDD_.png)
*   **시퀀스 다이어그램**: ![sequence_diagram-Snowflake_URL_Shortener_Sequences__Hexagonal___DDD_.png](docs/sequence_diagram-Snowflake_URL_Shortener_Sequences__Hexagonal___DDD_.png)

## 📄 라이선스

이 프로젝트는 MIT 라이선스에 따라 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하십시오.
