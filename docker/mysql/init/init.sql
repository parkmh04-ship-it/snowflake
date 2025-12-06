-- 기존 테이블 삭제
DROP TABLE IF EXISTS shortener_history;
DROP TABLE IF EXISTS worker_node;
DROP TABLE IF EXISTS snowflake_workers;
DROP TABLE IF EXISTS failed_events;

-- shortener_history 테이블 생성
CREATE TABLE shortener_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(255) NOT NULL UNIQUE,
    long_url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_long_url (long_url)
);

-- snowflake_workers 테이블 생성
CREATE TABLE IF NOT EXISTS snowflake_workers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_num BIGINT NOT NULL UNIQUE,
    worker_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
);

-- failed_events 테이블 생성
CREATE TABLE failed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(255) NOT NULL,
    long_url TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    failed_at BIGINT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    INDEX idx_status (status),
    INDEX idx_failed_at (failed_at),
    INDEX idx_status_retry_count (status, retry_count)
);

-- 초기 데이터 삽입
INSERT INTO snowflake_workers (worker_num, worker_name, status)
WITH RECURSIVE nums(n) AS (
 SELECT 0
 UNION ALL
  SELECT n + 1 FROM nums WHERE n < 255
)
SELECT n, 'NONE', 'IDLE' FROM nums;
