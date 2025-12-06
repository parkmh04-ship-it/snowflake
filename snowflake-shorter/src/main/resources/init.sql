-- 초기 데이터 256개 삽입 (worker_num: 0 ~ 255)
-- H2 Database, MariaDB, MySQL 등에서 재귀 CTE를 지원하는 경우 사용 가능
INSERT INTO snowflake_workers (worker_num, worker_name, status)
WITH RECURSIVE nums(n) AS (
  SELECT 0
  UNION ALL
  SELECT n + 1 FROM nums WHERE n < 255
)
SELECT n, 'NONE', 'IDLE' FROM nums;