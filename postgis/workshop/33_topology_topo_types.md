# 33. 토폴로지와 지오메트리 표현 (Topology and Geometry Representation)

토폴로지 모델에 저장된 노드, 에지, 면은 표준 `geometry` 객체로 언제든지 상호 변환할 수 있습니다.

---

## 1. TopoGeometry 데이터 타입

`TopoGeometry`는 일반 테이블의 컬럼으로 정의되며, 토폴로지 스키마 내의 에지나 페이스들의 집합을 가리키는 포인터 역할을 합니다.

```sql
-- 토폴로지 컬럼 추가
SELECT AddTopoGeometryColumn(
  'nyc_topo', 'public', 'my_parcels', 'topogeom', 'POLYGON'
);
```

---

## 2. TopoGeometry를 일반 Geometry로 변환

- `topogeom::geometry`: TopoGeometry 객체를 표준 OGC Geometry로 즉시 캐스팅 변환
- `ST_GetFaceGeometry('topo_name', face_id)`: 특정 Face ID에 해당하는 폴리곤 지오메트리 생성

---

| [⬅️ 32. 토폴로지 기본 타입 (Topology Basic Types)](32_topology_base_types.md) | [🏠 워크숍 목차](README.md) | [34. 트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers) ➡️](34_history_tracking.md) |
| :--- | :---: | ---: |
