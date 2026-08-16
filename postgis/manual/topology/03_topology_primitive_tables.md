# 9.2. 토폴로지 기본 테이블 (Topology Primitive Tables)

각 토폴로지 스키마가 생성되면 내부적으로 위상 기하학적 원시 요소를 저장하기 위한 4개의 핵심 테이블이 자동으로 구성됩니다.

---

## 1. `node` 테이블
토폴로지 네트워크의 모든 정점(Node) 정보를 저장합니다.

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `node_id` | `serial PRIMARY KEY` | 노드의 고유 식별자 |
| `geom` | `geometry(Point, SRID)` | 노드의 2D 점 좌표 지오메트리 |
| `containing_face` | `integer` | 고립된 노드(Isolated Node)인 경우 해당 노드가 위치한 Face ID |

---

## 2. `edge_data` 테이블
노드들을 연결하는 선분(Edge)의 연결성 및 좌우 면(Face) 정보를 저장합니다.

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `edge_id` | `serial PRIMARY KEY` | 에지의 고유 식별자 |
| `start_node` | `integer REFERENCES node` | 에지의 시작 노드 ID |
| `end_node` | `integer REFERENCES node` | 에지의 끝 노드 ID |
| `next_left_edge` | `integer` | 왼쪽 면을 기준으로 순환할 때 다음 에지 ID |
| `abs_next_left_edge`| `integer` | next_left_edge의 절대값 |
| `next_right_edge`| `integer` | 오른쪽 면을 기준으로 순환할 때 다음 에지 ID |
| `abs_next_right_edge`| `integer` | next_right_edge의 절대값 |
| `left_face` | `integer REFERENCES face` | 에지 진행 방향 기준 왼쪽 Face ID |
| `right_face` | `integer REFERENCES face` | 에지 진행 방향 기준 오른쪽 Face ID |
| `geom` | `geometry(LineString, SRID)` | 에지의 실제 선형 기하학적 형태 |

---

## 3. `face` 테이블
에지들로 둘러싸인 닫힌 영역(Face) 정보를 저장합니다.

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `face_id` | `serial PRIMARY KEY` | 면(Face)의 고유 식별자 (`0`은 외부 무한 면인 Universal Face) |
| `mbr` | `geometry(Polygon, SRID)` | 해당 Face의 최소 경계 사각형(MBR) |

---

## 4. `relation` 테이블
사용자 정의 `TopoGeometry` 객체와 기본 원시 요소(`node`, `edge`, `face`) 또는 하위 레이어 간의 소속 매핑 관계를 저장합니다.

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `topogeo_id` | `integer` | 사용자 TopoGeometry 객체의 ID |
| `layer_id` | `integer` | 대상 레이어 ID |
| `element_id` | `integer` | 소속된 기본 요소 ID 또는 자식 TopoGeometry ID |
| `element_type` | `integer` | 요소 유형 (1: Node, 2: Edge, 3: Face, 기타: Layer) |

---

| [⬅️ 9.3. 토폴로지 데이터 타입 (Topology Types)](02_topology_types.md) | [🏠 토폴로지 목차](README.md) | [9.5 ~ 9.16. 토폴로지 함수 레퍼런스 (Topology Functions) ➡️](04_topology_functions.md) |
| :--- | :---: | ---: |
