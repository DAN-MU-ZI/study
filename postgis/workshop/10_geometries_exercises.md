# 10. 도형 연습 (Geometry Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geometries_exercises.html>](https://postgis.net/workshops/postgis-intro/geometries_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지금까지 본 모든 기능을 상기시켜 보겠습니다. 운동에 유용할 것 같아요!

- 레코드 집합에 대한 합계를 반환하는 `sum(expression)` 집계
- 레코드 집합의 크기를 반환하는 `count(expression)` 집계
- `ST_GeometryType(geometry)`는 형상 유형을 반환합니다.
- `ST_NDims(geometry)`는 형상의 차원 수를 반환합니다.
- `ST_SRID(geometry)`는 기하학의 공간 참조 식별자 번호를 반환합니다.
- `ST_X(point)`는 X 좌표를 반환합니다.
- `ST_Y(point)`는 Y 세로좌표를 반환합니다.
- `ST_Length(linestring)`는 선스트링의 길이를 반환합니다.
- `ST_StartPoint(geometry)`는 첫 번째 좌표를 점으로 반환합니다.
- `ST_EndPoint(geometry)`는 마지막 좌표를 점으로 반환합니다.
- `ST_NPoints(geometry)`는 라인스트링의 좌표 수를 반환합니다.
- `ST_Area(geometry)`는 다각형의 면적을 반환합니다.
- `ST_NRings(geometry)`는 링 수를 반환합니다(보통 1개, 구멍이 있는 경우 그 이상).
- `ST_ExteriorRing(polygon)`는 외부 링을 선스트링으로 반환합니다.
- `ST_InteriorRingN(polygon, integer)`는 지정된 내부 링을 선스트링으로 반환합니다.
- `ST_Perimeter(geometry)`는 모든 링의 길이를 반환합니다.
- `ST_NumGeometries(multi/geomcollection)`는 컬렉션의 부품 수를 반환합니다.
- `ST_GeometryN(geometry, integer)`는 컬렉션의 지정된 부분을 반환합니다.
- `ST_GeomFromText(text)`는 `geometry`를 반환합니다.
- `ST_AsText(geometry)`는 WKT `text`를 반환합니다.
- `ST_AsEWKT(geometry)`는 EWKT `text`를 반환합니다.
- `ST_GeomFromWKB(bytea)`는 `geometry`를 반환합니다.
- `ST_AsBinary(geometry)`는 WKB `bytea`를 반환합니다.
- `ST_AsEWKB(geometry)`는 EWKB `bytea`를 반환합니다.
- `ST_GeomFromGML(text)`는 `geometry`를 반환합니다.
- `ST_AsGML(geometry)`는 GML `text`를 반환합니다.
- `ST_GeomFromKML(text)`는 `geometry`를 반환합니다.
- `ST_AsKML(geometry)`는 KML `text`를 반환합니다.
- `ST_AsGeoJSON(geometry)`는 JSON `text`를 반환합니다.
- `ST_AsSVG(geometry)`는 SVG `text`를 반환합니다.

또한 사용 가능한 테이블을 기억하십시오.

- `nyc_census_blocks`
  - blkid, popn_total, 보로나메, geom
- `nyc_streets`
  - 이름, 유형, 지리
- `nyc_subway_stations`
  - 이름, 기하학
- `nyc_neighborhoods`
  - 이름, 보로나메, 검

## 연습

- **'웨스트빌리지' 동네는 어떤 지역인가요?**

  ```sql
  SELECT ST_Area(geom)
    FROM nyc_neighborhoods
    WHERE name = 'West Village';
  ```

      1044614.5296486

  > [!NOTE]
  > 면적은 평방미터로 표시됩니다. 헥타르 단위의 면적을 구하려면 10000으로 나누세요. 에이커 단위의 면적을 구하려면 4047로 나눕니다.

