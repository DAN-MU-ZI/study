# 2. 소개 (Introduction)

## 공간 데이터베이스란 무엇인가?

공간 데이터베이스(Spatial Database)는 공간 객체(점, 선, 면 등)에 대한 데이터를 저장하고 쿼리할 수 있도록 최적화된 데이터베이스 관리 시스템(DBMS)입니다.

일반적인 데이터베이스는 숫자, 문자열, 날짜와 같은 단순한 데이터 유형을 처리하지만, 공간 데이터베이스는 다음과 같은 추가 기능을 제공합니다:

1. **공간 데이터 타입 (Spatial Data Types)**: 점(Point), 선(LineString), 다각형(Polygon), 다중 다각형(MultiPolygon) 등의 기하학적 형태를 저장합니다.
2. **공간 인덱싱 (Spatial Indexing)**: 2차원 또는 3차원 공간 검색을 고속으로 처리하기 위한 특수 인덱스(예: R-Tree, GiST, BRIN)를 지원합니다.
3. **공간 함수 (Spatial Functions)**: 두 객체 간의 거리, 면적, 교차 여부, 포함 관계, 버퍼 생성 등의 연산을 SQL 쿼리 내에서 직접 수행할 수 있습니다.

![공간 데이터베이스 개념도](introduction/beginning.png)

---

## PostGIS란 무엇인가?

**PostGIS**는 오픈 소스 객체 관계형 데이터베이스인 **PostgreSQL**에 공간 데이터 및 지리 공간 분석 기능을 추가해 주는 확장 기능(Extension)입니다.

PostGIS는 Open Geospatial Consortium(OGC)의 **Simple Features for SQL (SFSQL)** 표준을 엄격하게 준수하며, 세계에서 가장 강력하고 널리 사용되는 오픈 소스 공간 데이터베이스 엔진입니다.

### PostGIS의 주요 특징:
- **다양한 공간 데이터 타입 지원**: 2D, 3D, 4D (XYZM) 지오메트리 지원
- **풍부한 공간 분석 함수**: 수백 개 이상의 OGC 표준 공간 분석 함수 제공 (`ST_Intersects`, `ST_Buffer`, `ST_Distance`, `ST_Union` 등)
- **고성능 공간 인덱스**: PostgreSQL의 GiST(Generalized Search Tree) 및 SP-GiST, BRIN 인덱스를 기반으로 한 초고속 공간 필터링
- **다양한 포맷 입출력**: WKT, WKB, GeoJSON, KML, GML, MVT(Mapbox Vector Tiles), Shapefile, GeoTIFF 등
- **래스터(Raster) 및 토폴로지(Topology)**: 벡터 데이터뿐만 아니라 격자형 래스터 이미지 및 연결 관계를 유지하는 토폴로지 모델 지원
- **우수한 생태계 연동**: QGIS, GeoServer, MapServer, GDAL/OGR, Python(GeoPandas, Shapely), Leaflet, OpenLayers, MapLibre 등 사실상 모든 GIS 소프트웨어 및 라이브러리와 완벽 호환

---

## 공간 데이터 타입 표준 (OGC Simple Features)

OGC(Open Geospatial Consortium)는 공간 데이터를 표준화하기 위해 SFSQL 사양을 정의했습니다. PostGIS는 이 표준을 완벽하게 구현합니다.

| 지오메트리 타입 | 설명 | 예시 |
| :--- | :--- | :--- |
| **Point** | 0차원 점 (단일 위치 좌표) | 버스 정류장, 센서 위치, 도시 좌표 |
| **LineString** | 1차원 선 (연결된 선분들의 집합) | 도로, 강, 지하철 노선 |
| **Polygon** | 2차원 다각형 (내부 면적을 갖는 닫힌 영역) | 건물 윤곽, 토지 구획, 호수 |
| **MultiPoint** | 여러 개의 점 컬렉션 | 다중 측정 센서 위치 |
| **MultiLineString** | 여러 개의 선 컬렉션 | 지류를 포함한 하천망 |
| **MultiPolygon** | 여러 개의 다각형 컬렉션 | 여러 섬으로 구성된 국가(예: 하와이 제도) |
| **GeometryCollection** | 서로 다른 타입의 지오메트리들의 혼합 집합 | 점, 선, 면이 혼합된 데이터 |

![지오메트리 타입 계층](introduction/hierarchy.png)

---

| [⬅️ 1. 환영합니다 (Welcome)](01_welcome.md) | [🏠 워크숍 목차](README.md) | [3. 설치 (Installation) ➡️](03_installation.md) |
| :--- | :---: | ---: |
