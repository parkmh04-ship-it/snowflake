-- 기존 shortener_history 테이블 삭제 (개발용)
DROP TABLE IF EXISTS shortener_history;

-- shortener_history 테이블 생성 (인덱스 포함)
CREATE TABLE shortener_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(255) NOT NULL UNIQUE,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_long_url (long_url(255))
);

-- 기존 worker_node 테이블 삭제
DROP TABLE IF EXISTS worker_node;

-- Snowflake 워커 ID 할당을 위한 테이블 생성
CREATE TABLE IF NOT EXISTS snowflake_workers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_num BIGINT NOT NULL UNIQUE,
    worker_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 초기 데이터 256개 삽입 (worker_num: 0 ~ 255)
-- H2 Database, MariaDB, MySQL 등에서 재귀 CTE를 지원하는 경우 사용 가능
INSERT INTO snowflake_workers (worker_num, worker_name, status)
WITH RECURSIVE nums(n) AS (
  SELECT 0
  UNION ALL
  SELECT n + 1 FROM nums WHERE n < 255
)
SELECT n, 'NONE', 'IDLE' FROM nums;

