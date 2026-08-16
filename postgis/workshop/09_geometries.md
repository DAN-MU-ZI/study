# 9. 지오메트리 (Geometries)

PostGIS의 핵심은 기하학적 형태(점, 선, 면)를 데이터베이스 컬럼에 저장하고 연산하는 것입니다.

---

## 1. 공간 데이터의 텍스트 표현 (WKT)

OGC는 지오메트리를 사람이 읽을 수 있는 텍스트로 표현하는 **WKT (Well-Known Text)** 표준을 정의했습니다.

- **POINT**: `POINT(0 0)`
- **LINESTRING**: `LINESTRING(0 0, 1 1, 2 1, 2 2)`
- **POLYGON**: `POLYGON((0 0, 0 4, 4 4, 4 0, 0 0), (1 1, 1 2, 2 2, 2 1, 1 1))` *(구멍이 있는 도넛 형태 포함 가능)*
- **MULTIPOINT**: `MULTIPOINT((0 0), (1 2))`
- **MULTILINESTRING**: `MULTILINESTRING((0 0, 1 1), (2 2, 3 3))`
- **MULTIPOLYGON**: `MULTIPOLYGON(((0 0, 4 0, 4 4, 0 4, 0 0)), ((10 10, 14 10, 14 14, 10 14, 10 10)))`
- **GEOMETRYCOLLECTION**: `GEOMETRYCOLLECTION(POINT(2 0), POLYGON((0 0, 1 0, 1 1, 0 1, 0 0)))`

![WKT 지오메트리 표현](geometries/start01.png)

---

## 2. 지오메트리 생성 함수

WKT 문자열을 PostGIS의 내부 바이너리 지오메트리 객체로 변환할 때 `ST_GeomFromText` 함수를 사용합니다.

```sql
-- SRID(좌표계)가 없는 단순 포인트
SELECT ST_GeomFromText('POINT(2 5)');

-- SRID 4326(WGS84 위경도) 좌표를 갖는 포인트
SELECT ST_SetSRID(ST_MakePoint(-73.985130, 40.748817), 4326);

-- WKT와 SRID를 함께 지정
SELECT ST_GeomFromText('LINESTRING(0 0, 1 1, 2 4)', 26918);
```

---

## 3. 지오메트리 출력 함수

내부 지오메트리 바이너리를 읽기 쉬운 텍스트나 포맷으로 변환합니다:

- `ST_AsText(geom)`: WKT 문자열로 반환
- `ST_AsGeoJSON(geom)`: GeoJSON 포맷으로 반환
- `ST_AsKML(geom)`: Google Earth KML 포맷으로 반환
- `ST_AsSVG(geom)`: 웹용 SVG path 데이터로 반환

```sql
SELECT name, ST_AsText(geom)
FROM nyc_subway_stations
LIMIT 1;
```

결과:
```text
POINT(583571.495740449 4509376.62688086)
```

---

## 4. 지오메트리 속성 조회 함수

- `ST_GeometryType(geom)`: 지오메트리 타입 반환 (`ST_Point`, `ST_MultiPolygon` 등)
- `ST_NDims(geom)`: 차원 수 반환 (2차원, 3차원 등)
- `ST_SRID(geom)`: 좌표계 ID(SRID) 반환
- `ST_X(geom)`, `ST_Y(geom)`: 포인트의 X(경도/동서), Y(위도/남북) 좌표 추출
- `ST_Length(geom)`: 선의 길이 계산
- `ST_Area(geom)`: 폴리곤의 면적 계산
- `ST_NPoints(geom)`: 지오메트리를 구성하는 정점(Vertex)의 개수

```sql
-- 자치구별 면적 계산 (단위: 제곱미터 -> 제곱킬로미터 변환)
SELECT
  boroname,
  SUM(ST_Area(geom)) / 1000000.0 AS area_sq_km
FROM nyc_census_blocks
GROUP BY boroname
ORDER BY area_sq_km DESC;
```

---

| [⬅️ 8. 단순 SQL 실습 (Simple SQL Exercises)](08_simple_sql_exercises.md) | [🏠 워크숍 목차](README.md) | [10. 지오메트리 실습 (Geometry Exercises) ➡️](10_geometries_exercises.md) |
| :--- | :---: | ---: |
