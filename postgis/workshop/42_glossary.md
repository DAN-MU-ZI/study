# 42. 부록 B: 용어 사전 (Appendix B: Glossary)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/glossary.html>](https://postgis.net/workshops/postgis-intro/glossary.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

---

### **CRS (Coordinate Reference System / 좌표 참조 체계)**
지구상의 지리적 위치를 좌표 숫자로 나타내기 위한 기준 체계로, 타원체 타원 기준점(Datum)과 투영법(Projection)의 결합으로 정의됩니다.

### **GDAL (Geospatial Data Abstraction Library / 지리공간 데이터 추상화 라이브러리)**
오픈 소스 지리공간 재단(OSGeo)에서 개발하는 오픈 소스 래스터 및 벡터(OGR) 데이터 변환/처리 엔진 라이브러리입니다. ("그달" 등으로 발음됨)

### **GeoJSON**
JavaScript의 객체 표기법(JSON) 구조를 기반으로 점, 선, 면 등 공간 지오메트리와 속성 데이터를 웹 표준으로 교환하기 위해 정의된 개방형 텍스트 포맷입니다.

### **GIS (Geographic Information System / 지리정보시스템)**
지리적 공간 위치와 연계된 모든 형태의 데이터를 수집, 저장, 관리, 공간 분석 및 지도 시각화하는 통합 컴퓨터 시스템입니다.

### **GML (Geography Markup Language / 지리 마크업 언어)**
OGC 표준 XML 기반의 공간 지오메트리 및 피처 데이터 교환 규격입니다.

### **JSON (JavaScript Object Notation)**
인간이 읽을 수 있는 텍스트로 데이터를 key-value 쌍 형태로 표현하는 경량 데이터 교환 포맷입니다.

### **KML (Keyhole Markup Language)**
Google Earth 및 웹 지도에서 3차원 지형 및 지리 데이터를 시각화하기 위해 개발된 XML 기반 포맷입니다.

### **OGC (Open Geospatial Consortium / 개방형 공간정보 컨소시엄)**
공간 정보, GIS 데이터 포맷, 웹 공간 서비스(WMS, WFS 등)의 국제 표준 규격을 제정하는 비영리 국제 표준화 기구입니다.

### **OSGeo (Open Source Geospatial Foundation / 오픈 소스 지리공간 재단)**
PostGIS, QGIS, GDAL, GeoServer 등 오픈 소스 지리공간 소프트웨어의 개발과 보급을 지원하는 비영리 재단입니다.

### **SFSQL (Simple Features for SQL / SQL용 단순 피처 규격)**
OGC에서 제정한 표준으로, RDBMS 관계형 데이터베이스에서 공간 데이터 타입(Point, LineString, Polygon)과 공간 함수(`ST_*`)를 정의하는 표준 규격입니다.

### **SLD (Styled Layer Descriptor / 스타일 레이어 기술자)**
OGC 표준으로, 지도 서버(GeoServer, MapServer)에서 벡터 및 래스터 레이어의 지도 심볼 스타일(색상, 두께, 패턴)을 XML로 정의하는 규격입니다.

### **SRID (Spatial Reference System Identifier / 공간 참조 식별자)**
특정 좌표계(CRS)를 고유하게 식별하기 위해 부여된 정수 번호입니다 (예: `4326` = WGS84 경위도, `5179` = 한국 UTM-K, `26918` = NAD83 / UTM Zone 18N). PostGIS의 `spatial_ref_sys` 테이블에 상세 정의가 저장됩니다.

### **SQL (Structured Query Language / 구조화 질의어)**
관계형 데이터베이스(RDBMS)에서 데이터를 저장, 검색, 수정, 관리하기 위한 표준 언어입니다.

### **SQL/MM (SQL Multimedia and Application Packages - Spatial)**
ISO/IEC 국제 표준으로, 멀티미디어 및 3차원/4차원 지오메트리(PointZ, TIN, PolyhedralSurface, Topology)를 SQL에서 다루기 위한 확장 규격입니다.

### **SVG (Scalable Vector Graphics)**
웹 브라우저에서 2차원 벡터 그래픽을 렌더링하기 위해 W3C에서 제정한 XML 기반 그래픽 포맷입니다.

### **WFS (Web Feature Service / 웹 피처 서비스)**
웹을 통해 GIS 지오메트리 벡터 데이터를 직접 읽고 수정(WFS-T)할 수 있는 OGC 표준 웹 인터페이스입니다.

### **WMS (Web Map Service / 웹 맵 서비스)**
클라이언트의 요청 영역(BBox)에 맞춰 서버에서 렌더링된 지도 이미지(PNG, JPEG)를 반환하는 OGC 표준 웹 서비스입니다.

### **WKB (Well-Known Binary)**
지오메트리 객체를 시스템 간 전송이나 데이터베이스 내부 저장을 위해 고속 파싱이 가능한 바이너리(바이트 스트림) 형태로 인코딩한 OGC 표준 포맷입니다.

### **WKT (Well-Known Text)**
지오메트리(예: `POINT(0 0)`, `POLYGON(...)`) 또는 좌표계 정의(PROJCS, GEOGCS)를 사람이 직접 읽고 쓸 수 있는 텍스트 문자열로 표현한 OGC 표준 포맷입니다.


---

[← 이전](41_postgis_functions.md) · [목차](00_index.md) · [다음 →](43_license.md)