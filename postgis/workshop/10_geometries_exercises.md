# 10. 지오메트리 실습 (Geometry Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geometries_exercises.html>](https://postgis.net/workshops/postgis-intro/geometries_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

앞서 학습한 주요 지오메트리 함수와 집계 함수를 참고하여 다음 실습 문제를 직접 해결해 보세요.

### 실습 참조 함수 요약
- `sum(expression)`: 수치 집합의 총합계
- `count(expression)`: 레코드 개수
- `ST_GeometryType(geometry)`: 지오메트리 타입 반환
- `ST_NDims(geometry)`: 좌표 차원 수 반환
- `ST_SRID(geometry)`: SRID 번호 반환
- `ST_X(point)`: 포인트의 X 좌표 반환
- `ST_Y(point)`: 포인트의 Y 좌표 반환
- `ST_Length(linestring)`: 선의 길이 반환
- `ST_StartPoint(geometry)`: 선의 시작점 반환
- `ST_EndPoint(geometry)`: 선의 끝점 반환
- `ST_NPoints(geometry)`: 선의 정점 개수 반환
- `ST_Area(geometry)`: 폴리곤의 면적 반환
- `ST_NRings(geometry)`: 폴리곤의 링 개수 반환
- `ST_ExteriorRing(polygon)`: 외곽 경계 링 반환
- `ST_InteriorRingN(polygon, integer)`: n번째 내부 구멍 링 반환
- `ST_Perimeter(geometry)`: 폴리곤의 둘레 길이 반환
- `ST_NumGeometries(collection)`: 멀티/컬렉션 내 지오메트리 개수 반환
- `ST_GeometryN(geometry, integer)`: n번째 지오메트리 반환
- `ST_AsText(geometry)`: WKT 문자열로 반환
- `ST_AsEWKT(geometry)`: EWKT 문자열로 반환
- `ST_AsGeoJSON(geometry)`: GeoJSON 문자열로 반환
- `ST_AsSVG(geometry)`: SVG 패스 데이터로 반환

### 실습 대상 테이블
- `nyc_census_blocks`: `blkid`, `popn_total`, `boroname`, `geom`
- `nyc_streets`: `name`, `type`, `geom`
- `nyc_subway_stations`: `name`, `geom`
- `nyc_neighborhoods`: `name`, `boroname`, `geom`

---

## 연습 문제 및 정답

### 1. 'West Village' 근린지역의 면적은 얼마입니까?

```sql
SELECT ST_Area(geom)
FROM nyc_neighborhoods
WHERE name = 'West Village';
```

```text
1044614.5296486
```

> [!NOTE]
> 계산된 면적은 투영 단위인 제곱미터($\text{m}^2$)입니다. 헥타르($\text{ha}$) 단위는 $10,000$으로 나누고, 에이커($\text{acre}$) 단위는 약 $4,047$로 나누어 환산할 수 있습니다.

---

### 2. 'Pelham St' 도로의 지오메트리 타입과 길이는 각각 얼마입니까?

```sql
SELECT
  ST_GeometryType(geom),
  ST_Length(geom)
FROM nyc_streets
WHERE name = 'Pelham St';
```

```text
st_geometrytype   | st_length
--------------------+-----------
ST_MultiLineString |    50.323
```

---

### 3. 'Broad St' 지하철역의 GeoJSON 표현은 어떻게 출력됩니까?

```sql
SELECT ST_AsGeoJSON(geom)
FROM nyc_subway_stations
WHERE name = 'Broad St';
```

```json
{"type":"Point","crs":{"type":"name","properties":{"name":"EPSG:26918"}},"coordinates":[583571.905921312,4506714.341192182]}
```

---

### 4. 뉴욕시 전체 도로의 총 연장(길이, 킬로미터 단위)은 얼마입니까?

*(힌트: 투영 좌표계의 기본 단위는 미터이며, 1km = 1,000m입니다.)*

```sql
SELECT sum(ST_Length(geom)) / 1000 AS total_km
FROM nyc_streets;
```

```text
10418.9047172
```

---

### 5. 맨해튼(Manhattan)의 총 면적은 에이커(Acre) 단위로 얼마입니까?

*(힌트: `nyc_neighborhoods`와 `nyc_census_blocks` 두 테이블 모두 `boroname` 컬럼을 포함하고 있습니다.)*

**방법 A (`nyc_neighborhoods` 기준)**:

```sql
SELECT sum(ST_Area(geom)) / 4047 AS acres
FROM nyc_neighborhoods
WHERE boroname = 'Manhattan';
```

```text
13965.3201224118
```

**방법 B (`nyc_census_blocks` 기준)**:

```sql
SELECT sum(ST_Area(geom)) / 4047 AS acres
FROM nyc_census_blocks
WHERE boroname = 'Manhattan';
```

```text
14601.3987215548
```

> [!NOTE]
> 인구조사 블록과 근린지역 테이블 간의 약간의 면적 차이는 센서스 블록이 부두, 수역 인근 간척지 등 근린지역 경계에 포함되지 않은 일부 외곽 영역까지 커버하기 때문입니다.

---

### 6. 가장 서쪽에 위치한 지하철역의 이름과 X 좌표는 무엇입니까?

```sql
SELECT ST_X(geom), name
FROM nyc_subway_stations
ORDER BY ST_X(geom) ASC
LIMIT 1;
```

```text
      st_x       |    name
-----------------+------------
564998.636657805 | Tottenville
```

---

### 7. 'Columbus Cir'(콜럼버스 서클) 도로의 총 길이는 얼마입니까?

```sql
SELECT ST_Length(geom)
FROM nyc_streets
WHERE name = 'Columbus Cir';
```

```text
308.34199
```

---

### 8. 뉴욕시 도로망의 유형(type)별 총 길이를 요약하면 어떻게 됩니까?

```sql
SELECT type, sum(ST_Length(geom)) AS length
FROM nyc_streets
GROUP BY type
ORDER BY length DESC;
```

```text
                      type                       |      length
-------------------------------------------------+------------------
 residential                                     | 8629870.33786606
 motorway                                        | 403622.478126363
 tertiary                                        | 360394.879051303
 motorway_link                                   | 294261.419479668
 secondary                                       | 276264.303897926
 unclassified                                    | 166936.371604458
 primary                                         | 135034.233017947
 footway                                         | 71798.4878378096
 service                                         |  28337.635038596
 trunk                                           | 20353.5819826076
 cycleway                                        | 8863.75144825929
 pedestrian                                      | 4867.05032825026
 construction                                    | 4803.08162103562
 residential; motorway_link                      | 3661.57506293745
 trunk_link                                      | 3202.18981240201
 primary_link                                    | 2492.57457083536
 living_street                                   | 1894.63905457332
 primary; residential; motorway_link; residential| 1367.76576941335
 undefined                                       |  380.53861910346
 steps                                           | 282.745221342127
 motorway_link; residential                      |  215.07778911517
```


---

[← 이전](09_geometries.md) · [목차](00_index.md) · [다음 →](11_spatial_relationships.md)
