# 40. 고급 지오메트리 생성 (Advanced Geometry Constructions)

복잡한 공간 분석 문제를 해결하기 위한 고급 PostGIS 지오메트리 조작 기법들을 다룹니다.

![고급 지오메트리 생성에 사용할 점 집합](advanced_geometry_construction/adv_geom0.jpg)

---

## 1. 보로노이 다이어그램 (`ST_VoronoiPolygons`)
주어진 점들의 집합으로부터 각 점과의 거리가 가장 가까운 영역들로 공간을 분할하는 보로노이(Voronoi/Thiessen) 폴리곤을 생성합니다.

```sql
-- 지하철역 위치들을 기반으로 한 세권/영향권 보로노이 다이어그램 생성
SELECT ST_VoronoiPolygons(ST_Collect(geom)) AS voronoi_cells
FROM nyc_subway_stations;
```

---

## 2. 들로네 삼각분할 (`ST_DelaunayTriangles`)
점들의 집합을 삼각형 네트워크(TIN)로 분할합니다 (지형 수치표고모델 생성에 활용).

```sql
SELECT ST_DelaunayTriangles(ST_Collect(geom)) AS tin_triangles
FROM nyc_subway_stations;
```

---

## 3. 오목 껍질 / 볼록 껍질 (Concave & Convex Hull)
- `ST_ConvexHull(geom)`: 점들을 모두 감싸는 최소 볼록 다각형 (고무줄로 둘러싼 형태)
- `ST_ConcaveHull(geom, target_percent)`: 점들의 군집 형태를 세밀하게 따라가는 오목 다각형 생성 (클러스터 외곽선 추출)

```sql
-- 특정 지하철 노선 정차역들의 실제 서비스 영역 오목 외곽선 생성
SELECT ST_ConcaveHull(ST_Collect(geom), 0.8) AS service_area
FROM nyc_subway_stations
WHERE routes LIKE '%6%';
```

---

## 4. 지오메트리 단순화 (`ST_Simplify` & `ST_SimplifyPreserveTopology`)
웹 지도 표시 속도 향상을 위해 정점 수를 줄여 지오메트리를 가볍게 만듭니다 (Douglas-Peucker 알고리즘).

```sql
-- 토폴로지를 깨뜨리지 않고 10미터 허용치로 도로선 단순화
SELECT ST_SimplifyPreserveTopology(geom, 10.0) AS simplified_geom
FROM nyc_streets;
```

---

| [⬅️ 39. 소프트웨어 업그레이드 (Software Upgrades)](39_upgrades.md) | [🏠 워크숍 목차](README.md) | [41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions) ➡️](41_postgis_functions.md) |
| :--- | :---: | ---: |
