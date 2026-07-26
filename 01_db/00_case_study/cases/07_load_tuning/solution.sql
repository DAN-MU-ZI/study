-- PowerShell에서 기준 부하를 실행한다.
-- .\scripts\run-load.ps1 -Tag java -VirtualUsers 20 -Duration 30s

CREATE INDEX IF NOT EXISTS idx_posts_tags_trgm
    ON posts USING gin (tags gin_trgm_ops);
ANALYZE posts;

-- 같은 PowerShell 명령을 다시 실행한 뒤 아래 지표를 비교한다.
SELECT calls,
       round(total_exec_time::numeric, 2) AS total_exec_ms,
       round(mean_exec_time::numeric, 2) AS mean_exec_ms,
       shared_blks_hit,
       shared_blks_read,
       left(regexp_replace(query, '\s+', ' ', 'g'), 200) AS query
FROM pg_stat_statements
WHERE query LIKE '%FROM posts%'
ORDER BY total_exec_time DESC;

