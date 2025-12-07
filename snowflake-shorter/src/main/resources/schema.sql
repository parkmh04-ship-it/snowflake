-- 기존 shorter_history 테이블 삭제 (개발용)
DROP TABLE IF EXISTS shorter_history;

-- shorter_history 테이블 생성 (인덱스 포함)
CREATE TABLE shorter_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(255) COLLATE utf8mb4_bin NOT NULL UNIQUE,
    long_url VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_long_url (long_url)
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
);

-- Dead Letter Queue를 위한 failed_events 테이블 생성
DROP TABLE IF EXISTS failed_events;

CREATE TABLE failed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(255) NOT NULL,
    long_url VARCHAR(4000) NOT NULL,
    created_at BIGINT NOT NULL,
    failed_at BIGINT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    INDEX idx_status (status),
    INDEX idx_failed_at (failed_at),
    INDEX idx_status_retry_count (status, retry_count)
);