# 9. 토폴로지 (Topology) 상세 레퍼런스

PostGIS 공식 개발자 매뉴얼 9장 Topology 모듈의 완전한 한국어 상세 문서입니다.

---

## 📚 토폴로지 서브 문서 목록

1. **[9.4. 토폴로지 도메인 (`TopoElement`, `TopoElementArray`)](01_topology_domains.md)**
   - `TopoElement` (`integer[2]`) 및 `TopoElementArray` (`integer[][2]`) 도메인 명세
   - 제약 조건 위반 오류 설명 및 `TopoElementArray_Agg` 집계 예제
2. **[9.3. 토폴로지 데이터 타입 (`TopoGeometry` 등)](02_topology_types.md)**
   - `TopoGeometry` 복합 타입 내부 4대 필드(`topology_id`, `layer_id`, `id`, `type`)
   - `getfaceedges_returntype`, `validatetopology_returntype`
3. **[9.2. 토폴로지 기본 테이블 (`node`, `edge_data`, `face`, `relation`)](03_topology_primitive_tables.md)**
   - 4대 원시 테이블 스키마 및 외래키 참조 관계
4. **[9.5 ~ 9.16. 토폴로지 함수 레퍼런스](04_topology_functions.md)**
   - 생성, 편집, 접근, 처리, 공간 관계 및 I/O 함수 일람
