# 29. 최근접 이웃 탐색 (Nearest-Neighbour Searching / KNN)

**KNN (K-Nearest Neighbors)**은 기준 위치에서 가장 가까운 상위 $K$개의 객체를 찾는 기법입니다.

기존의 `ORDER BY ST_Distance(geom, point) LIMIT K` 방식은 전체 테이블의 거리를 일일이 계산(전체 스캔)해야 하므로 데이터가 많을 때 매우 느립니다.

PostGIS는 **GiST 인덱스 거리 연산자 (`<->`)**를 제공하여 인덱스 트리를 직접 탐색하는 초고속 인덱스 기반 KNN 검색을 지원합니다.

![KNN 검색 원리](screenshots/knn2.jpg)

---

## 1. GiST KNN 연산자 (`<->`)

`<->` 연산자는 바운딩 박스 간 거리를 인덱스 수준에서 계산하여 가장 가까운 행부터 정렬합니다.

```sql
-- 특정 지점(X: 583571, Y: 4509376)에서 가장 가까운 지하철역 3곳 초고속 검색
SELECT
  name,
  routes
FROM nyc_subway_stations
ORDER BY geom <-> ST_SetSRID(ST_MakePoint(583571, 4509376), 26918)
LIMIT 3;
```

---

## 2. 성능 비교
- 일반 `ORDER BY ST_Distance(...) LIMIT 3`: 테이블 100만 건 기준 수 초 소요 (Full Scan)
- GiST KNN `ORDER BY geom <-> point LIMIT 3`: 100만 건 중에서도 **1ms 미만**으로 즉시 완료 (Index Scan)

---

| [⬅️ 28. 3차원 데이터 (3-D)](28_3d.md) | [🏠 워크숍 목차](README.md) | [30. 래스터 (Rasters) ➡️](30_rasters.md) |
| :--- | :---: | ---: |
