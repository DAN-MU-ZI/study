# 32. 토폴로지 기본 타입 (Topology Basic Types)

PostGIS Topology는 위상 구조를 표현하기 위해 3가지 기본 프리미티브 요소를 사용합니다.

---

## 1. 토폴로지 3대 요소

1. **노드 (Node, Point)**: 선분의 시작점, 끝점, 또는 고립된 점
2. **에지 (Edge, Line)**: 두 노드를 연결하는 선분 (왼쪽 면과 오른쪽 면의 ID 정보를 포함)
3. **면 (Face, Polygon)**: 에지들로 둘러싸인 닫힌 영역

![노드, 에지, 페이스 다이어그램](topology/basic_types.png)

---

## 2. 토폴로지 생성 및 관리 함수

- `CreateTopology('topo_name', srid, tolerance)`: 새로운 토폴로지 스키마 생성
- `TopoGeo_AddPoint('topo_name', point, tolerance)`: 토폴로지에 노드 추가
- `TopoGeo_AddLinestring('topo_name', line, tolerance)`: 토폴로지에 에지 추가
- `TopoGeo_AddPolygon('topo_name', poly, tolerance)`: 토폴로지에 면 추가

```sql
-- 오차 허용치(Tolerance) 1미터로 'nyc_topo' 토폴로지 생성
SELECT CreateTopology('nyc_topo', 26918, 1.0);
```

---

| [⬅️ 31. 토폴로지 (Topology)](31_topology.md) | [🏠 워크숍 목차](README.md) | [33. 토폴로지와 지오메트리 표현 (Topology and Geometry Representation) ➡️](33_topology_topo_types.md) |
| :--- | :---: | ---: |
