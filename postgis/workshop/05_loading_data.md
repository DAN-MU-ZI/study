# 5. 공간 데이터 불러오기 (Loading spatial data)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/loading_data.html>](https://postgis.net/workshops/postgis-intro/loading_data.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostGIS는 다양한 라이브러리와 애플리케이션의 지원을 받으며, 데이터를 불러오는 방법도 여러 가지입니다.

먼저 데이터베이스 백업 파일에서 실습 데이터를 불러오고, 일반적인 도구로 여러 GIS 데이터 형식을 불러오는 표준적인 방법을 살펴보겠습니다.

## 백업 파일 불러오기

1.  pgAdmin 브라우저에서 **nyc** 데이터베이스 아이콘을 **마우스 오른쪽 버튼으로 클릭**하고 **Restore...**를 선택합니다.

![image](screenshots/data_loading_1.png)

2.  워크숍 [데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)을 내려받아 압축을 푼 디렉터리로 이동한 뒤 `nyc_data.backup` 파일을 선택합니다.

![image](screenshots/data_loading_2.png)

3.  **Restore options** 탭을 클릭하고 **Do not save** 섹션까지 아래로 스크롤한 다음 **Owner**를 **Yes**로 설정합니다.

![image](screenshots/data_loading_2a.png)

4.  **Restore** 버튼을 클릭합니다. 데이터베이스 복원이 오류 없이 완료되는지 확인합니다.

![image](screenshots/data_loading_3.png)

5.  불러오기가 끝나면 **nyc** 데이터베이스를 마우스 오른쪽 버튼으로 클릭하고 **Refresh**를 선택합니다. 그러면 데이터베이스에 있는 테이블 정보가 pgAdmin에 새로 반영됩니다.

![image](screenshots/data_loading_4.png)

> [!NOTE]
> 앞에서 다룬 PostgreSQL 데이터베이스 백업 파일 대신 원본 공간 데이터 형식에서 직접 데이터를 불러오는 연습을 하고 싶다면, 이어지는 절에서 여러 명령줄 도구와 QGIS DB Manager를 사용하는 방법을 따라 해 보세요. pgAdmin으로 데이터를 이미 불러왔다면 이 절들은 건너뛰어도 됩니다.

## ogr2ogr로 불러오기

`ogr2ogr`는 일반적인 파일 형식과 공간 데이터베이스를 비롯한 여러 GIS 데이터 형식 사이에서 데이터를 변환하는 명령줄 유틸리티입니다.

**Windows**:

