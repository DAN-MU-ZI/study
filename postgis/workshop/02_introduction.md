# 2. 소개 (Introduction)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/introduction.html>](https://postgis.net/workshops/postgis-intro/introduction.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 공간 데이터베이스(Spatial Database)란 무엇인가?

PostGIS는 대표적인 공간 데이터베이스(Spatial Database)입니다. Oracle Spatial과 Microsoft SQL Server(2008 이상) 역시 공간 데이터베이스입니다. 그렇다면 일반 데이터베이스를 '공간 데이터베이스'로 만드는 핵심 요소는 무엇일까요?

한마디로 요약하면 다음과 같습니다.

> **공간 데이터베이스는 일반 데이터베이스 객체와 마찬가지로 공간 객체(Spatial Object)를 직접 저장하고 조작할 수 있는 데이터베이스입니다.**

공간 데이터베이스의 발전 과정을 간략히 살펴본 뒤, 일반 데이터베이스를 *공간* 데이터베이스로 확장하는 세 가지 핵심 요소인 **공간 데이터 타입(Data Types)**, **공간 인덱스(Indexes)**, **공간 함수(Functions)**를 알아보겠습니다.

1. **공간 데이터 타입(Spatial Data Types)**: 점(Point), 선(LineString), 다각형(Polygon) 등 기하학적 형태를 표현합니다.
2. **공간 인덱스(Spatial Indexing)**: 공간 연산을 효율적으로 처리하기 위해 다차원 인덱스 구조를 사용합니다.
3. **공간 함수(Spatial Functions)**: `SQL` 환경에서 공간 속성과 공간 관계(교차, 포함, 거리 등)를 질의하고 분석할 수 있는 함수를 제공합니다.

이 세 가지 요소가 결합하여 공간 데이터를 효율적으로 저장, 분석, 최적화할 수 있는 유연하고 강력한 플랫폼을 구성합니다.

### 공간 데이터베이스의 발전 과정

초기 1세대 `GIS` 구현에서는 모든 공간 데이터가 독립적인 플랫 파일(Flat File)에 저장되었으며, 데이터를 읽고 조작하려면 전용 GIS 소프트웨어가 반드시 필요했습니다. 이러한 시스템은 단일 조직 내의 독립적인 요구를 처리하기 위해 제작된 폐쇄형 독점 시스템이었습니다.

2세대 공간 시스템은 속성(비공간 데이터)을 관계형 데이터베이스(RDBMS)에 저장했지만, 공간 데이터 자체와 완전히 통합되지 않아 유연성과 성능에 한계가 있었습니다.

**진정한 3세대 공간 데이터베이스는 공간 피처(Spatial Feature)를 데이터베이스의 일급 시민(First-class Object)으로 다루기 시작하면서 탄생했습니다.**

공간 데이터베이스는 공간 데이터를 RDBMS 엔진 내부와 완전히 통합합니다. 이를 통해 시스템의 중심축이 'GIS 중심'에서 '데이터베이스 중심'으로 전환되었습니다.

![초기 GIS가 공간 데이터 파일과 속성 데이터베이스를 분리해 관리하던 구조](introduction/beginning.png)

> [!NOTE]
> 공간 데이터베이스 관리 시스템은 지리 정보 외에도 다양한 분야에 활용될 수 있습니다. 인체 해부학 모델링, 대규모 집적 회로(VLSI) 설계, 분자 구조 시각화, 전자기장 해석 등 2차원/3차원 공간 좌표를 다루는 모든 데이터 관리에 활용됩니다.

### 공간 데이터 타입 (Spatial Data Types)

일반 데이터베이스에는 문자열(String), 숫자(Number), 날짜(Date) 등의 기본 타입이 있습니다. 공간 데이터베이스는 여기에 **지리적 피처(Geographic Feature)**를 표현하기 위한 추가 공간 타입을 제공합니다. 이러한 공간 데이터 타입은 경계, 차원, 좌표 등의 공간 구조를 추상화하고 캡슐화합니다.

![공간 데이터 타입이 점 선 면과 컬렉션으로 확장되는 계층 구조](introduction/hierarchy.png)

