# 12. 공간 관계 실습 (Spatial Relationships Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/spatial_relationships_exercises.html>](https://postgis.net/workshops/postgis-intro/spatial_relationships_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

앞서 학습한 공간 관계 함수들을 활용하여 다음 실습 문제를 직접 해결해 보세요.

### 실습 참조 함수 요약
- `ST_Contains(geometry A, geometry B)`: A가 B를 완전히 포함하는지 검사
- `ST_Crosses(geometry A, geometry B)`: A와 B가 서로 교차하는지 검사
- `ST_Disjoint(geometry A, geometry B)`: A와 B가 완전히 떨어져 있는지 검사
- `ST_Distance(geometry A, geometry B)`: A와 B 사이의 최단 거리 계산
- `ST_DWithin(geometry A, geometry B, radius)`: A와 B가 지정된 반경 이내에 있는지 고속 검사
- `ST_Equals(geometry A, geometry B)`: A와 B가 공간적으로 동일한지 검사
- `ST_Intersects(geometry A, geometry B)`: A와 B가 공간을 공유(교차/접촉/포함)하는지 검사
- `ST_Overlaps(geometry A, geometry B)`: 동일 차원의 A와 B가 일부 겹치는지 검사
- `ST_Touches(geometry A, geometry B)`: A와 B의 경계가 서로 맞닿아 있는지 검사
- `ST_Within(geometry A, geometry B)`: A가 B 내부에 완전히 포함되어 있는지 검사

### 실습 대상 테이블
- `nyc_census_blocks`: `blkid`, `popn_total`, `boroname`, `geom`
- `nyc_streets`: `gid`, `name`, `type`, `geom`
- `nyc_subway_stations`: `name`, `geom`
- `nyc_neighborhoods`: `name`, `boroname`, `geom`

---

## 연습 문제 및 정답

### 1. 'Atlantic Commons'라는 도로의 지오메트리 좌표값(WKT)은 어떻게 됩니까?

```sql
SELECT ST_AsText(geom)
FROM nyc_streets
WHERE name = 'Atlantic Commons';
```

```text
MULTILINESTRING((586781.701577724 4504202.15314339,586863.51964484 4504215.9881701))
```

---

### 2. 'Atlantic Commons' 거리는 어느 근린지역(Neighborhood)과 자치구(Borough)에 위치해 있습니까?

```sql
SELECT n.name AS neighborhood, n.boroname AS borough
FROM nyc_neighborhoods AS n
JOIN nyc_streets AS s
  ON ST_Intersects(n.geom, s.geom)
WHERE s.name = 'Atlantic Commons';
```

```text
neighborhood |  borough
-------------+----------
Fort Greene  | Brooklyn
```

---

### 3. 'Atlantic Commons' 도로와 직접 연결(접촉)되는 인접 도로들은 무엇입니까?

```sql
SELECT s.name
FROM nyc_streets AS s
JOIN nyc_streets AS ac
  ON ST_DWithin(s.geom, ac.geom, 0.1)
WHERE ac.name = 'Atlantic Commons'
  AND s.gid <> ac.gid;
```

```text
     name
---------------
 S Oxford St
 Cumberland St
```

![이미지](spatial_relationships/atlantic_commons.jpg)

> [!NOTE]
> `s.gid <> ac.gid` 조건은 자기 자신과의 비교를 제외하기 위해 추가되었습니다. `ST_DWithin`에 아주 작은 오차 허용치(0.1m)를 부여하면 부동소수점 정밀도 차이로 인해 `ST_Touches`가 놓칠 수 있는 연결 도로를 안정적으로 검색할 수 있습니다.

---

### 4. 'Atlantic Commons' 도로로부터 50m 이내에 거주하는 인구수는 대략 몇 명입니까?

```sql
SELECT sum(popn_total) AS population
FROM nyc_census_blocks
WHERE ST_DWithin(
  geom,
  ST_GeomFromText('LINESTRING(586782 4504202, 586864 4504216)', 26918),
  50
);
```

```text
1438
```


---

[← 이전](11_spatial_relationships.md) · [목차](00_index.md) · [다음 →](13_joins.md)
