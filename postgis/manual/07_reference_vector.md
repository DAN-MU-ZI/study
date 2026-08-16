# 제 7 장. 벡터 함수 레퍼런스 (Vector Functions Reference)

PostGIS 핵심 벡터 지오메트리 함수 상세 레퍼런스 요약입니다.

---

## 1. 생성자 (Constructors)
- `ST_MakePoint(x, y, [z], [m])`: 수치 좌표로 Point 생성
- `ST_PointFromText(wkt, [srid])`: WKT 문자열로 Point 생성
- `ST_GeomFromText(wkt, [srid])`: WKT 문자열로 Geometry 생성
- `ST_GeomFromGeoJSON(geojson)`: GeoJSON 문자열을 Geometry로 변환
- `ST_MakeEnvelope(xmin, ymin, xmax, ymax, [srid])`: BBOX 사각형 폴리곤 생성

---

## 2. 변환 및 출력 (Accessors & Outputs)
- `ST_AsText(geom)`: WKT 텍스트 반환
- `ST_AsGeoJSON(geom)`: GeoJSON 문자열 반환
- `ST_AsBinary(geom)`: WKB 바이너리 반환
- `ST_AsMVT(geom, ...)`: Mapbox Vector Tile (MVT) 바이너리 생성
- `ST_Transform(geom, target_srid)`: 타 좌표계로 투영 변환

---

## 3. 공간 관계 판별 (Spatial Relationships)
- `ST_Intersects(geom1, geom2)`: 두 지오메트리 교차 여부 판별
- `ST_Contains(geom1, geom2)`: geom1이 geom2를 포함하는지 판별
- `ST_Within(geom1, geom2)`: geom1이 geom2 내부에 속하는지 판별
- `ST_DWithin(geom1, geom2, distance)`: 기준 거리 이내 위치 여부 판별 (인덱스 가속)
- `ST_Disjoint(geom1, geom2)`: 완전히 분리되어 있는지 판별

---

## 4. 공간 가공 및 연산 (Processing & Operations)
- `ST_Buffer(geom, radius_of_buffer)`: 완충 구역(버퍼) 생성
- `ST_Intersection(geom1, geom2)`: 교집합 폴리곤 생성
- `ST_Union(geom1, [geom2])`: 합집합(병합) 생성
- `ST_Difference(geom1, geom2)`: 차집합 생성
- `ST_Centroid(geom)`: 기하학적 무게중심점 계산
- `ST_SimplifyPreserveTopology(geom, tolerance)`: 위상 보존 단순화
- `ST_MakeValid(geom)`: 비정상 폴리곤을 OGC 표준 유효 지오메트리로 자동 복구

---

| [⬅️ 제 6 장. 성능 팁 및 튜닝 (Performance Tips)](06_performance_tips.md) | [🏠 매뉴얼 목차](README.md) | [제 8 장. SFCGAL 3D 함수 레퍼런스 (SFCGAL Functions Reference) ➡️](08_reference_sfcgal.md) |
| :--- | :---: | ---: |
