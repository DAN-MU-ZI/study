# 제 1 장. PostGIS 소개 (Introduction)

PostGIS는 객체 관계형 데이터베이스인 **PostgreSQL**에 지리 공간(GIS) 객체 및 분석 기능을 추가해 주는 오픈 소스 확장(Extension)입니다.

---

## 1.1. PostGIS란 무엇인가?

PostGIS는 PostgreSQL을 본격적인 엔터프라이즈급 공간 데이터베이스로 탈바꿈시킵니다.
- **OGC Simple Features for SQL (SFSQL)** 표준을 준수하는 다양한 공간 데이터 타입 제공 (Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection)
- 공간 인덱싱: **GiST (Generalized Search Tree)**, **SP-GiST**, **BRIN** 기반 R-Tree 다차원 인덱스 지원
- 수백 개 이상의 공간 연산자 및 분석 함수 내장 (`ST_Intersects`, `ST_Buffer`, `ST_Distance`, `ST_Union`, `ST_Contains` 등)
- 2차원(2D), 3차원(3D Z), 4차원(4D ZM) 지오메트리 및 곡선(CircularString, CompoundCurve, CurvePolygon) 지원
- 구면 타원체 대권 거리 및 면적을 정밀하게 연산하는 **`GEOGRAPHY`** 데이터 타입 제공

---

## 1.2. PostGIS 배포판 구성 요소

PostGIS 공식 배포판은 다음과 같은 모듈들로 구성됩니다:
- **Core PostGIS (`postgis`)**: 벡터 공간 데이터 타입 및 핵심 공간 함수
- **PostGIS Raster (`postgis_raster`)**: 위성/항공 사진, 수치표고모델(DEM) 등 격자형 래스터 데이터 처리 모듈
- **PostGIS Topology (`postgis_topology`)**: 노드-에지-면 위상 관계를 관리하는 토폴로지 데이터 모델 및 도메인
- **PostGIS SFCGAL (`postgis_sfcgal`)**: 3D 솔리드(Solid), TIN(삼각망), 다면체(Polyhedral Surface) 연산 엔진
- **Address Standardizer (`address_standardizer`)**: 주소 문자열 규칙 기반 정규화 파서
- **shp2pgsql / pgsql2shp**: Shapefile 가져오기 및 내보내기 CLI 도구
- **raster2pgsql**: 래스터 이미지 타일링 및 로딩 CLI 도구

---

## 1.3. 표준 지원 (Standards Support)

PostGIS는 Open Geospatial Consortium (OGC) 및 ISO/IEC의 다음 표준을 충족합니다:
- **OGC 06-103r4 / ISO 19125-1**: Geographic information -- Simple feature access -- Part 1: Common architecture
- **OGC 06-104r4 / ISO 19125-2**: Geographic information -- Simple feature access -- Part 2: SQL option
- **ISO/IEC 13249-3**: Information technology -- Database languages -- SQL multimedia and application packages -- Part 3: Spatial (SQL/MM Part 3)

---

| | [🏠 매뉴얼 목차](README.md) | [제 2 장. PostGIS 설치 (Installation) ➡️](02_installation.md) |
| :--- | :---: | ---: |
