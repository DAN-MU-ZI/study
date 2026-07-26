\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'dbo'
    LOOP
        EXECUTE format('ALTER TABLE dbo.%I SET SCHEMA public', item.tablename);
    END LOOP;

    FOR item IN
        SELECT sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'dbo'
    LOOP
        EXECUTE format('ALTER SEQUENCE dbo.%I SET SCHEMA public', item.sequence_name);
    END LOOP;

    FOR item IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename <> lower(tablename)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I RENAME TO %I',
            item.tablename,
            lower(item.tablename)
        );
    END LOOP;

    FOR item IN
        SELECT sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
          AND sequence_name <> lower(sequence_name)
    LOOP
        EXECUTE format(
            'ALTER SEQUENCE public.%I RENAME TO %I',
            item.sequence_name,
            lower(item.sequence_name)
        );
    END LOOP;

    FOR item IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name <> lower(column_name)
        ORDER BY table_name, ordinal_position
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I RENAME COLUMN %I TO %I',
            item.table_name,
            item.column_name,
            lower(item.column_name)
        );
    END LOOP;
END
$$;

ANALYZE;
