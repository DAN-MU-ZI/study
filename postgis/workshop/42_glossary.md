# 42. 부록 B: 용어 사전 (Appendix B: Glossary)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/glossary.html>](https://postgis.net/workshops/postgis-intro/glossary.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

<div class="glossary">

CRS\
"좌표 참조 시스템". 지리 좌표계와 투영 좌표계의 조합입니다.

그달\
[지리공간 데이터 추상화 라이브러리](http://gdal.org)는 "GOO-duhl"로 발음되며, 오픈 소스 및 독점 소프트웨어 모두에서 널리 사용되는 다양한 형식을 지원하는 오픈 소스 래스터 액세스 라이브러리입니다.

GeoJSON\
"Javascript Object Notation"은 Javascript 가상 머신에서 구문 분석이 매우 빠른 텍스트 형식입니다. 공간에서는 [GeoJSON](http://geojson.org)의 확장 사양이 일반적으로 사용됩니다.

GIS\
[지리정보시스템](http://en.wikipedia.org/wiki/Geographic_information_system) 또는 지리정보시스템은 위치와 연계된 데이터를 포착, 저장, 분석, 관리, 제시하는 시스템이다.

GML\
[지리 마크업 언어](http://www.opengeospatial.org/standards/gml). GML은 공간 특징 정보를 표현하기 위한 `OGC` 표준 XML 형식입니다.

JSON\
"\`Javascript Object Notation \<[http://en.wikipedia.org/wiki/JSON\\\\\\"는](http://en.wikipedia.org/wiki/JSON\>\`\_"는) Javascript 가상 머신에서 구문 분석이 매우 빠른 텍스트 형식입니다. 공간에서는 [GeoJSON](http://geojson.org)의 확장 사양이 일반적으로 사용됩니다.

JSTL\
JSP에서 처리되는 많은 표준 기능(데이터베이스 쿼리, 반복, 조건부)을 간결한 구문으로 캡슐화하는 `JSP`용 태그 라이브러리인 "JavaServer 페이지 템플릿 라이브러리"입니다.

JSP\
"JavaServer Pages"는 마크업 및 Java 절차 코드의 인터리빙을 허용하는 Java 서버 애플리케이션용 스크립팅 시스템입니다.

KML\
Google Earth에서 사용하는 공간 XML 형식인 "Keyhole Markup Language"입니다. Google Earth는 원래 "Keyhole"이라는 회사에서 작성되었으므로 이름에 (현재는 모호함) 참조가 있습니다.

OGC\
[Open Geospatial Consortium](http://opengeospatial.org/)(OGC)은 지리공간 서비스에 대한 사양을 개발하는 표준 기관입니다.

OSGeo\
[오픈 소스 지리 공간 재단](http://osgeo.org)(OSGeo)은 오픈 소스 지리 공간 소프트웨어의 홍보 및 지원에 전념하는 비영리 재단입니다.

SFSQL\
`OGC`의 [SQL용 단순 기능](http://www.opengeospatial.org/standards/sfs)(SFSQL) 사양은 표준 공간 데이터베이스를 구성하는 유형과 기능을 정의합니다.

SLD\
`OGC`의 [Styled Layer Descriptor](http://www.opengeospatial.org/standards/sld)(SLD) 사양은 벡터 기능의 지도 제작 렌더링을 설명하기 위한 형식을 정의합니다.

SRID\
"공간 참조 ID"는 특정 "좌표 참조 시스템"에 할당된 고유 번호입니다. PostGIS 테이블 **spatial_ref_sys**에는 좌표 참조 시스템의 잘 알려진 srid 값과 텍스트 표현의 대규모 컬렉션이 포함되어 있습니다.

SQL\
"\`Structured query language \<[http://en.wikipedia.org/wiki/SQL\\\\\\"는](http://en.wikipedia.org/wiki/SQL\>\`\_"는) 관계형 데이터베이스를 쿼리하는 표준 수단입니다.

SQL/MM\
[SQL 멀티미디어](http://www.fer.hr/_download/repository/SQLMM_Spatial-_The_Standard_to_Manage_Spatial_Data_in_Relational_Database_Systems.pdf); 공간 유형에 대한 실질적인 섹션을 포함하여 확장 유형에 대한 여러 섹션이 포함되어 있습니다.

SVG\
"\`Scalable vector graphics \<[http://en.wikipedia.org/wiki/Scalable_Vector_Graphics\\\\\\"는](http://en.wikipedia.org/wiki/Scalable_Vector_Graphics\>\`\_"는) 정적 및 동적(예: 대화형 또는 애니메이션)의 2차원 벡터 그래픽을 설명하기 위한 XML 기반 파일 형식의 사양 제품군입니다.

WFS\
`OGC`의 [웹 기능 서비스](http://www.opengeospatial.org/standards/wfs)(WFS) 사양은 웹에서 지리 기능을 읽고 쓰기 위한 인터페이스를 정의합니다.

WMS\
`OGC`의 [웹 지도 서비스](http://www.opengeospatial.org/standards/wms)(WMS) 사양은 웹을 통해 렌더링된 지도 이미지를 요청하기 위한 인터페이스를 정의합니다.

WKB\
"잘 알려진 바이너리". SQL 사양의 단순 기능(`SFSQL`)에 설명된 기하학의 이진 표현을 참조합니다.

WKT\
"\`Well-known text \<[http://en.wikipedia.org/wiki/Well-known_text\\\\\\](http://en.wikipedia.org/wiki/Well-known_text\>\`\)\_". "POINT", "LINESTRING", "POLYGON" 등으로 시작하는 문자열을 사용하여 형상의 텍스트 표현을 참조할 수 있습니다. 또는 "PROJCS", "GEOGCS" 등으로 시작하는 문자열을 사용하여 `CRS`의 텍스트 표현을 참조할 수 있습니다. 잘 알려진 텍스트 표현은 `OGC` 표준이지만 자체 사양 문서는 없습니다. WKT(지오메트리 및 CRS용)에 대한 첫 번째 설명은 `SFSQL` 1.0 사양에 나타났습니다.

</div>

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 42. 부록 B: 용어 사전 (Appendix B: Glossary)

공간 데이터베이스 및 PostGIS에서 사용하는 핵심 기술 용어 정리입니다.

---

- **Bounding Box (MBR, 최소 경계 사각형)**: 지오메트리를 감싸는 $X_{min}, Y_{min}, X_{max}, Y_{max}$ 직사각형 경계로, 공간 인덱스의 1단계 고속 필터링에 사용됩니다.
- **CRS / SRS (좌표 참조계)**: 지표면 상의 위치를 수치 좌표로 정의하기 위한 기준 좌표계입니다.
- **EPSG 코드**: 석유 탐사 표준 기구(EPSG)에서 전 세계 좌표계에 부여한 고유 식별 번호입니다 (예: 4326 = WGS84, 3857 = Web Mercator).
- **Geography**: 지구 곡면(타원체)을 기준으로 하는 구면 지리 좌표 데이터 타입입니다 (항상 미터 단위 계산).
- **Geometry**: 2차원 또는 3차원 유클리드 평면 좌표계를 기준으로 하는 공간 데이터 타입입니다.
- **GiST (Generalized Search Tree)**: PostgreSQL에서 R-Tree 계층형 공간 인덱스를 구현하기 위해 사용하는 확장 인덱스 프레임워크입니다.
- **KNN (K-Nearest Neighbors)**: 특정 지점에서 가장 가까운 $K$개의 이웃 객체를 찾는 공간 쿼리 기법입니다 (`<->` 연산자 사용).
- **Raster (래스터)**: 위성 사진, 표고 모델처럼 격자(Grid) 픽셀 형태로 표현되는 공간 데이터입니다.
- **SRID (Spatial Reference System Identifier)**: 공간 데이터베이스에서 좌표 참조계를 식별하기 위한 고유 정수 ID입니다.
- **Topology (토폴로지)**: 노드(Node), 에지(Edge), 면(Face) 간의 위상학적 연결 관계를 저장하여 경계 불일치를 방지하는 데이터 모델입니다.
- **Vector (벡터)**: 점(Point), 선(LineString), 면(Polygon)의 정점 좌표들의 집합으로 표현되는 공간 데이터입니다.
- **WKB (Well-Known Binary)**: 지오메트리를 데이터베이스 내부에 저장하거나 전송할 때 사용하는 표준 바이너리 인코딩 포맷입니다.
- **WKT (Well-Known Text)**: 지오메트리를 사람이 읽을 수 있는 텍스트 형식으로 표현하는 OGC 표준 표기법입니다 (예: `POINT(1 2)`).

---

| [⬅️ 41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)](41_postgis_functions.md) | [🏠 워크숍 목차](README.md) | [43. 부록 C: 라이선스 (Appendix C: License) ➡️](43_license.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](41_postgis_functions.md) · [목차](00_index.md) · [다음 →](43_license.md)