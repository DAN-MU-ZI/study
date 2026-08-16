# 제 13 장. 특수 함수 색인 (Special Functions Index)

공간 집계, 윈도우 함수, SQL-MM 표준 호환 함수 및 특수 지오메트리 타입 지원 함수 색인입니다.

---

- **공간 집계 함수 (Aggregate Functions)**: `ST_Collect`, `ST_Union`, `ST_Polygonize`, `ST_ClusterDBSCAN`, `ST_ClusterKMeans`
- **클러스터링 알고리즘**:
  - `ST_ClusterDBSCAN(geom, eps, minpoints)`: 밀도 기반 공간 군집화 (DBSCAN)
  - `ST_ClusterKMeans(geom, k)`: K-평균 공간 군집화
- **곡선 지오메트리 지원**: `CircularString`, `CompoundCurve`, `CurvePolygon`

---

| [⬅️ 제 12 장. 부가 기능 (PostGIS Extras)](12_extras.md) | [🏠 매뉴얼 목차](README.md) | [제 14 장. 문제 보고 및 기여 (Reporting Problems) ➡️](14_reporting_problems.md) |
| :--- | :---: | ---: |