- `ogr2ogr` 빌드는 [GIS Internals](https://www.gisinternals.com/release.php)에서 내려받을 수 있습니다.
- `ogr2ogr`는 [QGIS](https://qgis.org/en/site/forusers/download.html)에 포함되어 있으며 OSGeo4W Shell에서 실행할 수 있습니다.
- `ogr2ogr` 빌드는 [MS4W](https://ms4w.com/download.html)에서도 내려받을 수 있습니다.

**macOS**:

- [Postgres.app](https://postgresapp.com/)을 설치한 경우 `/Applications/Postgres.app/Contents/Versions/*/bin` 디렉터리에 `ogr2ogr`가 있습니다.
- [Homebrew](https://brew.sh/)를 사용한다면 **gdal** 패키지를 설치하여 `ogr2ogr`를 사용할 수 있습니다.

**Linux**:

- 패키지 관리자로 QGIS를 설치했다면 `ogr2ogr`도 **gdal** 또는 *libgdal*\* 패키지의 일부로 설치되어 PATH에서 바로 실행할 수 있습니다.

PostGIS 워크숍 데이터의 `2000/` 하위 디렉터리에는 2010년 자료로 대체된 2000년 인구조사 셰이프파일이 들어 있습니다. 이 파일로 불러오기를 연습하면 백업 파일에서 이미 불러온 데이터와 이름이 겹치지 않습니다. 다음 명령을 실행하기 전에 셸의 현재 디렉터리를 `2000/`으로 변경하세요.

    export PGPASSWORD=mydatabasepassword

비밀번호를 연결 문자열에 직접 넣지 않고 환경 변수로 설정하면, 명령이 실행되는 동안 프로세스 목록에 비밀번호가 노출되지 않습니다.

Windows에서는 `export` 대신 `set`을 사용해야 합니다.

    ogr2ogr \
      -nln nyc_census_blocks_2000 \
      -nlt PROMOTE_TO_MULTI \
      -lco GEOMETRY_NAME=geom \
      -lco FID=gid \
      -lco PRECISION=NO \
      Pg:"dbname=nyc host=localhost user=pramsey port=5432" \
      nyc_census_blocks_2000.shp

보기 쉽도록 명령을 여러 줄로 나누고 줄 끝에 줄 계속 문자 `\`를 표시했습니다. `\`를 생략하려면 명령 전체를 한 줄로 입력해야 합니다.

`ogr2ogr`에는 매우 많은 옵션이 있지만 여기서는 그중 일부만 사용합니다. 이제 명령을 한 줄씩 살펴보겠습니다.

    ogr2ogr \

실행할 프로그램의 이름입니다. 환경에 따라 실행 파일이 있는 디렉터리를 <span class="title-ref">PATH</span>에 추가하거나 실행 파일의 전체 경로를 입력해야 할 수도 있습니다.

    -nln nyc_census_blocks_2000 \

**nln** 옵션은 "new layer name"의 약자로, 대상 데이터베이스에 생성할 테이블의 이름을 지정합니다.

    -nlt PROMOTE_TO_MULTI \

**nlt** 옵션은 "new layer type"의 약자입니다. 특히 셰이프파일에는 다중 부분 도형이 들어 있는 경우가 많으므로, 지오메트리 유형으로 `Polygon` 대신 `MultiPolygon`을 사용하도록 미리 지정해야 합니다.

    -lco GEOMETRY_NAME=geom \
    -lco FID=gid \
    -lco PRECISION=NO \

**lco** 옵션은 "layer creation option"의 약자입니다. 드라이버마다 지원하는 생성 옵션이 다르며, 여기서는 [PostgreSQL 드라이버](https://gdal.org/drivers/vector/pg.html)의 옵션 세 가지를 사용합니다.

- **GEOMETRY_NAME**은 지오메트리 열의 이름을 지정합니다. 여기서는 워크숍의 표준 열 이름에 맞추기 위해 기본값 대신 `geom`을 사용합니다.
- **FID**는 기본 키 열의 이름을 지정합니다. 마찬가지로 워크숍의 표준 이름인 `gid`를 사용합니다.
- **PRECISION**은 숫자 필드를 데이터베이스에 어떤 자료형으로 저장할지 제어합니다. 셰이프파일을 불러올 때는 기본적으로 정밀도가 높은 `numeric` 자료형을 사용하지만, 이 자료형은 `integer`나 `double precision` 같은 단순한 숫자 자료형보다 다루기 어려울 수 있습니다. `PRECISION=NO`를 지정하면 `numeric` 자료형을 사용하지 않습니다.

<!-- -->

    Pg:"dbname=nyc host=localhost user=pramsey port=5432" \

`ogr2ogr`의 인수는 대체로 실행 파일, 옵션, **대상(destination)**, **소스(source)** 순서입니다. 이 줄은 대상 PostgreSQL 데이터베이스의 연결 문자열입니다. `Pg:`는 사용할 드라이버를 나타내며, [연결 문자열](https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-CONNSTRING)에는 공백이 들어갈 수 있으므로 따옴표로 묶습니다.

    nyc_census_blocks_2000.shp

이 명령에서 소스 데이터셋은 불러올 셰이프파일입니다. 소스 위치에 연결 문자열을 지정하고 그 뒤에 레이어 이름을 나열하면 한 번에 여러 레이어를 읽을 수도 있지만, 여기서는 셰이프파일 하나만 불러옵니다.

## 셰이프파일? 그게 뭐야?

셰이프파일이란 무엇일까요? 일반적으로 같은 기본 파일명(예: `nyc_census_blocks`)을 공유하는 `.shp`, `.shx`, `.dbf` 등의 파일 묶음을 셰이프파일이라고 합니다. 엄밀히 말하면 셰이프파일은 `.shp` 확장자의 파일을 뜻하지만, `.shp` 파일만으로는 데이터가 완전하지 않으므로 필수 보조 파일과 함께 배포해야 합니다.

필수 파일:

- `.shp`—셰이프 형식으로, 피처의 지오메트리 자체를 저장합니다.
- `.shx`—셰이프 인덱스 형식으로, 피처 지오메트리의 위치 인덱스를 저장합니다.
- `.dbf`—속성 형식으로, 각 피처의 열 속성을 dBase III 형식으로 저장합니다.

선택적 파일은 다음과 같습니다.

- `.prj`—투영 형식으로, 좌표계와 투영 정보를 WKT(Well-Known Text)로 기술한 일반 텍스트 파일입니다.

`shp2pgsql` 유틸리티는 바이너리 셰이프 데이터를 SQL 명령으로 변환합니다. 이 SQL을 데이터베이스에서 실행하면 셰이프 데이터를 PostGIS에 불러올 수 있습니다.

## shp2pgsql로 불러오기

`shp2pgsql`은 셰이프파일을 SQL로 변환하는 유틸리티입니다. PostGIS 코드베이스의 일부이며 PostGIS 패키지에 포함되어 있습니다. 컴퓨터에 PostgreSQL과 PostGIS를 로컬로 설치했다면 설치 경로의 실행 파일 디렉터리에서 `shp2pgsql`을 찾을 수 있습니다.

`ogr2ogr`와 달리 `shp2pgsql`은 대상 데이터베이스에 직접 연결하지 않고, 입력 셰이프파일을 SQL로 변환해 출력합니다. 사용자는 파이프로 이 SQL을 데이터베이스에 전달하거나 파일로 저장한 뒤 직접 실행해야 합니다.

다음은 앞에서와 같은 데이터를 불러오는 명령의 예입니다.

    export PGPASSWORD=mydatabasepassword

    shp2pgsql \
      -D \
      -I \
      -s 26918 \
      nyc_census_blocks_2000.shp \
      nyc_census_blocks_2000 \
      | psql dbname=nyc user=postgres host=localhost

이제 명령을 한 줄씩 살펴보겠습니다.

    shp2pgsql \

실행할 프로그램의 이름입니다. 소스 데이터 파일을 읽어 SQL을 출력하며, 이 SQL은 파일로 저장하거나 `psql`에 파이프로 전달하여 데이터베이스에 바로 불러올 수 있습니다.

    -D \

**D** 플래그는 기본 `INSERT` 형식보다 훨씬 빠르게 불러올 수 있는 덤프 형식으로 SQL을 생성합니다.

    -I \

**I** 플래그는 데이터 불러오기가 끝난 뒤 테이블에 공간 인덱스를 생성합니다.

    -s 26918 \

**s** 플래그는 데이터의 SRID(공간 참조 체계 식별자)를 지정합니다. 이 워크숍의 소스 데이터는 모두 UTM 18N 좌표계를 사용하며, 해당 SRID는 **26918**입니다(아래 참조).

    nyc_census_blocks_2000.shp \

불러올 소스 셰이프파일입니다.

    nyc_census_blocks_2000 \

대상 데이터베이스에 생성할 테이블의 이름입니다.

    | psql dbname=nyc user=postgres host=localhost

`shp2pgsql`은 SQL 스트림을 생성합니다. 파이프 연산자 `|`는 이 스트림을 데이터베이스 터미널 프로그램인 `psql`의 입력으로 전달합니다. `psql` 뒤의 인수는 대상 데이터베이스의 연결 정보입니다.

## SRID 26918? 그게 뭐야?

대부분의 가져오기 과정은 별도의 설명 없이 이해할 수 있지만, **SRID**는 숙련된 GIS 전문가도 자주 혼동하는 부분입니다.

SRID(Spatial Reference System Identifier, 공간 참조 체계 식별자)는 좌표를 지구상의 실제 위치로 해석할 때 사용할 좌표 참조 체계(CRS)를 가리키는 고유 번호입니다. 좌표값이 같더라도 SRID에 따라 위치와 측정 단위가 다르게 해석될 수 있습니다. 따라서 SRID를 잘못 지정하면 데이터가 엉뚱한 위치에 표시되거나 거리·면적 계산이 부정확해지고, 서로 다른 SRID의 공간 데이터를 연산할 때 오류가 발생할 수 있습니다.

이 워크숍의 데이터에는 **26918**을 사용합니다. 이는 **NAD83 / UTM zone 18N** 좌표계이며, 좌표와 거리의 단위는 미터입니다. 복잡한 좌표계 정의 전체를 매번 입력하는 대신 이 번호 하나로 해당 정의를 참조할 수 있습니다.

워크숍에서 사용하는 좌표 참조 체계의 정의는 다음 온라인 데이터베이스에서 확인할 수 있습니다.

- <https://epsg.io/26918>

또는 PostGIS의 `spatial_ref_sys` 테이블을 조회하여 직접 확인할 수도 있습니다.

```sql
SELECT srtext FROM spatial_ref_sys WHERE srid = 26918;
```

> [!NOTE]
> PostGIS의 `spatial_ref_sys`는 데이터베이스가 알고 있는 공간 참조 체계를 정의하는 OGC 표준 테이블입니다. PostGIS가 기본 제공하는 이 테이블에는 3,000개가 넘는 공간 참조 체계와 체계 간 좌표 변환·재투영에 필요한 정보가 들어 있습니다.

어느 방법을 사용하든 **26918** 공간 참조 체계의 텍스트 정의를 확인할 수 있습니다. 아래 내용은 읽기 쉽도록 들여쓰기를 정리한 것입니다.

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

데이터 디렉터리의 `nyc_neighborhoods.prj` 파일을 열어도 같은 투영 정의를 볼 수 있습니다.

뉴욕시와 같은 지방 기관에서 받은 데이터는 일반적으로 "State Plane"이나 "UTM"으로 표기된 지역 투영 좌표계를 사용합니다. 이 워크숍에서 사용하는 투영 좌표계는 **UTM(Universal Transverse Mercator) Zone 18 North**, 즉 **EPSG:26918**입니다.

## 실습: QGIS로 데이터 확인하기

[QGIS](http://qgis.org)는 데이터를 빠르게 살펴볼 수 있는 데스크톱 GIS 뷰어이자 편집기입니다. 셰이프파일과 PostGIS 데이터베이스를 비롯한 다양한 형식을 지원합니다. 그래픽 인터페이스에서 데이터를 쉽게 탐색하고 간단한 테스트나 스타일 설정도 빠르게 수행할 수 있습니다.

QGIS로 PostGIS 데이터베이스에 연결해 보세요. QGIS는 <https://qgis.org>에서 내려받을 수 있습니다.

먼저 `Layer -> Add Layer -> PostGIS Layers -> New` 메뉴를 열고 안내에 따라 PostGIS 데이터베이스 연결 정보를 입력합니다. 연결을 만든 뒤 **Connect**를 클릭하고 표시할 테이블을 선택하면 레이어를 추가할 수 있습니다.

## QGIS DB Manager를 사용하여 데이터 불러오기

QGIS에는 PostGIS가 활성화된 데이터베이스를 비롯해 여러 종류의 데이터베이스에 연결할 수 있는 [DB Manager](https://docs.qgis.org/3.28/en/docs/user_manual/plugins/core_plugins/plugins_db_manager.html#dbmanager)가 포함되어 있습니다. PostGIS 데이터베이스 연결을 설정한 뒤 `Database -> DB Manager`로 이동하여 아래 그림처럼 해당 데이터베이스 항목을 펼칩니다.

> ![image](screenshots/qgis_dbmanager.png)

여기서 `Import Layer/File` 메뉴를 사용하면 여러 공간 데이터 형식을 불러올 수 있습니다. 다양한 형식으로 데이터를 가져오거나 내보낼 수 있을 뿐 아니라, 그림에 표시된 렌치 아이콘을 사용하여 임시 쿼리 결과를 지도 캔버스에 추가하거나 데이터베이스 뷰를 정의할 수도 있습니다.


---

[← 이전](04_creating_db.md) · [목차](00_index.md) · [다음 →](06_about_data.md)
