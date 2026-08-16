# 9.3. 토폴로지 데이터 타입 (Topology Types)

PostGIS Topology 모듈에서 사용하는 핵심 복합 데이터 타입(Composite Types)에 대한 공식 레퍼런스입니다.

---

## 목차
- [TopoGeometry](#topogeometry)
- [getfaceedges_returntype](#getfaceedges_returntype)
- [validatetopology_returntype](#validatetopology_returntype)

---

## TopoGeometry

**`TopoGeometry`** — 토폴로지 스키마 내의 노드, 에지, 면들의 위상학적 집합을 나타내는 복합 공간 데이터 타입입니다.

### 설명 (Description)
`TopoGeometry`는 일반 사용자 테이블의 컬럼 타입으로 지정되며, 특정 토폴로지 스키마에 정의된 기하학적 요소들의 관계를 가리킵니다.

### 내부 복합 구조 (Composite Attributes)
`TopoGeometry`는 내부적으로 다음과 같은 4개의 필드로 구성됩니다:
- `topology_id` (`integer`): 속해 있는 토폴로지의 고유 식별자 (`topology.topology.id`)
- `layer_id` (`integer`): 속해 있는 토폴로지 레이어의 고유 식별자 (`topology.layer.layer_id`)
- `id` (`integer`): 해당 레이어 내에서 이 TopoGeometry 객체의 고유 ID (`topogeo_id`)
- `type` (`integer`): 기하학적 유형 코드
  - `1`: **[Multi]Point** (점 / 노드 집합)
  - `2`: **[Multi]LineString** (선 / 에지 집합)
  - `3`: **[Multi]Polygon** (면 / 페이스 집합)
  - `4`: **GeometryCollection** (혼합 컬렉션)

---

### SQL 사용 예시

#### 1. TopoGeometry 컬럼 추가
```sql
SELECT topology.AddTopoGeometryColumn(
  'topo_schema_name', 'public', 'my_parcels', 'topogeom', 'POLYGON'
);
```

#### 2. TopoGeometry를 표준 OGC Geometry로 변환
```sql
SELECT topogeom::geometry AS standard_geom
FROM my_parcels;
```

---

## getfaceedges_returntype

**`getfaceedges_returntype`** — `GetFaceEdges` 함수가 반환하는 복합 데이터 타입으로, 특정 Face를 둘러싸고 있는 에지(Edge)의 일련번호와 에지 식별자 정보를 담고 있습니다.

### 구성 필드
- `sequence` (`integer`): 해당 Face의 둘레를 따라 도는 에지의 순번 (1부터 시작)
- `edge` (`integer`): 에지의 고유 식별자 (Edge ID).
  - 양수(`+`): 에지의 원래 정의 방향(시작 노드 -> 끝 노드)과 일치
  - 음수(`-`): 에지의 역방향(끝 노드 -> 시작 노드)으로 Face 경계를 구성

---

## validatetopology_returntype

**`validatetopology_returntype`** — `ValidateTopology` 함수가 토폴로지 무결성을 검증할 때 감지된 오류 내용을 보고하기 위해 반환하는 복합 데이터 타입입니다.

### 구성 필드
- `error` (`varchar`): 감지된 토폴로지 오류에 대한 설명 (예: `edge crosses node`, `face without outer ring` 등)
- `id1` (`integer`): 오류와 관련된 첫 번째 토폴로지 객체의 식별자 (예: 에지 ID)
- `id2` (`integer`): 오류와 관련된 두 번째 토폴로지 객체의 식별자 (필요 시)

---

| [⬅️ 9.4. 토폴로지 도메인 (Topology Domains)](01_topology_domains.md) | [🏠 토폴로지 목차](README.md) | [9.2. 토폴로지 기본 테이블 (Topology Primitive Tables) ➡️](03_topology_primitive_tables.md) |
| :--- | :---: | ---: |
