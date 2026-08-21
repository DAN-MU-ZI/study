# 41. 부록 A: PostGIS 주요 함수 목록 (Appendix A: PostGIS Functions)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/postgis-functions.html>](https://postgis.net/workshops/postgis-intro/postgis-functions.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

---

## 1. 지오메트리 생성자 (Constructors)

- **`ST_MakePoint(Longitude, Latitude, [Elevation], [Measure])`**\
  지정된 X(경도), Y(위도), 선택적 Z(고도), M(측정값) 좌표로부터 새로운 포인트(Point) 객체를 생성합니다. *(좌표 입력 순서가 경도, 위도임에 유의)*
- **`ST_GeomFromText(WellKnownText, [srid])`**\
  표준 OGC WKT(Well-Known Text) 문자열과 SRID로부터 지오메트리 객체를 파싱하여 생성합니다.
- **`ST_SetSRID(geometry, srid)`**\
  지오메트리의 좌표값은 그대로 유지한 채 메타데이터인 SRID 정수값만 새로 설정합니다.
- **`ST_Expand(geometry, radius)`**\
  입력 지오메트리의 경계 상자(바운딩 박스)를 사방으로 지정된 거리만큼 확장한 2D 사각형 지오메트리를 생성합니다.

---

## 2. 입출력 및 직렬화 (Outputs)

- **`ST_AsText(geometry)`**\
  지오메트리를 사람이 읽을 수 있는 표준 WKT(Well-Known Text) 문자열로 변환합니다.
- **`ST_AsBinary(geometry)`**\
  지오메트리를 시스템 간 고속 전송을 위한 표준 WKB(Well-Known Binary) 바이트 배열(`bytea`)로 변환합니다.
- **`ST_AsGeoJSON(geometry)`**\
  웹 매핑 애플리케이션 연동을 위한 표준 GeoJSON 포맷 문자열로 변환합니다.
- **`ST_AsGML(geometry)`**\
  OGC 표준 GML(Geography Markup Language) XML 문자열로 변환합니다.
- **`ST_AsSVG(geometry)`**\
  웹 브라우저 렌더링을 위한 SVG(Scalable Vector Graphics) 패스 데이터로 변환합니다.

---

## 3. 공간 측정 및 분석 (Measurements)

- **`ST_Area(geometry)`**\
  폴리곤/멀티폴리곤 지오메트리의 2차원 평면 면적(투영 좌표계 단위)을 계산합니다.
- **`ST_Length(geometry)`**\
  라인스트링/멀티라인스트링의 2차원 평면 선형 길이를 계산합니다.
- **`ST_Perimeter(geometry)`**\
  다각형의 외곽 둘레 길이를 계산합니다.
- **`ST_NumPoints(linestring)`**\
  선형 객체를 구성하는 정점(Vertices)의 총 개수를 반환합니다.
- **`ST_NumRings(polygon)`**\
  다각형을 구성하는 링(외곽 링 1개 + 내부 구멍 링 N개)의 총 개수를 반환합니다.
- **`ST_NumGeometries(geometry)`**\
  멀티/컬렉션 지오메트리를 구성하는 하위 단일 지오메트리의 개수를 반환합니다.

---

## 4. 공간 관계 및 술어 (Relationships)

- **`ST_Distance(geometry A, geometry B)`**\
  두 지오메트리 간의 최단 유클리드 2차원 거리를 계산합니다.
- **`ST_DWithin(geometry A, geometry B, distance)`**\
  두 지오메트리 간의 최단 거리가 지정된 반경 이내일 때 공간 인덱스를 활용하여 빠르게 `TRUE`를 반환합니다.
- **`ST_Intersects(geometry A, geometry B)`**\
  두 지오메트리가 공간을 조금이라도 공유(교차/접촉/포함)하면 `TRUE`를 반환합니다.
- **`ST_Contains(geometry A, geometry B)`**\
  지오메트리 A가 지오메트리 B를 자신의 내부에 완전히 포함할 때 `TRUE`를 반환합니다.
- **`ST_Crosses(geometry A, geometry B)`**\
  두 선형 객체 또는 선과 면 객체가 내부에서 서로 십자로 가로지를 때 `TRUE`를 반환합니다.
- **`ST_Touches(geometry A, geometry B)`**\
  두 지오메트리가 내부는 겹치지 않고 외곽 경계선/정점에서만 서로 맞닿아 있을 때 `TRUE`를 반환합니다.


---

[← 이전](40_advanced_geometry_construction.md) · [목차](00_index.md) · [다음 →](42_glossary.md)