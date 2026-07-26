EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT id, posttypeid, score, creationdate
FROM posts
WHERE owneruserid = 22656
  AND creationdate >= TIMESTAMP '2009-01-01'
  AND creationdate <  TIMESTAMP '2010-01-01'
ORDER BY creationdate DESC, id DESC
LIMIT 50;

CREATE INDEX idx_posts_owner_created_id
    ON posts (owneruserid, creationdate DESC, id DESC)
    INCLUDE (posttypeid, score);

ANALYZE posts;

EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT id, posttypeid, score, creationdate
FROM posts
WHERE owneruserid = 22656
  AND creationdate >= TIMESTAMP '2009-01-01'
  AND creationdate <  TIMESTAMP '2010-01-01'
ORDER BY creationdate DESC, id DESC
LIMIT 50;

SELECT pg_size_pretty(pg_relation_size('idx_posts_owner_created_id')) AS index_size;

