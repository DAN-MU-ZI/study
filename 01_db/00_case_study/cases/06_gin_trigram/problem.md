# 06. GIN과 trigram

API의 태그 조건은 다음 형태다.

```sql
WHERE tags LIKE '%<postgresql>%'
```

선행 와일드카드 때문에 일반 B-tree로는 검색 범위를 좁히기 어렵다. `pg_trgm`과 GIN을 적용한 뒤 실행 계획, 읽은 블록, 실행 시간, 인덱스 크기를 비교한다.

