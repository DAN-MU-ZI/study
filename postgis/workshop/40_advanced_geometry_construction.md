# 40. 고급 지오메트리 생성 (Advanced Geometry Constructions)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/advanced_geometry_construction.html>](https://postgis.net/workshops/postgis-intro/advanced_geometry_construction.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

`nyc_subway_stations` 레이어는 점(Point) 데이터로만 이루어져 있어 지하철 노선의 실제 선형 경로를 한눈에 파악하기 어렵습니다.

![이미지](advanced_geometry_construction/adv_geom0.jpg)

이번 장에서는 PostgreSQL의 **재귀적 공통 테이블 표현식(Recursive CTE)**, **배열 함수(Array Functions)**, 그리고 PostGIS의 **`ST_MakeLine`**을 결합하여, **순서가 없는 역 포인트들로부터 실제 지하철 노선 선형(LineString)을 자동으로 생성**하는 고급 알고리즘을 구현해 보겠습니다.

---

## 1. 해결해야 할 과제

1. 하나의 지하철역에 여러 노선이 동시에 정차합니다 (예: `routes = 'N,Q,R'`).
2. 테이블에는 역들이 노선을 따라 운행하는 순서 정보가 전혀 없습니다.

따라서 알고리즘은 **노선의 시작 종착역을 스스로 찾고**, **현재 역에서 가장 가까운 미방문 인접 역을 순차적으로 탐색하여 선으로 연결**해야 합니다.

![이미지](advanced_geometry_construction/adv_geom1.jpg)

---

## 2. 단일 노선(Q 라인) 경로 생성 실습 (Recursive CTE)

최북단 종착역(`gid = 304`)에서 출발하여 Q 라인의 선형을 만드는 재귀 쿼리입니다.

```sql
WITH RECURSIVE next_stop(geom, idlist) AS (
  -- 1. 초기 앵커 쿼리: 시작 종착역 선정 및 방문 배열 초기화
  (SELECT
    geom,
    ARRAY[gid] AS idlist
  FROM nyc_subway_stations
  WHERE gid = 304)

  UNION ALL

  -- 2. 재귀 반복 쿼리: 미방문 역 중 가장 가까운 다음 역 선택
  (SELECT
    s.geom,
    array_append(n.idlist, s.gid) AS idlist
  FROM nyc_subway_stations AS s,
       next_stop AS n
  WHERE strpos(s.routes, 'Q') != 0
    AND NOT n.idlist @> ARRAY[s.gid]
  ORDER BY ST_Distance(n.geom, s.geom) ASC
  LIMIT 1)
)
-- 정렬된 역 점들을 연결하여 단일 LineString 생성
SELECT ST_MakeLine(geom) AS geom
FROM next_stop;
```

![이미지](advanced_geometry_construction/adv_geom3.jpg)

---

## 3. 전체 24개 지하철 노선 자동화 알고리즘

### 단계 1: 노선별 시작 종착역 자동 판별 (중심점에서 가장 먼 역)
각 노선에 속한 모든 역의 무게중심(`ST_Centroid(ST_Collect(geom))`)을 계산하고, 중심점으로부터 가장 멀리 떨어진 역을 시작 종착역으로 자동 선정합니다.

![이미지](advanced_geometry_construction/adv_geom4.jpg)

### 단계 2: 노선 경로 생성 함수 정의

```sql
CREATE OR REPLACE FUNCTION walk_subway(start_gid integer, route_name text)
RETURNS geometry AS
$$
WITH RECURSIVE next_stop(geom, idlist) AS (
  (SELECT
    geom,
    ARRAY[gid] AS idlist
  FROM nyc_subway_stations
  WHERE gid = start_gid)

  UNION ALL

  (SELECT
    s.geom,
    array_append(n.idlist, s.gid) AS idlist
  FROM nyc_subway_stations AS s,
       next_stop AS n
  WHERE strpos(s.routes, route_name) != 0
    AND NOT n.idlist @> ARRAY[s.gid]
  ORDER BY ST_Distance(n.geom, s.geom) ASC
  LIMIT 1)
)
SELECT ST_MakeLine(geom) AS geom
FROM next_stop;
$$ LANGUAGE sql;
```

### 단계 3: nyc_subway_lines 테이블 일괄 생성

```sql
CREATE TABLE nyc_subway_lines AS
WITH routes AS (
  -- 1. 고유한 노선 목록 추출 (1, 2, 3, A, B, C, Q...)
  SELECT DISTINCT unnest(string_to_array(routes, ',')) AS route
  FROM nyc_subway_stations
  ORDER BY route
),
stops AS (
  -- 2. 노선별 정차역 매핑
  SELECT s.gid, s.geom, r.route
  FROM routes AS r
  JOIN nyc_subway_stations AS s
    ON strpos(s.routes, r.route) <> 0
),
centers AS (
  -- 3. 노선별 역 군집의 중심점 계산
  SELECT route, ST_Centroid(ST_Collect(geom)) AS geom
  FROM stops
  GROUP BY route
),
stops_distance AS (
  -- 4. 중심점으로부터의 거리 계산 및 내림차순 정렬
  SELECT s.*, ST_Distance(s.geom, c.geom) AS distance
  FROM stops AS s
  JOIN centers AS c
    ON s.route = c.route
  ORDER BY route, distance DESC
),
first_stops AS (
  -- 5. 노선별 가장 먼 시작 종착역 선별
  SELECT DISTINCT ON (route) stops_distance.*
  FROM stops_distance
)
-- 6. 노선별 선형 지오메트리 생성
SELECT
  ascii(route) AS gid,
  route,
  walk_subway(gid, route) AS geom
FROM first_stops;

ALTER TABLE nyc_subway_lines ADD PRIMARY KEY (gid);
```

QGIS에서 완성된 `nyc_subway_lines` 테이블을 불러오면 뉴욕시 지하철 전체 노선망이 아름다운 선형 네트워크로 완벽하게 렌더링됩니다.

![이미지](advanced_geometry_construction/adv_geom5.jpg)


---

[← 이전](39_upgrades.md) · [목차](00_index.md) · [다음 →](41_postgis_functions.md)
