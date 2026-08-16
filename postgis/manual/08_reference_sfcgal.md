# 제 8 장. SFCGAL 3D 함수 레퍼런스 (SFCGAL Functions Reference)

**SFCGAL** 라이브러리를 기반으로 한 3차원 입체 기하, 솔리드(Solid), TIN, 다면체 연산 확장 모듈입니다.

---

## 주요 함수 목록
- `ST_3DIntersection(geom1, geom2)`: 3차원 공간 교집합 연산
- `ST_3DDifference(geom1, geom2)`: 3차원 공간 차집합 연산
- `ST_3DUnion(geom1, geom2)`: 3차원 솔리드 합집합 연산
- `ST_Volume(geom)`: 3차원 입체 솔리드의 체적(부피) 계산
- `ST_MakeSolid(geom)`: 닫힌 다면체 표면을 3D 솔리드 객체로 변환
- `ST_IsSolid(geom)`: 객체가 유효한 솔리드인지 검증
- `ST_Extrude(geom, dx, dy, dz)`: 2D 폴리곤을 Z축으로 돌출시켜 3D 입체 생성

---

| [⬅️ 제 7 장. 벡터 함수 레퍼런스 (Vector Functions Reference)](07_reference_vector.md) | [🏠 매뉴얼 목차](README.md) | [제 9 장. 토폴로지 (Topology) ➡️](09_topology.md) |
| :--- | :---: | ---: |
