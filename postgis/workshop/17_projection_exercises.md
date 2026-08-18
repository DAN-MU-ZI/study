# 17. 투영 연습 (Projection Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/projection_exercises.html>](https://postgis.net/workshops/postgis-intro/projection_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

다음은 우리가 본 기능 중 일부를 상기시켜주는 것입니다. 힌트: 연습에 유용할 것입니다!

- 레코드 집합에 대한 합계를 반환하는 `sum(expression)` 집계
- `ST_Length(linestring)`는 선스트링의 길이를 반환합니다.
- `ST_SRID(geometry)`는 형상의 SRID를 반환합니다.
- `ST_Transform(geometry, srid)`는 기하학을 다른 공간 참조 시스템으로 변환합니다.
- `ST_GeomFromText(text)`는 `geometry`를 반환합니다.
- `ST_AsText(geometry)`는 WKT `text`를 반환합니다.
- `ST_AsGML(geometry)`는 GML `text`를 반환합니다.

귀하가 이용할 수 있는 온라인 리소스를 기억하십시오.

- <https://epsg.io/>

또한 사용 가능한 테이블을 기억하십시오.

- `nyc_census_blocks`
  - 이름, popn_total, 보로나메, 검
- `nyc_streets`
  - 이름, 유형, 지리
- `nyc_subway_stations`
  - 이름, 기하학
- `nyc_neighborhoods`
  - 이름, 보로나메, 검

## 연습

- **UTM 18로 측정한 뉴욕의 모든 거리 길이는 얼마입니까?**

  ```sql
  SELECT Sum(ST_Length(geom))
    FROM nyc_streets;
  ```

      10418904.7172

- **SRID 2831의 WKT 정의는 무엇입니까?**

  ```sql
  SELECT srtext FROM spatial_ref_sys
  WHERE SRID = 2831;
  ```

  또는 <https://epsg.io/2831>를 통해

      PROJCS["NAD83(HARN) / New York Long Island",
        GEOGCS["NAD83(HARN)",
          DATUM["NAD83 (High Accuracy Regional Network)",
            SPHEROID["GRS 1980", 6378137.0, 298.257222101,
              AUTHORITY["EPSG","7019"]],
            TOWGS84[-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0],
            AUTHORITY["EPSG","6152"]],
          PRIMEM["Greenwich", 0.0,
            AUTHORITY["EPSG","8901"]],
          UNIT["degree", 0.017453292519943295],
          AXIS["Geodetic longitude", EAST],
          AXIS["Geodetic latitude", NORTH],
          AUTHORITY["EPSG","4152"]],
        PROJECTION["Lambert Conic Conformal (2SP)",
          AUTHORITY["EPSG","9802"]],
        PARAMETER["central_meridian", -74.0],
        PARAMETER["latitude_of_origin", 40.166666666666664],
        PARAMETER["standard_parallel_1", 41.03333333333333],
        PARAMETER["false_easting", 300000.0],
        PARAMETER["false_northing", 0.0],
        PARAMETER["scale_factor", 1.0],
        PARAMETER["standard_parallel_2", 40.666666666666664],
        UNIT["m", 1.0],
        AXIS["Easting", EAST],
        AXIS["Northing", NORTH],
        AUTHORITY["EPSG","2831"]]

- **SRID 2831로 측정한 뉴욕의 모든 거리 길이는 얼마입니까?**

  ```sql
  SELECT Sum(ST_Length(ST_Transform(geom,2831)))
    FROM nyc_streets;
  ```

      10421993.706374

  > [!NOTE]
  > UTM 18과 State Plane Long Island 측정 간의 차이는 (10421993 - 10418904)/10418904, 즉 0.02%입니다. `geography`를 사용하여 회전타원체에서 계산하면 총 거리 길이는 10421999이며 이는 State Plane 값에 더 가깝습니다. State Plane Long Island 투영은 매우 작은 지역(뉴욕시)에 대해 정확하게 보정된 반면 UTM 18은 넓은 지역에 대해 합리적인 결과를 제공해야 하기 때문에 이는 놀라운 일이 아닙니다.

- **74도 자오선을 통과하는 도로는 몇 개입니까?**

  ```sql
  SELECT Count(*)
  FROM nyc_streets
  WHERE ST_Intersects(
    ST_Transform(geom, 4326),
    'SRID=4326;LINESTRING(-74 20, -74 60)'
    );
  ```

      223

  "74번째 자오선"은 "X 값이 -74인 지리상의 수직선"을 표현하는 멋진 방법입니다. 우리는 그러한 선을 구성한 다음 이를 지리학적으로 투영된 거리와 비교할 수 있습니다. 라인을 UTM에 투영하고 비교하면 약간 다른 대답이 반환됩니다. 동일한 답을 얻으려면 변환하기 전에 더 많은 포인트를 갖도록 "세분화"해야 합니다.

  ```sql
  SELECT Count(*)
  FROM nyc_streets
  WHERE ST_Intersects(
    geom,
    ST_Transform(ST_Segmentize('SRID=4326;LINESTRING(-74 20, -74 60)'::geometry,0.001), 26918)
    );
  ```


---

[← 이전](16_projection.md) · [목차](00_index.md) · [다음 →](18_geography.md)