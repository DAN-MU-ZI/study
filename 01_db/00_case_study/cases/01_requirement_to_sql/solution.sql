SELECT id,
       title,
       score,
       creationdate,
       owneruserid
FROM posts
WHERE posttypeid = 1
  AND creationdate >= TIMESTAMP '2010-01-01 00:00:00'
  AND creationdate <  TIMESTAMP '2011-01-01 00:00:00'
  AND score >= 5
  AND tags LIKE '%<postgresql>%'
ORDER BY score DESC, creationdate DESC, id DESC
LIMIT 20;

