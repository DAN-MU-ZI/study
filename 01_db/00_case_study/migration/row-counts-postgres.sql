\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
\pset fieldsep ','

SELECT format(
    'SELECT %L, count(*) FROM public.%I;',
    lower(tablename),
    tablename
)
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename
\gexec

