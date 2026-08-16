# 11. 공간 관계 (Spatial Relationships)

공간 관계 함수는 두 지오메트리 간의 위치 관계를 평가하여 참(`true`) 또는 거짓(`false`)을 반환하는 불리언(Boolean) 함수들입니다.

![공간 관계 다이어그램](spatial_relationships/st_intersects.png)

---

## 주요 공간 관계 함수

### 1. `ST_Intersects` (교차)
두 지오메트리가 어떤 방식으로든 공간을 공유하거나 닿아있으면 `true`를 반환합니다.
- `ST_Disjoint`는 `ST_Intersects`의 완전한 반대입니다 (서로 전혀 닿지 않음).

```sql
-- Atlantic Commons 도로와 교차하는 인구조사 블록 찾기
SELECT c.blkid, c.popn_total
FROM nyc_census_blocks c, nyc_streets s
WHERE s.name = 'Atlantic Commons'
  AND ST_Intersects(c.geom, s.geom);
```

---

### 2. `ST_Contains` 및 `ST_Within` (포함 관계)
- `ST_Contains(A, B)`: A 지오메트리가 B 지오메트리를 완전히 포함하면 `true`
- `ST_Within(A, B)`: A 지오메트리가 B 지오메트리 내부에 완전히 속하면 `true`
- 즉, `ST_Contains(A, B) = ST_Within(B, A)` 입니다.

```sql
-- 'Chinatown' 이웃 지역 안에 위치하는 모든 지하철역 조회
SELECT s.name
FROM nyc_subway_stations s, nyc_neighborhoods n
WHERE n.name = 'Chinatown'
  AND ST_Within(s.geom, n.geom);
```

---

### 3. `ST_Touches` (접함)
두 지오메트리의 경계(Boundary)만 서로 닿아 있고, 내부(Interior)는 전혀 겹치지 않는 경우 `true`를 반환합니다. (예: 인접한 두 행정 구역의 국경선)

---

### 4. `ST_Crosses` (가로지름)
선과 면, 또는 두 선이 교차하되 내부를 관통하는 경우 `true`를 반환합니다. (예: 도로가 강을 가로지르는 경우)

---

### 5. `ST_DWithin` (반경 거리 내 검색)
두 지오메트리 사이의 최단 거리가 지정한 거리 `R` 이내인지 여부를 빠르게 판별합니다.

> [!TIP]
> `ST_Distance(A, B) < R` 대신 반드시 `ST_DWithin(A, B, R)`을 사용하세요!  
> `ST_DWithin`은 공간 인덱스(Bounding Box)를 사용하여 수백 배 이상 빠르게 연산됩니다.

```sql
-- 'Wall St' 지하철역에서 500미터 이내에 있는 모든 도로 조회
SELECT DISTINCT str.name
FROM nyc_streets str, nyc_subway_stations sub
WHERE sub.name = 'Wall St'
  AND ST_DWithin(str.geom, sub.geom, 500);
```

---

| [⬅️ 10. 지오메트리 실습 (Geometry Exercises)](10_geometries_exercises.md) | [🏠 워크숍 목차](README.md) | [12. 공간 관계 실습 (Spatial Relationships Exercises) ➡️](12_spatial_relationships_exercises.md) |
| :--- | :---: | ---: |
