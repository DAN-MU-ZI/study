# 05. 복합 B-tree 인덱스

특정 사용자가 기간 내 작성한 글을 최신순으로 50개 조회하는 쿼리를 튜닝한다.

```sql
SELECT id, posttypeid, score, creationdate
FROM posts
WHERE owneruserid = 22656
  AND creationdate >= TIMESTAMP '2009-01-01'
  AND creationdate <  TIMESTAMP '2010-01-01'
ORDER BY creationdate DESC, id DESC
LIMIT 50;
```

조건 열, 범위 열, 정렬, 반환 열을 구분해 인덱스를 설계한다. 인덱스 크기와 쓰기 비용도 기록한다.

