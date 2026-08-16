# 13. 공간 조인 (Spatial Joins)

공간 조인(Spatial Join)은 두 테이블을 연결할 때 일반적인 외래키(ID) 대신 **공간적 관계(`ST_Intersects`, `ST_Contains`, `ST_DWithin` 등)**를 조인 조건(`ON` 절)으로 사용하는 기법입니다.

공간 데이터베이스의 가장 강력한 기능 중 하나입니다.

![공간 조인 개념도](screenshots/centroid_neighborhood.png)

---

## 1. 기본 공간 조인 문법

표준 SQL `JOIN ... ON` 문법에 공간 관계 함수를 직접 작성합니다:

```sql
-- 각 지하철역이 어떤 이웃 지역(neighborhood)에 속해 있는지 조인
SELECT
  s.name AS station_name,
  n.name AS neighborhood_name,
  n.boroname
FROM nyc_subway_stations s
JOIN nyc_neighborhoods n
  ON ST_Contains(n.geom, s.geom);
```

---

## 2. 공간 조인과 집계 (Spatial Aggregation)

공간 조인과 `GROUP BY`를 결합하면 구역별 통계를 손쉽게 도출할 수 있습니다:

### 이웃 지역별 지하철역 개수 집계:
```sql
SELECT
  n.name AS neighborhood_name,
  COUNT(s.name) AS num_stations
FROM nyc_neighborhoods n
JOIN nyc_subway_stations s
  ON ST_Contains(n.geom, s.geom)
GROUP BY n.name
ORDER BY num_stations DESC;
```

---

## 3. 거리 기반 조인 (Distance Joins)

`ST_DWithin`을 사용한 반경 내 객체 조인:

```sql
-- 각 강력사건 발생 위치로부터 100미터 이내에 있는 지하철역 찾기
SELECT
  h.incident_date,
  h.weapon,
  s.name AS station_name
FROM nyc_homicides h
JOIN nyc_subway_stations s
  ON ST_DWithin(h.geom, s.geom, 100)
LIMIT 10;
```

---

| [⬅️ 12. 공간 관계 실습 (Spatial Relationships Exercises)](12_spatial_relationships_exercises.md) | [🏠 워크숍 목차](README.md) | [14. 공간 조인 실습 (Spatial Joins Exercises) ➡️](14_joins_exercises.md) |
| :--- | :---: | ---: |
