# 16. 데이터 투영 (Projecting Data)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/projection.html>](https://postgis.net/workshops/postgis-intro/projection.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지구는 평평하지 않으므로 종이 지도나 컴퓨터 화면에 왜곡 없이 옮길 수 없습니다. 이 때문에 저마다 장단점이 있는 다양한 투영법이 만들어졌습니다. 어떤 투영은 면적을 보존하여 객체의 상대적 크기를 유지하고, 메르카토르 투영처럼 각도를 보존하는 투영도 있습니다. 여러 종류의 왜곡을 적절히 절충하는 투영도 있습니다. 모든 투영은 구형에 가까운 지구를 평면 데카르트 좌표계로 변환하며, 적합한 투영은 데이터의 용도에 따라 달라집니다.

NYC 데이터를 불러올 때 이미 투영을 접했습니다. 앞에서 혼동하기 쉬운 SRID 26918을 살펴본 것을 기억하세요. 때로는 서로 다른 공간 참조 체계 사이에서 데이터를 변환, 즉 재투영해야 합니다. PostGIS에서는 `ST_Transform(geometry, srid)`으로 데이터를 재투영하고, `ST_SRID(geometry)`와 `ST_SetSRID(geometry, srid)`로 지오메트리의 SRID를 관리합니다.

`ST_SRID` 함수를 사용하여 데이터의 SRID를 확인할 수 있습니다.

```sql
SELECT ST_SRID(geom) FROM nyc_streets LIMIT 1;
```

    26918

그렇다면 "26918"은 어떻게 정의되어 있을까요? [공간 데이터 불러오기](05_loading_data.md)에서 보았듯이 정의는 `spatial_ref_sys` 테이블에 들어 있습니다. 여기에는 **두 가지** 정의가 있습니다. WKT(Well-Known Text) 정의는 `srtext` 열에, "proj.4" 형식의 정의는 `proj4text` 열에 저장됩니다.

```sql
SELECT * FROM spatial_ref_sys WHERE srid = 26918;
```

PostGIS 재투영 엔진은 `spatial_ref_sys` 테이블에서 최상의 투영을 찾으려고 시도합니다.

- **auth_name / auth_srid**: PROJ가 내부 카탈로그에서 유효한 기관명과 기관 SRID를 찾으면 이를 이용해 투영 정의를 생성합니다.
- **srtext**: PROJ가 `srtext`의 정의를 해석할 수 있으면 이를 사용합니다.
- **proj4text**: 마지막으로 PROJ는 `proj4text`를 해석합니다.

이처럼 여러 정의 방식을 지원하므로 유효한 `srtext` 또는 `proj4text` 문자열만 있으면 PostGIS에 새 투영을 추가할 수 있습니다. 널리 쓰이는 기관명과 코드의 조합은 기본적으로 테이블에 등록되어 있습니다.

