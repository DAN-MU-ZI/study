# 27. 인덱스 기반 클러스터링 (Clustering on Indices)

데이터베이스 테이블의 물리적 디스크 저장 순서가 공간 인덱스(GiST) 순서와 일치하도록 디스크 블록을 재배열하는 기법을 **공간 클러스터링(Spatial Clustering)**이라고 합니다.

---

## 1. 클러스터링의 장점

- 지리적으로 가까운 행들이 동일한 디스크 블록(디스크 페이지)에 연속으로 저장됩니다.
- 범위 검색(BBOX, ST_DWithin) 시 디스크 I/O(I/O Page Reads)가 획기적으로 감소하여 쿼리 성능이 대폭 향상됩니다.

---

## 2. 클러스터링 실행 문법

```sql
-- 1. GiST 인덱스 기준으로 테이블 물리 재정렬
CLUSTER nyc_census_blocks USING nyc_census_blocks_geom_idx;

-- 2. 통계 갱신
ANALYZE nyc_census_blocks;
```

> [!NOTE]
> `CLUSTER` 명령은 실행 중 테이블에 배타적 잠금(Exclusive Lock)을 생성하므로 트래픽이 적은 유지보수 시간에 실행하는 것이 권장됩니다.

---

| [⬅️ 26. 차원 확장 9-교차 모델 (DE-9IM)](26_de9im.md) | [🏠 워크숍 목차](README.md) | [28. 3차원 데이터 (3-D) ➡️](28_3d.md) |
| :--- | :---: | ---: |
