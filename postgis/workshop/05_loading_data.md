# 5. 공간 데이터 로딩 (Loading spatial data)

공간 데이터를 데이터베이스로 로드하는 방법에는 여러 가지가 있습니다. 가장 널리 사용되는 도구는 **shp2pgsql** CLI 도구와 **GUI 형태의 Shapefile Loader**, 그리고 **QGIS / GDAL(ogr2ogr)**입니다.

---

## 1. 실습 데이터 압축 풀기

다운로드한 `postgis-workshop-2020.zip` 파일을 압축 해제하면 `data/` 디렉터리에 뉴욕시(NYC) 관련 Shapefile들이 포함되어 있습니다:
- `nyc_census_blocks.shp` (인구조사 블록 구역)
- `nyc_neighborhoods.shp` (동/이웃 지역 경계)
- `nyc_streets.shp` (도로망)
- `nyc_subway_stations.shp` (지하철역 위치)
- `nyc_homicides.shp` (강력사건 발생 위치)

---

## 2. shp2pgsql 도구를 사용한 데이터 로딩

`shp2pgsql`은 Shapefile을 PostgreSQL에 삽입할 수 있는 SQL 덤프 파일이나 파이프 명령어로 변환해 주는 PostGIS 기본 유틸리티입니다.

### 기본 문법:
```bash
shp2pgsql -s <SRID> -I <shapefile.shp> <테이블명> | psql -U postgres -d nyc
```
- `-s <SRID>`: 데이터의 좌표계 식별자 지정 (뉴욕 실습 데이터는 **26918** - NAD83 / UTM zone 18N)
- `-I`: 지오메트리 컬럼에 자동으로 공간 인덱스(GiST)를 생성하는 필수 옵션
- `-D`: 대용량 로딩 속도를 극대화하는 `COPY` 문법 사용

### 실습 데이터 로딩 명령어:

```bash
# 1. 인구조사 블록 데이터 로드
shp2pgsql -s 26918 -I -D data/nyc_census_blocks.shp nyc_census_blocks | psql -U postgres -d nyc

# 2. 동/이웃 구역 데이터 로드
shp2pgsql -s 26918 -I -D data/nyc_neighborhoods.shp nyc_neighborhoods | psql -U postgres -d nyc

# 3. 도로 데이터 로드
shp2pgsql -s 26918 -I -D data/nyc_streets.shp nyc_streets | psql -U postgres -d nyc

# 4. 지하철역 데이터 로드
shp2pgsql -s 26918 -I -D data/nyc_subway_stations.shp nyc_subway_stations | psql -U postgres -d nyc

# 5. 강력사건 데이터 로드
shp2pgsql -s 26918 -I -D data/nyc_homicides.shp nyc_homicides | psql -U postgres -d nyc
```

---

## 3. QGIS 또는 ogr2ogr을 통한 로딩 (대안)

`GDAL/OGR`의 `ogr2ogr` 명령어를 사용하면 Shapefile뿐만 아니라 GeoJSON, GeoPackage, KML, CSV 등 거의 모든 포맷을 바로 PostGIS에 로드할 수 있습니다:

```bash
ogr2ogr -f "PostgreSQL" PG:"dbname=nyc user=postgres" data/nyc_subway_stations.shp -nln nyc_subway_stations -nlt PROMOTE_TO_MULTI -lco OVERWRITE=YES
```

---

| [⬅️ 4. 공간 데이터베이스 생성 (Creating a Spatial Database)](04_creating_db.md) | [🏠 워크숍 목차](README.md) | [6. 실습 데이터 소개 (About our data) ➡️](06_about_data.md) |
| :--- | :---: | ---: |
