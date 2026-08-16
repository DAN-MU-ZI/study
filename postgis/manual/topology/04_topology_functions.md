# 9.5 ~ 9.16. 토폴로지 함수 레퍼런스 (Topology Functions)

PostGIS Topology 확장에서 제공하는 관리, 편집, 접근, 연산 함수 총람입니다.

---

## 1. 토폴로지 관리 함수 (Management Functions)
- `CreateTopology(topology_name, srid, tolerance)`: 새로운 토폴로지 스키마 생성
- `DropTopology(topology_name)`: 토폴로지 스키마 및 관련 메타데이터 완전 삭제
- `ValidateTopology(topology_name)`: 토폴로지 무결성 검증 (오류 목록 반환)
- `TopologySummary(topology_name)`: 노드, 에지, 면 수 및 통계 요약 보고서 반환
- `AddTopoGeometryColumn(toponame, schema, table, col, type)`: 사용자 테이블에 TopoGeometry 컬럼 등록
- `DropTopoGeometryColumn(schema, table, col)`: TopoGeometry 컬럼 등록 해제

---

## 2. 토폴로지 편집 및 프리미티브 추가 (Editing Functions)
- `ST_AddIsoNode(toponame, face_id, point_geom)`: 고립된 단독 노드 추가
- `ST_AddIsoEdge(toponame, start_node, end_node, line_geom)`: 고립된 에지 추가
- `ST_AddEdgeNewFaces(toponame, start_node, end_node, line_geom)`: 새 면을 생성하며 에지 추가
- `ST_AddEdgeModFace(toponame, start_node, end_node, line_geom)`: 기존 면을 분할하며 에지 추가
- `ST_RemEdgeModFace(toponame, edge_id)`: 에지를 제거하고 인접 면들을 하나로 병합
- `ST_ChangeEdgeGeom(toponame, edge_id, line_geom)`: 위상을 유지하며 에지의 기하 형태만 변경
- `ST_ModEdgeSplit(toponame, edge_id, point_geom)`: 에지 중간에 노드를 삽입하여 2개의 에지로 분할
- `ST_ModEdgeHeal(toponame, edge_id1, edge_id2)`: 공유 노드를 제거하고 두 에지를 하나로 결합

---

## 3. 토폴로지 접근자 (Accessors)
- `GetNodeByPoint(toponame, point, tolerance)`: 주어진 좌표 위치에 있는 노드 ID 조회
- `GetEdgeByPoint(toponame, point, tolerance)`: 주어진 좌표 위치를 지나는 에지 ID 조회
- `GetFaceByPoint(toponame, point, tolerance)`: 주어진 좌표를 포함하는 Face ID 조회
- `GetFaceEdges(toponame, face_id)`: 특정 Face를 둘러싸고 있는 에지 순번 및 ID 목록 반환
- `GetRingEdges(toponame, edge_id)`: 특정 에지가 속한 링의 에지 목록 반환

---

## 4. 토폴로지 공간 관계 및 변환
- `ST_GetFaceGeometry(toponame, face_id)`: Face ID를 표준 OGC Polygon 지오메트리로 생성
- `TopoGeo_AddLinestring(toponame, line_geom, tolerance)`: 선형 지오메트리를 입력받아 노드/에지/면을 자동 분할 생성
- `TopoGeo_AddPolygon(toponame, poly_geom, tolerance)`: 폴리곤 지오메트리를 입력받아 토폴로지 자동 생성

---

| [⬅️ 9.2. 토폴로지 기본 테이블 (Topology Primitive Tables)](03_topology_primitive_tables.md) | [🏠 토폴로지 목차](README.md) | |
| :--- | :---: | ---: |
