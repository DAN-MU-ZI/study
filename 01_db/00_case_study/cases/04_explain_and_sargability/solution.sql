EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT count(*)
FROM posts
WHERE extract(year FROM creationdate) = 2010;

EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT count(*)
FROM posts
WHERE creationdate >= TIMESTAMP '2010-01-01 00:00:00'
  AND creationdate <  TIMESTAMP '2011-01-01 00:00:00';

-- 인덱스 효과까지 확인할 때만 생성한다.
CREATE INDEX idx_posts_creationdate
    ON posts (creationdate);

ANALYZE posts;

EXPLAIN (ANALYZE, BUFFERS, SETTINGS, TIMING OFF)
SELECT count(*)
FROM posts
WHERE creationdate >= TIMESTAMP '2010-01-01 00:00:00'
  AND creationdate <  TIMESTAMP '2011-01-01 00:00:00';