사용자 정의 투영을 생성할 때 선택 사항이 있는 경우 `srtext` 열을 작성하세요. 해당 열은 [GeoServer](http://geoserver.org), [QGIS](https://qgis.org), [FME](http://www.safe.com/) 등과 같은 외부 프로그램에서도 사용되기 때문입니다.

## 데이터 비교

좌표와 SRID를 종합하면 지구 상의 위치를 정의합니다. SRID가 없으면 좌표는 추상적인 개념일 뿐입니다. "데카르트" 좌표계는 지구 표면에 배치된 "평평한" 좌표계로 정의됩니다. PostGIS 기능은 이러한 평면에서 작동하기 때문에 비교 작업을 수행하려면 두 도형이 동일한 SRID에 표시되어야 합니다.

서로 다른 SRID를 사용하여 형상을 입력하면 오류가 발생합니다.

```sql
SELECT ST_Equals(
         ST_GeomFromText('POINT(0 0)', 4326),
         ST_GeomFromText('POINT(0 0)', 26918)
         );
```

    ERROR:  ST_Equals: Operation on mixed SRID geometries (Point, 4326) != (Point, 26918)

> [!NOTE]
> 즉각적인 변환을 위해 `ST_Transform`를 사용하는 것에 너무 만족하지 않도록 주의하십시오. 공간 인덱스는 저장된 도형의 SRID를 사용하여 구축됩니다. 다른 SRID에서 비교가 수행되면 공간 인덱스가 (종종) 사용되지 않습니다. 데이터베이스의 모든 테이블에 대해 **하나의 SRID**를 선택하는 것이 가장 좋습니다. 외부 애플리케이션에서 데이터를 읽거나 쓸 때만 변환 기능을 사용하십시오.

## 데이터 변환

SRID 26918에 대한 proj4 정의로 돌아가면 작업 투영이 구역 18의 UTM(Universal Transverse Mercator)이고 측정 단위가 미터임을 알 수 있습니다.

```sql
SELECT srtext FROM spatial_ref_sys WHERE srid = 26918;
```

    PROJCS["NAD83 / UTM zone 18N",
      GEOGCS["NAD83",
        DATUM["North_American_Datum_1983",
          SPHEROID["GRS 1980",6378137,298.257222101,AUTHORITY["EPSG","7019"]],
          TOWGS84[0,0,0,0,0,0,0],
          AUTHORITY["EPSG","6269"]],
        PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
        UNIT["degree",0.0174532925199433,AUTHORITY["EPSG","9122"]],
        AUTHORITY["EPSG","4269"]],
      PROJECTION["Transverse_Mercator"],
      PARAMETER["latitude_of_origin",0],
      PARAMETER["central_meridian",-75],
      PARAMETER["scale_factor",0.9996],
      PARAMETER["false_easting",500000],
      PARAMETER["false_northing",0],
      UNIT["metre",1,AUTHORITY["EPSG","9001"]],
      AXIS["Easting",EAST],AXIS["Northing",NORTH],
      AUTHORITY["EPSG","26918"]]

작업 투영의 일부 데이터를 "경도/위도"라고도 알려진 지리적 좌표로 변환해 보겠습니다.

하나의 SRID에서 다른 SRID로 데이터를 변환하려면 먼저 형상에 유효한 SRID가 있는지 확인해야 합니다. 유효한 SRID를 이미 확인했으므로 다음으로 변환할 투영의 SRID가 필요합니다. 즉, 지리적 좌표의 SRID는 무엇입니까?

지리적 좌표에 대한 가장 일반적인 SRID는 4326이며, 이는 "WGS84 회전 타원체의 경도/위도"에 해당합니다. 여기에서 정의를 볼 수 있습니다.

> <https://epsg.io/4326>

`spatial_ref_sys` 테이블에서 정의를 가져올 수도 있습니다.

```sql
SELECT srtext FROM spatial_ref_sys WHERE srid = 4326;
```

    GEOGCS["WGS 84",
      DATUM["WGS_1984",
        SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],
        AUTHORITY["EPSG","6326"]],
      PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
      UNIT["degree",0.01745329251994328,AUTHORITY["EPSG","9122"]],
      AUTHORITY["EPSG","4326"]]

'Broad St' 지하철역의 좌표를 지리 정보로 변환해 보겠습니다.

```sql
SELECT ST_AsText(ST_Transform(geom,4326))
FROM nyc_subway_stations
WHERE name = 'Broad St';
```

    POINT(-74.01067146887341 40.70710481558761)

SRID를 지정하지 않고 데이터를 로드하거나 새 지오메트리를 생성하는 경우 SRID 값은 0이 됩니다. `geometries`에서 `geometries` 테이블을 생성할 때 SRID를 지정하지 않았다는 점을 기억하세요. 데이터베이스를 쿼리하는 경우 모든 `nyc_` 테이블의 SRID는 26918이고 `geometries` 테이블의 기본값은 0입니다.

테이블의 SRID 할당을 보려면 데이터베이스의 `geometry_columns` 테이블을 쿼리하세요.

```sql
SELECT f_table_name AS name, srid
FROM geometry_columns;
```

    name         | srid
    ---------------------+-------
    nyc_census_blocks   | 26918
    nyc_homicides       | 26918
    nyc_neighborhoods   | 26918
    nyc_streets         | 26918
    nyc_subway_stations | 26918
    geometries          |     0

그러나 좌표의 SRID가 무엇인지 알고 있다면 형상에서 `ST_SetSRID`를 사용하여 사후에 설정할 수 있습니다. 그런 다음 기하학을 다른 시스템으로 변환할 수 있습니다.

```sql
SELECT ST_AsText(
    ST_Transform(
      ST_SetSRID(geom,26918),
      4326)
    )
  FROM geometries;
```

## 기능 목록

[ST_AsText](http://postgis.net/docs/ST_AsText.html): SRID 메타데이터 없이 도형/지리의 WKT(Well-Known Text) 표현을 반환합니다.

[ST_SetSRID(geometry, srid)](http://postgis.net/docs/ST_SetSRID.html): 기하학의 SRID를 특정 정수 값으로 설정합니다.

[ST_SRID(geometry)](http://postgis.net/docs/ST_SRID.html): Spatial_ref_sys 테이블에 정의된 ST_Geometry에 대한 공간 참조 식별자를 반환합니다.

[ST_Transform(geometry, srid)](http://postgis.net/docs/ST_Transform.html): 정수 매개변수가 참조하는 SRID로 좌표가 변환된 새 기하학을 반환합니다.


---

[← 이전](15_indexing.md) · [목차](00_index.md) · [다음 →](17_projection_exercises.md)
