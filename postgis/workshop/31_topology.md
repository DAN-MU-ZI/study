# 31. 토폴로지 (Topology)

일반적인 벡터 지오메트리에서는 인접한 두 폴리곤의 공유 경계선이 각각 독립된 좌표로 중복 저장됩니다. 이로 인해 한쪽을 수정했을 때 틈(Gap)이나 겹침(Overlap)이 발생하는 등의 데이터 불일치 문제가 생길 수 있습니다.

**PostGIS Topology** (`postgis_topology` 확장)는 공유 경계와 노드를 단 한 번만 저장하고, 객체 간의 연결 관계(위상 관계)를 유지하는 토폴로지 데이터 모델을 제공합니다.

![토폴로지 모델](topology/topology_main_concept.png)

---

## 1. 토폴로지 확장 활성화

```sql
CREATE EXTENSION postgis_topology;
```

---

## 2. 토폴로지의 핵심 장점

- **데이터 무결성(Integrity)**: 공유 경계선을 수정하면 인접한 모든 면(Face)이 자동으로 함께 업데이트되어 틈이나 중복이 발생하지 않습니다.
- **용량 절감**: 중복된 정점과 선분을 공유하므로 저장 공간이 절약됩니다.
- **네트워크 및 구획 분석**: 도로망, 행정구역 경계, 지적도(Cadastre) 관리에 최적입니다.

---

| [⬅️ 30. 래스터 (Rasters)](30_rasters.md) | [🏠 워크숍 목차](README.md) | [32. 토폴로지 기본 타입 (Topology Basic Types) ➡️](32_topology_base_types.md) |
| :--- | :---: | ---: |