공간 데이터 타입은 객체지향적 타입 계층 구조(Type Hierarchy)로 구성됩니다. 각 하위 타입은 상위 타입의 구조(속성)와 동작(메서드/함수)을 상속받습니다.

### 공간 인덱스 및 경계 상자 (Bounding Box)

일반 데이터베이스는 데이터에 빠르게 임의 접근(Random Access)할 수 있도록 **인덱스(Indexes)**를 제공합니다. 표준 자료형(숫자, 문자열, 날짜)의 인덱싱에는 주로 [B-트리(B-Tree)](http://en.wikipedia.org/wiki/B-tree) 인덱스가 사용됩니다.

B-트리는 값의 자연스러운 정렬 순서(Natural Sorting Order: 작다, 크다, 같다)를 기준으로 데이터를 분할하여 계층적 트리로 구성합니다.

그러나 다각형(Polygon)과 같은 공간 객체는 서로 겹치거나 포함될 수 있으며, 2차원(또는 그 이상) 평면에 배치되기 때문에 단순한 1차원 B-트리로는 효율적으로 인덱싱할 수 없습니다. 따라서 공간 데이터베이스는 "주어진 특정 영역(경계 상자) 안에 어떤 객체가 존재하는가?"라는 질문에 빠르게 답할 수 있는 **공간 인덱스(Spatial Index)**를 제공합니다.

**경계 상자(Bounding Box, 바운딩 박스)**는 주어진 공간 피처를 완전히 감싸는 좌표축에 평행한 가장 작은 직사각형(MBR, Minimum Bounding Rectangle)입니다.

![복잡한 지오메트리를 둘러싸는 최소 경계 상자](introduction/boundingbox.png)

경계 상자를 사용하는 이유는 복잡한 다각형의 정확한 포함 여부를 계산하는 것은 비용이 매우 크지만, 단순한 직사각형끼리의 교차 여부를 판별하는 것은 극도로 빠르기 때문입니다. 아무리 복잡한 다각형이나 라인스트링이라도 간단한 4개 좌표(Xmin, Ymin, Xmax, Ymax)의 경계 상자로 단순화할 수 있습니다.

공간 인덱스는 B-트리처럼 한 번에 최종 결과를 반환하는 대신, 1단계에서 후보군(근사 결과)을 고속으로 추출합니다. 예를 들어 "이 다각형 안에 어떤 선들이 있는가?"라는 질의를 받으면, 1단계로 "이 다각형의 경계 상자와 겹치는 경계 상자를 가진 선들은 무엇인가?"를 인덱스로 신속히 필터링한 뒤, 2단계에서 필터링된 후보들에 대해서만 정밀한 공간 연산을 수행합니다.

공간 데이터베이스마다 구현 방식은 다양합니다. 가장 널리 쓰이는 구조는 [R-Tree](http://en.wikipedia.org/wiki/R-tree)와 [Quadtree](http://en.wikipedia.org/wiki/Quadtree)이며, PostGIS는 PostgreSQL의 범용 검색 트리(GiST)를 기반으로 R-Tree 인덱스를 구현합니다. 이 외에도 [그리드 인덱스(Grid Index)](<http://en.wikipedia.org/wiki/Grid_(spatial_index)>)나 [GeoHash 인덱스](https://en.wikipedia.org/wiki/Geohash) 등이 있습니다.

### 공간 함수 (Spatial Functions)

일반 데이터베이스가 문자열 연결, 날짜 계산, 수치 연산 등을 위한 내장 함수를 제공하듯, 공간 데이터베이스는 공간 객체를 분석하고 관계를 판별하며 형태를 조작하기 위한 풍부한 **공간 함수(Spatial Functions)**를 제공합니다.

공간 함수는 크게 다음 5개 범주로 분류할 수 있습니다.

1. **변환(Conversion)**: 지오메트리와 외부 데이터 포맷(WKT, WKB, GeoJSON, GML, KML 등) 간 상호 변환을 수행하는 함수.
2. **관리(Management)**: 공간 테이블 등록, SRID 관리 등 PostGIS 시스템 메타데이터를 관리하는 함수.
3. **조회 및 측정(Retrieval & Measurement)**: 지오메트리의 속성(면적, 길이, 둘레, 중심점, 정점 개수 등)을 추출하고 측정하는 함수.
4. **비교 및 관계 판별(Comparison & Predicates)**: 두 지오메트리 간의 공간적 관계(교차, 포함, 접촉, 겹침 등)를 판별하여 참/거짓을 반환하는 함수.
5. **생성 및 가공(Generation & Processing)**: 기존 지오메트리로부터 새로운 지오메트리를 생성(버퍼, 교집합, 합집합, 볼록 껍질 등)하는 함수.

이러한 표준 함수 규격은 OGC(Open Geospatial Consortium)의 `SFSQL`(Simple Features for SQL) 명세에 정의되어 있으며, PostGIS는 이를 충실히 구현하고 추가적인 고급 확장 함수들을 다수 제공합니다.

---

## PostGIS란 무엇인가?

PostGIS는 공간 데이터 타입, 공간 인덱스, 공간 함수에 대한 지원을 추가하여 객체-관계형 데이터베이스 관리 시스템인 [PostgreSQL](http://www.postgresql.org/)을 완벽한 공간 데이터베이스로 변환해 주는 확장(Extension) 프로그램입니다.

PostgreSQL을 기반으로 구축되었기 때문에, PostGIS는 PostgreSQL의 강력한 트랜잭션(ACID), 고성능 쿼리 최적화기, 엔터프라이즈급 신뢰성 및 오픈 표준 기능을 그대로 상속받습니다.

### PostgreSQL이란?

PostgreSQL은 강력한 오픈 소스 객체-관계형 데이터베이스(ORDBMS)입니다. BSD 계열 오픈 소스 라이선스를 따르며, 단일 벤더에 종속되지 않고 [전 세계 수많은 개발자와 기업으로 구성된 글로벌 커뮤니티](https://www.postgresql.org/community/contributors/)가 협력하여 개발하고 있습니다.

PostgreSQL은 설계 초기부터 런타임에 새로운 데이터 타입, 연산자, 인덱스 메서드, 함수를 자유롭게 추가할 수 있는 **플러그형 타입 확장성(Extensibility)**을 염두에 두고 설계되었습니다. 덕분에 PostGIS 개발팀은 PostgreSQL 코어를 직접 수정하지 않고도 코어 엔진과 매우 긴밀하게 결합된 고성능 공간 확장을 구현할 수 있었습니다.

#### 왜 PostgreSQL을 선택했을까?

"왜 MySQL 대신 PostgreSQL 위에 PostGIS를 구축했는가?"라는 질문을 자주 받습니다.

PostgreSQL을 선택한 이유는 명확합니다.

- 검증된 트랜잭션 무결성(ACID) 및 엔터프라이즈 신뢰성 기본 제공
- 엄격하고 충실한 SQL 표준 지원 (전체 SQL92 및 이후 표준)
- 완벽한 사용자 정의 타입/함수 확장 구조 (Pluggable Type Extensibility)
- 대용량 GIS 객체(수만 개 정점의 폴리곤 등)를 행 단위 크기 제한 없이 저장할 수 있는 TOAST(The Oversized-Attribute Storage Technique) 메커니즘 지원
- R-Tree 등 사용자 정의 공간 인덱스를 유연하게 구현할 수 있는 범용 검색 트리(GiST) 프레임워크 제공
- 개방적이고 건강한 커뮤니티 중심 개발 모델

MySQL 4.1 시절 기본 공간 타입을 도입했을 때, 공간 객체를 문자열 컬럼 위에 덧붙이는 구조여서 확장성에 한계가 있었습니다. 반면 PostgreSQL은 처음부터 공간 확장에 최적화된 아키텍처를 갖추고 있었기 때문에 PostGIS 0.1이 한 달 만에 탄생할 수 있었습니다.

### 왜 플랫 파일(Shapefile 등) 대신 공간 데이터베이스를 써야 할까?

[Shapefile](http://en.wikipedia.org/wiki/Shapefile), Esri File Geodatabase, [GeoPackage](https://www.geopackage.org/) 등은 전통적인 GIS 데이터 저장 포맷이지만, 다음과 같은 한계가 있습니다.

- **데이터 접근 및 분석을 위한 전용 소프트웨어 의존성**: SQL과 같은 표준 질의 언어가 없어 임의의 데이터 추출이나 분석을 수행할 때 매번 전용 GIS 프로그램을 열거나 코드를 직접 작성해야 합니다.
- **동시 다중 사용자 접근 시 데이터 손상 위험**: 파일 기반 구조에서는 여러 사용자가 동시에 데이터를 수정할 때 충돌이 발생하거나 파일이 손상되기 쉽습니다. 이를 막기 위해 락킹(Locking)과 트랜잭션을 구현하다 보면 결국 데이터베이스 시스템을 다시 만드는 셈이 됩니다.
- **복잡한 공간 분석 쿼리의 비효율성**: 공간 조인, 반경 내 검색, 집계 연산 등 SQL 한 줄로 처리할 수 있는 연산을 파일 기반 시스템에서 처리하려면 수백 줄의 복잡한 커스텀 스크립트가 필요합니다.

다중 사용자 동시 접근, 임의의 복잡한 공간 SQL 쿼리, 대용량 데이터 세트에 대한 고속 인덱싱 성능은 공간 데이터베이스가 파일 시스템과 차별화되는 가장 큰 장점입니다.

### PostGIS의 역사

- **2001년 5월**: 캐나다의 [Refractions Research](http://www.refractions.net/)가 PostGIS 0.1을 공개.
- **SFSQL 표준 채택**: OGC의 "Simple Features for SQL" 사양을 채택하여 함수 명명 규칙 및 표준 구조를 확립.
- **MapServer 연동**: 공간 데이터베이스 데이터를 웹 지도로 실시간 시각화하는 최초의 오픈 소스 연동 구현.
- **GEOS 라이브러리 통합 (0.8)**: C++ 기반 Geometry Engine Open Source(GEOS) 라이브러리를 연동하여 `ST_Intersects`, `ST_Buffer`, `ST_Union` 등 복잡한 위상 기하 연산 지원.
- **경량 지오메트리 포맷 도입 (1.0)**: 메모리 및 디스크 오버헤드를 300% 이상 절감하는 고속 경량 바이너리 포맷(LWGEOM) 적용.
- **지속적인 진화**: 래스터(Raster) 지원, 토폴로지(Topology), 3D/4D 연산, KNN 최근접 이웃 인덱스 검색, 병렬 쿼리 지원 등 현대 엔터프라이즈 공간 인프라의 핵심 표준으로 자리매김.

### PostGIS를 지원하는 주요 소프트웨어 및 플랫폼

PostGIS는 전 세계 수많은 오픈 소스 및 상용 소프트웨어, 클라우드 DBaaS 플랫폼에서 폭넓게 지원됩니다.

| 구분 | 오픈 소스 / 무료 도구 | 상용 소프트웨어 / 클라우드 DBaaS |
| :--- | :--- | :--- |
| **데이터 로딩 / 변환** | - `shp2pgsql`<br>- `ogr2ogr`<br>- `raster2pgsql`<br>- GeoKettle | - Safe Software FME Desktop<br>- 다양한 ETL 도구 |
| **웹 지도 서버** | - MapServer<br>- GeoServer / GeoNode<br>- pg_tileserv (벡터 타일 서버)<br>- pg_featureserv (OGC API Features)<br>- MapLibre / Tegola<br>- QGIS Server | - ESRI ArcGIS Server / ArcGIS Online<br>- Cadcorp GeognoSIS |
| **데스크톱 GIS / 클라이언트** | - QGIS<br>- pgAdmin<br>- DBeaver<br>- OpenJUMP<br>- GRASS GIS<br>- uDig / gvSIG | - ESRI ArcGIS Pro / ArcMap<br>- Manifold System<br>- MapInfo Professional<br>- Global Mapper |
| **클라우드 매니지드 DB (DBaaS)** | - 자체 구축 PostgreSQL + PostGIS | - Amazon RDS / Aurora for PostgreSQL<br>- Google Cloud SQL for PostgreSQL<br>- Azure Database for PostgreSQL<br>- Crunchy Bridge<br>- Aiven for PostgreSQL<br>- CARTO |


---

[← 이전](01_welcome.md) · [목차](00_index.md) · [다음 →](03_installation.md)
