# 제 9 장. 토폴로지 (Topology)

PostGIS Topology 확장은 노드(Node), 에지(Edge), 면(Face) 간의 위상학적 연결 관계를 저장하여 경계 불일치를 방지하고 무결성을 유지하는 데이터 모델을 제공합니다.

---

## 📚 토폴로지 서브 레퍼런스 목록

1. **[9.4. 토폴로지 도메인 (Topology Domains)](topology/01_topology_domains.md)**
   - `TopoElement`: 정수 2개 배열(`integer[2]`)로 토폴로지 기본 요소(ID, Type)를 식별
   - `TopoElementArray`: `TopoElement` 객체들의 2차원 배열(`integer[][2]`)
   - `dimensions` 체크 제약 조건 및 집계 함수(`TopoElementArray_Agg`)
2. **[9.3. 토폴로지 데이터 타입 (Topology Types)](topology/02_topology_types.md)**
   - `TopoGeometry`: 토폴로지 객체의 소속 관계를 표현하는 4개 정수 복합 타입
   - `getfaceedges_returntype`, `validatetopology_returntype`
3. **[9.2. 토폴로지 기본 테이블 (Topology Primitive Tables)](topology/03_topology_primitive_tables.md)**
   - `node` (정점), `edge_data` (에지/연결성), `face` (면), `relation` (매핑 관계)
4. **[9.5 ~ 9.16. 토폴로지 관리/편집 함수](topology/04_topology_functions.md)**
   - `CreateTopology`, `DropTopology`, `ValidateTopology`
   - `ST_AddIsoNode`, `ST_AddEdgeModFace`, `ST_RemEdgeModFace`, `ST_ChangeEdgeGeom`
   - `GetNodeByPoint`, `GetEdgeByPoint`, `GetFaceByPoint`

---

| [⬅️ 제 8 장. SFCGAL 3D 함수 레퍼런스 (SFCGAL Functions Reference)](08_reference_sfcgal.md) | [🏠 매뉴얼 목차](README.md) | [제 10 장. 래스터 데이터 관리 및 분석 (Raster Data Management) ➡️](10_raster_data_management.md) |
| :--- | :---: | ---: |