- **'Pelham St'의 기하학적 유형은 무엇입니까? 길이?**

  ```sql
  SELECT
     ST_GeometryType(geom),
     ST_Length(geom)
    FROM nyc_streets
    WHERE name = 'Pelham St';
  ```

      ST_MultiLineString
      50.323

- **'Broad St' 지하철역의 GeoJSON 표현은 무엇입니까?**

  ```sql
  SELECT
   ST_AsGeoJSON(geom)
  FROM nyc_subway_stations
  WHERE name = 'Broad St';
  ```

      {"type":"Point",
       "crs":{"type":"name","properties":{"name":"EPSG:26918"}},
       "coordinates":[583571.905921312,4506714.341192182]}

- **뉴욕시의 총 거리 길이(킬로미터)는 얼마입니까?** (힌트: 공간 데이터의 측정 단위는 미터이며, 1킬로미터는 1000미터입니다.)

  ```sql
  SELECT Sum(ST_Length(geom)) / 1000
    FROM nyc_streets;
  ```

      10418.9047172

- **맨해튼의 면적은 에이커 단위로 얼마입니까?** (힌트: `nyc_census_blocks`와 `nyc_neighborhoods` 모두 `boroname`를 포함합니다.)

  ```sql
  SELECT Sum(ST_Area(geom)) / 4047
    FROM nyc_neighborhoods
    WHERE boroname = 'Manhattan';
  ```

      13965.3201224118

  아니면...

  ```sql
  SELECT Sum(ST_Area(geom)) / 4047
    FROM nyc_census_blocks
    WHERE boroname = 'Manhattan';
  ```

      14601.3987215548

- **가장 서쪽에 있는 지하철역은 무엇인가요?**

  ```sql
  SELECT ST_X(geom), name
    FROM nyc_subway_stations
    ORDER BY ST_X(geom)
    LIMIT 1;
  ```

      Tottenville

- **'Columbus Cir'(일명 Columbus Circle)의 기간은 얼마나 되나요?**

  ```sql
  SELECT ST_Length(geom)
    FROM nyc_streets
    WHERE name = 'Columbus Cir';
  ```

      308.34199

- **뉴욕시의 거리 길이를 유형별로 요약하면 얼마나 되나요?**

  ```sql
  SELECT type, Sum(ST_Length(geom)) AS length
  FROM nyc_streets
  GROUP BY type
  ORDER BY length DESC;
  ```

      type                       |      length
      --------------------------------------------------+------------------
      residential                                      | 8629870.33786606
      motorway                                         | 403622.478126363
      tertiary                                         | 360394.879051303
      motorway_link                                    | 294261.419479668
      secondary                                        | 276264.303897926
      unclassified                                     | 166936.371604458
      primary                                          | 135034.233017947
      footway                                          | 71798.4878378096
      service                                          |  28337.635038596
      trunk                                            | 20353.5819826076
      cycleway                                         | 8863.75144825929
      pedestrian                                       | 4867.05032825026
      construction                                     | 4803.08162103562
      residential; motorway_link                       | 3661.57506293745
      trunk_link                                       | 3202.18981240201
      primary_link                                     | 2492.57457083536
      living_street                                    | 1894.63905457332
      primary; residential; motorway_link; residential | 1367.76576941335
      undefined                                        |  380.53861910346
      steps                                            | 282.745221342127
      motorway_link; residential                       |  215.07778911517

  > [!NOTE]
  > `ORDER BY length DESC` 절은 결과를 길이별로 내림차순으로 정렬합니다. 결과적으로 가장 널리 사용되는 유형이 목록의 첫 번째 항목이 됩니다.

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

```sql
SELECT ST_Area(geom) AS area_sq_meters
FROM nyc_neighborhoods
WHERE name = 'Upper West Side';
```

**결과**: 약 `4842188` ㎡ (약 4.84 ㎢)

</details>

---

[← 이전](09_geometries.md) · [목차](00_index.md) · [다음 →](11_spatial_relationships.md)