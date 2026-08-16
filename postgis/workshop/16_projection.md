# 16. 데이터 투영 (Projecting Data)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/projection.html>](https://postgis.net/workshops/postgis-intro/projection.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지구는 평평하지 않고, 평평한 종이 지도(또는 컴퓨터 화면)에 간단히 표시할 수 있는 방법도 없기 때문에 사람들은 각각 장단점이 있는 온갖 종류의 독창적인 해결책을 생각해 냈습니다. 일부 투영은 영역을 유지하므로 모든 객체는 서로 상대적인 크기를 갖습니다. 다른 투영은 Mercator 투영과 같은 각도(등각)를 유지합니다. 일부 프로젝션에서는 여러 매개변수에 대한 왜곡이 거의 없는 좋은 중간 혼합을 찾으려고 합니다. 모든 투영의 공통점은 (구형) 세계를 평면 데카르트 좌표계로 변환한다는 점이며, 어떤 투영을 선택할지는 데이터를 어떻게 사용할지에 따라 달라집니다.

우리는 `loaded our nyc data <loading_data>`를 통해 이미 예측을 접했습니다. (성가신 SRID 26918을 기억하세요). 그러나 때로는 공간 참조 시스템 간에 변환하고 다시 투영해야 하는 경우도 있습니다. PostGIS에는 `ST_Transform(geometry, srid)` 함수를 사용하여 데이터 투영 변경을 위한 지원 기능이 내장되어 있습니다. 도형의 공간 참조 식별자를 관리하기 위해 PostGIS는 `ST_SRID(geometry)` 및 `ST_SetSRID(geometry, srid)` 기능을 제공합니다.

`ST_SRID` 함수를 사용하여 데이터의 SRID를 확인할 수 있습니다.

```sql
SELECT ST_SRID(geom) FROM nyc_streets LIMIT 1;
```

    26918

그리고 "26918"의 정의는 무엇입니까? "`loading data section <loading_data>`"에서 본 것처럼 정의는 `spatial_ref_sys` 테이블에 포함되어 있습니다. 실제로 **two** 정의가 있습니다. "잘 알려진 텍스트"(`WKT`) 정의는 `srtext` 열에 있고 `proj4text` 열에는 "proj.4" 형식의 두 번째 정의가 있습니다.

```sql
SELECT * FROM spatial_ref_sys WHERE srid = 26918;
```

PostGIS 재투영 엔진은 `spatial_ref_sys` 테이블에서 최상의 투영을 찾으려고 시도합니다.

- **auth_name / auth_srid** proj가 내부 카탈로그에서 유효한 "권한 이름"과 "권한 srid"를 찾을 수 있으면 이를 사용하여 프로젝션 정의를 생성합니다.
- **srtext** proj가 `srtext`에서 정의 개체를 구문 분석하고 형성할 수 있으면 이를 사용합니다.
- **proj4text** 마지막으로 proj는 `proj4text` 처리를 시도합니다.

이 모든 중복성은 PostGIS에서 새 투영을 생성하는 데 필요한 모든 것이 유효한 `srtext` 문자열 또는 `proj4text` 문자열이라는 것을 의미합니다. 모든 공통 기관 이름/코드 쌍은 기본적으로 테이블에 이미 로드되어 있습니다.

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

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 16. 데이터 투영 (Projecting Data)

지구는 둥근 타원체(3D)이지만, 지도와 화면은 평면(2D)입니다. 3차원 지구 표면의 좌표를 2차원 평면으로 변환하는 과정을 **지도 투영(Map Projection)**이라고 합니다.

모든 공간 데이터는 자신이 어떤 기준계로 정의되었는지를 나타내는 **SRID (Spatial Reference System Identifier)**를 가집니다.

---

## 1. 대표적인 좌표계

- **EPSG:4326 (WGS 84)**: GPS에서 사용하는 전 지구 표준 경위도 좌표계 (단위: Degree, 도)
- **EPSG:3857 (Web Mercator / Pseudo-Mercator)**: 구글 지도, OSM, 카카오맵, 네이버맵 등 웹 지도 서비스의 표준 투영 좌표계 (단위: 미터)
- **EPSG:26918 (NAD83 / UTM Zone 18N)**: 뉴욕 지역에 최적화된 UTM 평면 직각 투영 좌표계 (단위: 미터)
- **EPSG:5179 / EPSG:5186 등**: 대한민국 국가 표준 평면 직각 좌표계 (UTM-K, 중부원점 등)

---

## 2. 좌표계 변환 함수 (`ST_Transform`)

`ST_Transform(geom, target_srid)` 함수를 사용하여 지오메트리의 좌표계를 다른 SRID로 실시간 변환할 수 있습니다.

```sql
-- UTM Zone 18N (26918) 좌표를 표준 WGS84 경위도(4326)로 변환하여 조회
SELECT
  name,
  ST_AsText(ST_Transform(geom, 4326)) AS wgs84_coords
FROM nyc_subway_stations
LIMIT 1;
```

출력 결과:
```text
POINT(-73.985130 40.748817) -- (경도, 위도)
```

---

## 3. 좌표계 메타데이터 확인 및 설정

- `ST_SRID(geom)`: 데이터의 현재 SRID 반환
- `ST_SetSRID(geom, srid)`: 데이터의 좌표 숫자는 그대로 두고 메타데이터의 SRID 라벨만 변경 (좌표 변환이 아님에 주의)

> [!WARNING]
> `ST_SetSRID`는 잘못 지정된 SRID 라벨을 바로잡을 때 사용하며, 실제 투영 변환에는 반드시 `ST_Transform`을 사용해야 합니다!

---

| [⬅️ 15. 공간 인덱싱 (Spatial Indexing)](15_indexing.md) | [🏠 워크숍 목차](README.md) | [17. 투영 실습 (Projection Exercises) ➡️](17_projection_exercises.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](15_indexing.md) · [목차](00_index.md) · [다음 →](17_projection_exercises.md)