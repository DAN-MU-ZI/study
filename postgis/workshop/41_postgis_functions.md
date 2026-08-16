# 41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/postgis-functions.html>](https://postgis.net/workshops/postgis-intro/postgis-functions.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 생성자

`ST_MakePoint(Longitude, Latitude)`\
새로운 포인트를 반환합니다. 좌표 순서(경도, 위도)를 확인하세요.

`ST_GeomFromText(WellKnownText, srid)`\
표준 WKT 문자열 및 srid에서 새 기하학을 반환합니다.

`ST_SetSRID(geometry, srid)`\
형상의 srid를 업데이트합니다. 동일한 기하학을 반환합니다. 이는 지오메트리의 좌표를 변경하지 않고 srid만 업데이트합니다. 이 기능은 Srid 없이 생성된 형상을 조절하는 데 유용합니다.

`ST_Expand(geometry, Radius)`\
입력 도형의 확장된 경계 상자인 새 도형을 반환합니다. 이 기능은 색인 검색에 사용할 봉투를 만드는 데 유용합니다.

## 출력

`ST_AsText(geometry)`\
사람이 읽을 수 있는 텍스트 형식으로 도형을 반환합니다.

`ST_AsGML(geometry)`\
표준 OGC `GML` 형식으로 도형을 반환합니다.

`ST_AsGeoJSON(geometry)`\
도형을 표준 [GeoJSON](http://geojson.org) 형식으로 반환합니다.

## 측정

`ST_Area(geometry)`\
공간 참조 시스템의 단위로 도형의 영역을 반환합니다.

`ST_Length(geometry)`\
공간 참조 시스템의 단위로 도형의 길이를 반환합니다.

`ST_Perimeter(geometry)`\
공간 참조 시스템의 단위로 기하학의 둘레를 반환합니다.

`ST_NumPoints(linestring)`\
라인스트링의 정점 수를 반환합니다.

`ST_NumRings(polygon)`\
다각형의 링 수를 반환합니다.

`ST_NumGeometries(geometry)`\
도형 컬렉션의 도형 수를 반환합니다.

## 관계

`ST_Distance(geometry, geometry)`\
공간 참조 시스템의 단위로 두 Geometry 사이의 거리를 리턴합니다.

`ST_DWithin(geometry, geometry, radius)`\
도형이 서로의 반경 거리 내에 있으면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Intersects(geometry, geometry)`\
기하학이 서로 분리되지 않으면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Contains(geometry, geometry)`\
첫 번째 도형이 두 번째 도형을 완전히 포함하면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Crosses(geometry, geometry)`\
선이나 다각형 경계가 다른 선이나 다각형 경계와 교차하면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)

워크숍에서 자주 사용하는 주요 PostGIS SQL 함수 요약 레퍼런스입니다.

---

## 지오메트리 생성 및 변환
- `ST_MakePoint(x, y, [z], [m])`: 좌표 값으로 Point 객체 생성
- `ST_GeomFromText(text, [srid])`: WKT 문자열을 지오메트리로 변환
- `ST_SetSRID(geom, srid)`: 지오메트리의 SRID 메타데이터 설정
- `ST_Transform(geom, srid)`: 지오메트리를 다른 좌표계로 투영 변환
- `ST_AsText(geom)`: 지오메트리를 WKT 텍스트로 출력
- `ST_AsGeoJSON(geom)`: 지오메트리를 GeoJSON 포맷으로 출력

---

## 지오메트리 측정 및 속성
- `ST_Area(geom)`: 폴리곤의 면적 계산
- `ST_Length(geom)`: 선의 연장 길이 계산
- `ST_Distance(geom1, geom2)`: 두 지오메트리 간의 최단 유클리드 거리
- `ST_Centroid(geom)`: 기하학적 무게중심점 반환
- `ST_SRID(geom)`: 지오메트리의 SRID 반환
- `ST_X(point)`, `ST_Y(point)`: Point의 X, Y 좌표 추출

---

## 공간 관계 및 조건 판별
- `ST_Intersects(geom1, geom2)`: 두 객체가 공간을 공유하는지 여부
- `ST_Disjoint(geom1, geom2)`: 두 객체가 서로 전혀 닿지 않는지 여부
- `ST_Contains(geom1, geom2)`: geom1이 geom2를 완전히 포함하는지 여부
- `ST_Within(geom1, geom2)`: geom1이 geom2 내부에 속하는지 여부
- `ST_Touches(geom1, geom2)`: 두 객체의 경계만 접하는지 여부
- `ST_Crosses(geom1, geom2)`: 선과 선/면이 서로 가로지르는지 여부
- `ST_DWithin(geom1, geom2, distance)`: 두 객체 간의 거리가 기준치 이내인지 여부 (인덱스 활용)

---

## 지오메트리 연산 및 가공
- `ST_Buffer(geom, radius)`: 지정 반경의 완충 구역(버퍼) 생성
- `ST_Intersection(geom1, geom2)`: 두 객체의 공통 교집합 영역 생성
- `ST_Union(geom1, [geom2])`: 여러 지오메트리의 결합/합집합 생성
- `ST_Difference(geom1, geom2)`: geom1에서 geom2를 제외한 차집합 생성
- `ST_MakeValid(geom)`: 유효하지 않은 지오메트리를 올바른 형태로 복구
- `ST_SimplifyPreserveTopology(geom, tolerance)`: 위상을 유지하며 정점 단순화
- `ST_VoronoiPolygons(geom)`: 점 집합 기반 보로노이 폴리곤 생성

---

| [⬅️ 40. 고급 지오메트리 생성 (Advanced Geometry Constructions)](40_advanced_geometry_construction.md) | [🏠 워크숍 목차](README.md) | [42. 부록 B: 용어 사전 (Appendix B: Glossary) ➡️](42_glossary.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](40_advanced_geometry_construction.md) · [목차](00_index.md) · [다음 →](42_glossary.md)