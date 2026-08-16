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
