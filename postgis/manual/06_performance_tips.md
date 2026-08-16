# 제 6 장. 성능 팁 및 튜닝 (Performance Tips)

대규모 엔터프라이즈 GIS 데이터베이스 성능 최적화 가이드입니다.

---

## 6.1. 핵심 파라미터 최적화 (`postgresql.conf`)

- `shared_buffers`: RAM의 25% (대용량 공간 페이지 버퍼링)
- `work_mem`: 복잡한 공간 집계 및 정렬 연산용 메모리 (32MB ~ 128MB)
- `maintenance_work_mem`: 공간 인덱스 GiST 빌드 속도 향상 (1GB ~ 2GB)
- `random_page_cost`: SSD 환경에서 `1.1 ~ 1.5`로 하향 설정 (공간 인덱스 활용 유도)

---

## 6.2. 공간 클러스터링 (`CLUSTER`)

테이블의 물리적 디스크 블록 순서를 GiST 공간 인덱스 순서와 일치하도록 재정렬하여 I/O 비용을 대폭 줄입니다:

```sql
CLUSTER airports USING airports_geom_idx;
ANALYZE airports;
```

---

| [⬅️ 제 5 장. 공간 쿼리 및 분석 (Spatial Queries)](05_spatial_queries.md) | [🏠 매뉴얼 목차](README.md) | [제 7 장. 벡터 함수 레퍼런스 (Vector Functions Reference) ➡️](07_reference_vector.md) |
| :--- | :---: | ---: |
