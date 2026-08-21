# 5. 공간 데이터 불러오기 (Loading spatial data)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/loading_data.html>](https://postgis.net/workshops/postgis-intro/loading_data.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostGIS는 다양한 공간정보 라이브러리와 애플리케이션의 지원을 받으며, 외부 데이터를 데이터베이스로 가져오는 여러 가지 방법을 제공합니다.

이번 장에서는 먼저 실습용 데이터베이스 백업 덤프 파일(`.backup`)을 pgAdmin을 통해 복원하는 가장 빠른 방법을 다루고, 이어서 실무에서 자주 사용되는 표준 공간 데이터 변환 도구(`ogr2ogr`, `shp2pgsql`, QGIS DB Manager)를 활용하는 방법도 함께 살펴봅니다.

## 백업 파일 복원하기 (Restore)

1. pgAdmin 왼쪽 브라우저 패널에서 **nyc** 데이터베이스를 마우스 오른쪽 버튼으로 클릭하고 **Restore...**를 선택합니다.

   ![image](screenshots/data_loading_1.png)

2. 워크숍 [실습 데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)을 다운로드하여 압축을 해제한 폴더로 이동한 뒤, `nyc_data.backup` 파일을 선택합니다.

   ![image](screenshots/data_loading_2.png)

3. **Restore options** 탭을 클릭하고 아래로 스크롤하여 **Do not save > Owner** 항목을 **Yes**로 활성화합니다. (로컬 사용자와 덤프 생성자의 계정 불일치 방지)

   ![image](screenshots/data_loading_2a.png)

4. **Restore** 버튼을 클릭하여 복원을 실행합니다. 우측 하단 알림창에 "Process completed"가 표시되는지 확인합니다.

   ![image](screenshots/data_loading_3.png)

5. 복원이 완료되면 **nyc** 데이터베이스를 마우스 오른쪽 버튼으로 클릭하고 **Refresh**를 실행합니다. `Schemas > public > Tables`에 로딩된 테이블 목록을 확인할 수 있습니다.

   ![image](screenshots/data_loading_4.png)

> [!NOTE]
> 데이터베이스 백업 덤프 대신 원본 Shapefile 등에서 직접 데이터를 가져오는 명령줄 도구 및 QGIS 연동을 실습하고 싶다면 이어지는 절을 진행하세요. 이미 백업 복원으로 데이터를 로딩했다면 바로 [6장. 실습 데이터 소개](06_about_data.md)로 넘어가도 좋습니다.

---

## ogr2ogr를 활용한 데이터 로딩

`ogr2ogr`는 GDAL/OGR 라이브러리에 포함된 강력한 명령줄 유틸리티로, 셰이프파일(Shapefile), GeoJSON, GeoPackage, PostGIS, Oracle 등 수백 종의 벡터 데이터 포맷 간 상호 변환을 지원합니다.

