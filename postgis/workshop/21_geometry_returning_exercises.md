# 21. 도형 반환 연습 (Geometry Returning Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geometry_returning_exercises.html>](https://postgis.net/workshops/postgis-intro/geometry_returning_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

앞에서 살펴본 함수 가운데 이번 실습에 유용한 것들을 정리하면 다음과 같습니다.

- 레코드 집합에 대한 합계를 반환하는 `sum(expression)` 집계
- `ST_Area(geometry)`는 형상의 면적을 반환합니다.
- `ST_Centroid(geometry)`는 `geometry` 중심을 반환합니다.
- `ST_Transform(geometry, srid)`는 `geometries`를 다른 공간 참조 시스템으로 변환합니다.
- `ST_Buffer(geometry, radius)`는 확장된 `geometry` 모양을 반환합니다.
- `ST_Contains(geometry1, geometry2)`는 기하학1에 기하학2가 포함된 경우 true를 반환합니다.
- `ST_Union(geometry[])`는 그룹에 있는 모든 도형의 총합을 반환합니다.
- `ST_GeometryType(geometry)`는 형상 유형을 반환합니다.
- `ST_NumGeometries(geometry)`는 컬렉션의 도형 수를 반환하거나 단순 도형의 경우 1을 반환합니다.
- `ST_Intersection(geometry, geometry)`는 두 입력 형상이 공통으로 공유하는 영역을 반환합니다.

우리가 사용할 수 있는 테이블을 기억하세요:

- `nyc_census_blocks`
  - 이름, popn_total, 보로나메, 검
- `nyc_streets`
  - 이름, 유형, 지리
- `nyc_subway_stations`
  - 이름, 기하학
- `nyc_neighborhoods`
  - 이름, 보로나메, 검

## 연습

- **자체 중심을 포함하지 않는 인구 조사 블록은 몇 개입니까?**

  ```sql
  SELECT Count(*)
    FROM nyc_census_blocks
    WHERE NOT
      ST_Contains(
        geom,
        ST_Centroid(geom)
      );
  ```

      481

- **모든 인구 조사 블록을 단일 출력으로 통합합니다. 어떤 종류의 기하학입니까? 부품이 몇 개 있나요?**

  ```sql
  CREATE TABLE nyc_census_blocks_merge AS
    SELECT ST_Union(geom) AS geom
    FROM nyc_census_blocks;

  SELECT ST_GeometryType(geom)
    FROM nyc_census_blocks_merge;
  ```

      ST_MultiPolygon

  ```sql
  SELECT ST_NumGeometries(geom)
    FROM nyc_census_blocks_merge;
  ```

      63

- **원점 주위에 반지름 1인 버퍼를 만들면 면적은 얼마입니까? 예상값과 얼마나 다르며, 그 이유는 무엇입니까?**

  ```sql
  SELECT ST_Area(ST_Buffer('POINT(0 0)', 1));
  ```

      3.121445152258052

  > [!NOTE]
  > 단위원(반지름이 1인 원)의 면적은 pi, 3.1415926...이어야 합니다. 차이는 버퍼 가장자리의 선형 스트로크 때문입니다. 버퍼에는 유한한 수의 모서리가 있습니다. 버퍼의 가장자리 수를 늘리면 값이 pi에 가까워지지만 선형화로 인해 항상 작아집니다.

- **브루클린 지역인 '파크 슬로프'와 '캐롤 가든'이 전쟁을 벌입니다! 이웃 사이의 경계에 폭 100m의 DMZ를 나타내는 다각형을 만듭니다. DMZ의 면적은 무엇입니까?**

  ```sql
  CREATE TABLE brooklyn_dmz AS
    SELECT
      ST_Intersection(
        ST_Buffer(ps.geom, 50),
        ST_Buffer(cg.geom, 50))
      AS geom
    FROM
      nyc_neighborhoods ps,
      nyc_neighborhoods cg
    WHERE ps.name = 'Park Slope'
    AND cg.name = 'Carroll Gardens';

  SELECT ST_Area(geom) FROM brooklyn_dmz;
  ```

  > [!NOTE]
  > 두 관심 지역을 모두 버퍼링하는 것은 쉽지만 교차점을 얻으려면 테이블의 자체 조인이 필요합니다. 즉, "Park Slope" 레코드만으로 하나의 관계(`ps`)를 생성하고 "Carroll Gardens" 레코드로만 다른 관계(`cg`)를 생성해야 합니다. 아직 UTM 18(EPSG:26918)에서 작업 중이므로 교차로 영역은 평방미터 단위입니다.

      180990.964207547


---

[← 이전](20_geometry_returning.md) · [목차](00_index.md) · [다음 →](22_joins_advanced.md)
