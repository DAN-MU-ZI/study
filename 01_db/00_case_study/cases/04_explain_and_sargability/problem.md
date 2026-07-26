# 04. 실행 계획과 SARGability

아래 쿼리의 실행 계획과 버퍼 사용량을 측정한다.

```sql
SELECT count(*)
FROM posts
WHERE extract(year FROM creationdate) = 2010;
```

동일한 결과를 유지하면서 `creationdate`의 일반 B-tree 인덱스를 사용할 수 있는 조건으로 바꾼다. 수정 전후의 `Execution Time`, `shared hit/read`, 스캔 노드와 추정·실제 행 수를 기록한다.

