\set ON_ERROR_STOP on

SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname IN ('stackoverflow_base', 'stackoverflow_lab')
  AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS stackoverflow_lab;

ALTER DATABASE stackoverflow_base WITH IS_TEMPLATE false ALLOW_CONNECTIONS true;

DROP DATABASE IF EXISTS stackoverflow_base;
CREATE DATABASE stackoverflow_base;

