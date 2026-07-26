\set ON_ERROR_STOP on

SELECT current_database() AS database_name,
       pg_size_pretty(pg_database_size(current_database())) AS database_size;

SELECT class_info.relname AS table_name,
       class_info.reltuples::bigint AS estimated_rows
FROM pg_class AS class_info
JOIN pg_namespace AS namespace_info
  ON namespace_info.oid = class_info.relnamespace
WHERE namespace_info.nspname = 'public'
  AND class_info.relkind = 'r'
ORDER BY class_info.relname;

DO $$
DECLARE
    required_table text;
BEGIN
    FOREACH required_table IN ARRAY ARRAY['posts', 'users', 'comments', 'votes']
    LOOP
        IF to_regclass('public.' || required_table) IS NULL THEN
            RAISE EXCEPTION '필수 테이블이 없습니다: %', required_table;
        END IF;
    END LOOP;
END
$$;

SELECT count(*) > 0 AS has_posts FROM posts;
SELECT count(*) > 0 AS has_users FROM users;
