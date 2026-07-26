\pset pager off
\timing on

SELECT now() AS captured_at,
       current_database() AS database_name,
       pg_size_pretty(pg_database_size(current_database())) AS database_size;

SELECT calls,
       round(total_exec_time::numeric, 2) AS total_exec_ms,
       round(mean_exec_time::numeric, 2) AS mean_exec_ms,
       rows,
       shared_blks_hit,
       shared_blks_read,
       temp_blks_read,
       temp_blks_written,
       left(regexp_replace(query, '\s+', ' ', 'g'), 240) AS query
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY total_exec_time DESC
LIMIT 20;

SELECT state,
       wait_event_type,
       wait_event,
       count(*) AS sessions
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state, wait_event_type, wait_event
ORDER BY sessions DESC;

SELECT relation::regclass AS relation,
       mode,
       granted,
       count(*) AS lock_count
FROM pg_locks
WHERE database = (SELECT oid FROM pg_database WHERE datname = current_database())
GROUP BY relation, mode, granted
ORDER BY lock_count DESC, relation;

SELECT relname AS table_name,
       seq_scan,
       seq_tup_read,
       idx_scan,
       n_live_tup
FROM pg_stat_user_tables
ORDER BY seq_tup_read DESC
LIMIT 20;

SELECT relname AS table_name,
       indexrelname AS index_name,
       idx_scan,
       idx_tup_read,
       idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC, relname, indexrelname
LIMIT 30;

