EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT id, title, score
FROM posts
WHERE tags LIKE '%<postgresql>%'
ORDER BY score DESC, id DESC
LIMIT 20;

CREATE INDEX idx_posts_tags_trgm
    ON posts USING gin (tags gin_trgm_ops);

ANALYZE posts;

EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT id, title, score
FROM posts
WHERE tags LIKE '%<postgresql>%'
ORDER BY score DESC, id DESC
LIMIT 20;

SELECT pg_size_pretty(pg_relation_size('idx_posts_tags_trgm')) AS index_size;