### 플랫폼별 ogr2ogr 설치 위치
- **Windows**: [GIS Internals](https://www.gisinternals.com/release.php)에서 독립 빌드를 받거나, [QGIS](https://qgis.org/) 설치 시 함께 제공되는 OSGeo4W Shell에서 실행 가능합니다.
- **macOS**: [Postgres.app](https://postgresapp.com/) 내부(`/Applications/Postgres.app/Contents/Versions/*/bin`) 또는 [Homebrew](https://brew.sh/)(`brew install gdal`)를 통해 사용할 수 있습니다.
- **Linux**: 패키지 관리자로 GDAL(`sudo apt install gdal-bin`)을 설치하여 사용합니다.

워크숍 데이터의 `2000/` 하위 디렉터리에는 2000년 미국 인구조사 셰이프파일이 포함되어 있습니다. 터미널의 현재 작업 디렉터리를 `2000/` 폴더로 이동한 뒤 아래 명령을 실행합니다.

```sh
export PGPASSWORD=mydatabasepassword

ogr2ogr \
  -nln nyc_census_blocks_2000 \
  -nlt PROMOTE_TO_MULTI \
  -lco GEOMETRY_NAME=geom \
  -lco FID=gid \
  -lco PRECISION=NO \
  -f "PostgreSQL" \
  Pg:"dbname=nyc host=localhost user=postgres port=5432" \
  nyc_census_blocks_2000.shp
```

> [!NOTE]
> Windows 명령 프롬프트(CMD)에서는 `export` 대신 `set PGPASSWORD=...`를 사용하고 줄 바꿈 문자 `\` 대신 한 줄로 입력하거나 `^`를 사용합니다.

### 주요 옵션 설명
- `-nln (new layer name)`: 대상 데이터베이스에 생성할 테이블명을 지정합니다. (`nyc_census_blocks_2000`)
- `-nlt PROMOTE_TO_MULTI`: 단일 폴리곤(Polygon)과 다중 폴리곤(MultiPolygon)이 섞여 있는 경우 통일된 `MultiPolygon` 타입으로 강제 변환하여 로딩합니다.
- `-lco GEOMETRY_NAME=geom`: 지오메트리 컬럼명을 `geom`으로 지정합니다.
- `-lco FID=gid`: 기본 키(PK) 컬럼명을 `gid`로 지정합니다.
- `-lco PRECISION=NO`: 수치 필드를 복잡한 `numeric` 대신 단순한 `integer`나 `double precision`으로 자동 매핑합니다.
- `Pg:"..."`: 대상 PostgreSQL/PostGIS 데이터베이스의 연결 문자열입니다.
- `nyc_census_blocks_2000.shp`: 로딩할 소스 셰이프파일 경로입니다.

---

## 셰이프파일(Shapefile) 구조의 이해

일반적으로 "셰이프파일"이라고 부르지만 실제로는 동일한 파일명을 가진 여러 개 파일의 세트로 구성됩니다.

### 필수 구성 파일
- `.shp`: 공간 객체의 지오메트리(형상 좌표)를 저장하는 메인 파일
- `.shx`: 지오메트리의 빠른 탐색을 위한 위치 인덱스 파일
- `.dbf`: 각 피처의 속성(Attribute) 데이터를 저장하는 dBase III 형식의 테이블 파일

### 주요 부가 파일
- `.prj`: 좌표계 및 투영 정보를 WKT(Well-Known Text) 포맷으로 기술한 텍스트 파일
- `.cpg`: 속성 데이터의 문자 인코딩(UTF-8, CP949 등)을 명시한 파일

---

## shp2pgsql을 활용한 데이터 로딩

`shp2pgsql`은 PostGIS가 기본 제공하는 전용 셰이프파일 로더 유틸리티입니다. 셰이프파일을 읽어 PostGIS에 삽입할 수 있는 표준 SQL(`INSERT` 또는 `COPY` 덤프 문) 스트림으로 변환해 줍니다.

```sh
export PGPASSWORD=mydatabasepassword

shp2pgsql \
  -D \
  -I \
  -s 26918 \
  nyc_census_blocks_2000.shp \
  nyc_census_blocks_2000 \
  | psql -d nyc -U postgres -h localhost
```

### 주요 플래그 설명
- `-D`: 개별 `INSERT`문 대신 대용량 고속 복사를 지원하는 PostgreSQL `COPY` 덤프 포맷을 사용하여 로딩 속도를 극대화합니다.
- `-I`: 데이터 로딩 완료 직후 지오메트리 컬럼에 GiST 공간 인덱스를 자동으로 생성합니다.
- `-s 26918`: 소스 데이터의 공간 참조 식별자(SRID)를 명시합니다. (UTM Zone 18N, NAD83)
- `| psql ...`: 생성된 SQL 스트림을 파이프(`|`)를 통해 `psql` 클라이언트로 직접 전달하여 데이터베이스에 실행합니다.

---

## SRID 26918이란 무엇인가?

**SRID(Spatial Reference System Identifier, 공간 참조 식별자)**는 공간 좌표가 지구상의 어느 위치와 어떤 투영법에 해당하는지를 고유하게 정의하는 정수 코드입니다.

이 워크숍의 모든 실습 데이터는 **EPSG:26918 (NAD83 / UTM zone 18N)** 좌표계를 사용합니다. 미터(Meter) 단위의 평면 투영 좌표계이므로 거리나 면적 계산을 직관적인 미터/제곱미터 단위로 직접 수행할 수 있습니다.

데이터베이스 내의 `spatial_ref_sys` 테이블을 조회하면 해당 SRID의 정확한 투영 파라미터를 확인할 수 있습니다.

```sql
SELECT srtext FROM spatial_ref_sys WHERE srid = 26918;
```

```text
PROJCS["NAD83 / UTM zone 18N",
  GEOGCS["NAD83",
    DATUM["North_American_Datum_1983",
      SPHEROID["GRS 1980",6378137,298.257222101,AUTHORITY["EPSG","7019"]],
      AUTHORITY["EPSG","6269"]],
    PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
    UNIT["degree",0.01745329251994328,AUTHORITY["EPSG","9122"]],
    AUTHORITY["EPSG","4269"]],
  UNIT["metre",1,AUTHORITY["EPSG","9001"]],
  PROJECTION["Transverse_Mercator"],
  PARAMETER["latitude_of_origin",0],
  PARAMETER["central_meridian",-75],
  PARAMETER["scale_factor",0.9996],
  PARAMETER["false_easting",500000],
  PARAMETER["false_northing",0],
  AUTHORITY["EPSG","26918"],
  AXIS["Easting",EAST],
  AXIS["Northing",NORTH]]
```

---

## QGIS로 PostGIS 데이터 시각화 및 탐색

[QGIS](https://qgis.org)는 오픈 소스 데스크톱 지리정보시스템(GIS)으로, PostGIS 데이터베이스의 공간 테이블을 지도 위에 즉시 시각화하고 스타일을 적용하거나 공간 질의를 테스트하기에 가장 이상적인 도구입니다.

1. QGIS를 실행하고 *Layer > Add Layer > Add PostGIS Layers...* 메뉴를 선택합니다.
2. **New** 버튼을 클릭하여 `localhost`, `5432`, `nyc` 데이터베이스 접속 정보를 등록합니다.
3. 등록된 접속 정보를 선택하고 **Connect**를 클릭한 뒤, 원하는 테이블(`nyc_neighborhoods`, `nyc_streets` 등)을 선택하여 레이어로 추가합니다.

### QGIS DB Manager 활용
QGIS의 내장 도구인 **DB Manager**(*Database > DB Manager*)를 사용하면 SQL 쿼리를 직접 작성하여 지도 레이어로 즉시 띄우거나, 외부 파일(Shapefile, GeoPackage, CSV 등)을 PostGIS 테이블로 GUI 환경에서 편리하게 로딩할 수 있습니다.

![image](screenshots/qgis_dbmanager.png)


---

[← 이전](04_creating_db.md) · [목차](00_index.md) · [다음 →](06_about_data.md)
