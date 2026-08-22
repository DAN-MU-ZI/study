# 33. 토폴로지와 지오메트리 표현 (Topology and Geometry Representation)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/topology_topo_types.html>](https://postgis.net/workshops/postgis-intro/topology_topo_types.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

토폴로지 기반의 공간 데이터 모델에서는 지오메트리를 단순 좌표 배열로 저장하지 않고, 토폴로지의 기본 원시 요소(Primitives) 또는 하위 레이어의 요소들을 참조하는 **`TopoGeometry` 복합 타입**으로 관리합니다.

---

## 1. TopoGeometry와 계층적 데이터 모델

![일반 geometry 테이블을 TopoGeometry 참조 테이블로 변환하는 구조](topology/geomtable2topotable.png)

일반 공간 테이블이 좌표 형태의 `geometry` 컬럼을 갖는 것과 달리, 토폴로지 테이블은 위상 관계를 가리키는 `TopoGeometry` 컬럼을 사용합니다.

![TopoGeometry가 relation 테이블을 통해 원시 요소를 참조하는 핵심 구조](topology/topology_main_concept.png)

- **레이어 (Layer)**: 특정 테이블의 `TopoGeometry` 컬럼과 1:1로 매핑되는 상위 컨테이너입니다.
- **토포지오메트리 (TopoGeometry)**: 테이블의 각 행(Feature)을 구성하는 객체 식별자입니다.
- **토포엘리먼트 (TopoElement)**: TopoGeometry가 참조하는 구체적인 위상 원시 요소(노드, 에지, 페이스) 또는 하위 레이어의 요소 번호입니다.

---

## 2. 피처 타입 (Feature Types)

토폴로지 레이어는 다음 4가지 피처 타입 중 하나로 정의됩니다.

1. **`1` (Point / Node)**: 노드로 구성된 포인트 객체
2. **`2` (Line / Edge)**: 에지로 구성된 선형 객체
3. **`3` (Polygon / Face)**: 페이스로 구성된 다각형 면 객체
4. **`4` (Collection)**: 여러 원시 요소의 혼합 컬렉션

---

## 3. 계층적 레이어 구조 (Layer Hierarchy)

![하위 TopoGeometry를 상위 레이어가 참조하는 계층 구조](topology/hierarchy.png)

- **기본 레이어 (Level 0, Base Layer)**: 원시 요소(노드, 에지, 페이스)들을 직접 참조하여 구성되는 레이어 (예: 동네 레이어 `nyc_neighborhoods_t`)
- **상위 계층 레이어 (Level 1+, Hierarchical Layer)**: 하위 레이어의 TopoGeometry들을 다시 그룹화하여 구성되는 레이어 (예: 동네들을 모아 구성한 자치구 레이어 `nyc_boros_t`)

상위 레이어는 하위 레이어의 `layer_id`를 `child_id`로 기록하여 계층 관계를 유지합니다.

---

## 4. relation 관계 테이블의 구조

사용자 정의 토폴로지 스키마 내의 `relation` 테이블은 TopoGeometry와 이를 구성하는 TopoElement 간의 매핑을 저장합니다.

![TopoGeometry를 구성하는 노드 에지 페이스 요소 관계](topology/components.png)

```text
relation 테이블 컬럼 구성:
- topogeom_id : TopoGeometry의 고유 번호
- layer_id     : 소속 레이어 ID
- element_id   : 참조하는 원시 요소(또는 하위 TopoGeometry)의 ID
- element_type : 참조하는 요소의 타입(1: Node, 2: Edge, 3: Face) 또는 하위 layer_id
```

 하나의 TopoGeometry가 여러 개의 에지나 페이스로 구성된 경우, 동일한 `(topogeom_id, layer_id)`에 대해 여러 행의 `(element_id, element_type)` 레코드가 저장됩니다.

---

## 5. TopoElement 해석 알고리즘

PostGIS는 쿼리 실행 시 `TopoGeometry`를 다음과 같은 로직으로 실제 지오메트리로 복원합니다.

![relation 행에서 최종 TopoElement와 실제 형상을 복원하는 과정](topology/read_topoelement.png)

1. `topology.layer` 테이블에서 해당 레이어의 **`child_id`**를 확인합니다.
2. **`child_id`가 `NULL`인 경우 (기본 레이어)**:
   - `element_id`: 위상 원시 요소 번호 (`node_id`, `edge_id`, `face_id`)
   - `element_type`: 원시 요소 타입 (1: 노드, 2: 에지, 3: 페이스)
   - 해당 번호의 `node`, `edge_data`, `face` 테이블에서 실제 형상을 즉시 조회하여 결합합니다.
3. **`child_id`가 `NULL`이 아닌 경우 (계층적 상위 레이어)**:
   - `element_id`: 하위 레이어에 속한 자식 `TopoGeometry`의 `topogeom_id`
   - `element_type`: 하위 레이어의 `layer_id`
   - `relation` 테이블을 재귀적으로 조회하여 최종 원시 요소(Primitives)까지 추적한 후 결합합니다.


---

[← 이전](32_topology_base_types.md) · [목차](00_index.md) · [다음 →](34_history_tracking.md)
