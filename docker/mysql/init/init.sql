-- 초기 데이터 삽입
-- Worker ID 0~255 할당 (Recursive CTE 사용)
INSERT IGNORE INTO snowflake_workers (worker_num, worker_name, status)
WITH RECURSIVE nums(n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 255
)
SELECT n, 'NONE', 'IDLE' FROM nums;