-- 세션 A
BEGIN;
SELECT id FROM users ORDER BY id LIMIT 1;
UPDATE users SET views = views WHERE id = 1; -- 위에서 얻은 ID로 교체

-- 세션 B
BEGIN;
SET LOCAL lock_timeout = '3s';
UPDATE users SET views = views WHERE id = 1; -- 같은 ID 사용

-- 세션 C
SELECT activity.pid,
       activity.state,
       activity.wait_event_type,
       activity.wait_event,
       pg_blocking_pids(activity.pid) AS blocking_pids,
       left(activity.query, 160) AS query
FROM pg_stat_activity AS activity
WHERE activity.datname = 'stackoverflow_lab'
ORDER BY activity.pid;

SELECT locks.pid,
       locks.locktype,
       locks.mode,
       locks.granted,
       locks.relation::regclass AS relation
FROM pg_locks AS locks
WHERE locks.database = (
    SELECT oid FROM pg_database WHERE datname = 'stackoverflow_lab'
)
ORDER BY locks.pid, locks.granted, locks.mode;

-- 세션 A와 B에서 정리
ROLLBACK;

