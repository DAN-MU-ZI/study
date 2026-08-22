# 11. 공간 관계 (Spatial Relationships)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/spatial_relationships.html>](https://postgis.net/workshops/postgis-intro/spatial_relationships.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지금까지는 단일 지오메트리의 속성을 측정(`ST_Area`, `ST_Length`)하거나 포맷을 직렬화(`ST_GeomFromText`, `ST_AsGeoJSON`)하는 단항 함수들을 주로 다루었습니다.

공간 데이터베이스가 진정한 위력을 발휘하는 순간은 지오메트리를 단순히 저장하는 것을 넘어 **지오메트리 간의 공간적 관계(Spatial Relationships)**를 비교하고 분석할 때입니다.

- "이 공원에서 반경 500m 이내에 있는 모든 자전거 대여소는 어디인가?"
- "이 지하철 노선과 교차하는 도로들은 무엇인가?"
- "이 특정 선거구 내에 완전히 포함되는 인구조사 블록은 무엇인가?"

이러한 현실 세계의 공간적 질문들은 두 개 이상의 지오메트리를 서로 비교하는 공간 관계 판별 함수를 통해 해결할 수 있습니다. OGC 표준은 공간 관계를 판별하기 위해 다음과 같은 공간 술어(Spatial Predicates) 함수들을 정의합니다.

---

## 1. ST_Equals

`ST_Equals(geometry A, geometry B)`는 두 지오메트리가 공간적으로 완전히 동일한 형태와 위치를 가지는지 검사합니다.

![두 지오메트리가 같은 공간을 차지하는 ST_Equals 관계](spatial_relationships/st_equals.png)

동일한 지오메트리 타입의 두 객체가 정확히 동일한 X, Y 좌표 구조를 가지는 경우 `TRUE`를 반환합니다.

'Broad St' 지하철역의 바이너리 지오메트리 값을 가져와 `ST_Equals`로 일치 여부를 확인해 보겠습니다.

```sql
SELECT name, geom
FROM nyc_subway_stations
WHERE name = 'Broad St';
```

```text
   name   |                      geom
----------+---------------------------------------------------
 Broad St | 0101000020266900000EEBD4CF27CF2141BC17D69516315141
```

```sql
SELECT name
FROM nyc_subway_stations
WHERE ST_Equals(
  geom,
  '0101000020266900000EEBD4CF27CF2141BC17D69516315141'
);
```

```text
Broad St
```

---

## 2. ST_Intersects, ST_Disjoint, ST_Crosses, ST_Overlaps

### ST_Intersects
`ST_Intersects(geometry A, geometry B)`는 두 지오메트리가 공간의 일부를 조금이라도 공유(경계가 맞닿거나 내부가 겹치는 경우)하면 `TRUE`를 반환합니다.

![두 지오메트리가 일부 공간을 공유하는 ST_Intersects 관계](spatial_relationships/st_intersects.png)

### ST_Disjoint
`ST_Disjoint(geometry A, geometry B)`는 `ST_Intersects`의 정반대입니다. 두 지오메트리가 서로 완전히 떨어져 있어 어떤 공간도 공유하지 않을 때 `TRUE`를 반환합니다.

![두 지오메트리가 완전히 분리된 ST_Disjoint 관계](spatial_relationships/st_disjoint.png)

> [!TIP]
> 실무에서는 `ST_Disjoint` 대신 `NOT ST_Intersects`를 사용하는 것이 훨씬 효율적입니다. `ST_Intersects`는 공간 인덱스(GiST)를 통해 고속으로 처리되지만, `ST_Disjoint`는 인덱스로 후보군을 좁히기 어렵기 때문입니다.

### ST_Crosses
`ST_Crosses(geometry A, geometry B)`는 두 지오메트리가 교차하여 생성된 교집합의 차원이 두 지오메트리 중 큰 차원보다 1 작고, 교집합이 두 객체의 내부에 위치할 때 `TRUE`를 반환합니다 (예: 선과 선이 십자로 교차하여 점이 생기는 경우, 선이 폴리곤 내부를 관통하는 경우).

![선이 다른 지오메트리의 내부를 가로지르는 ST_Crosses 관계](spatial_relationships/st_crosses.png)

### ST_Overlaps
`ST_Overlaps(geometry A, geometry B)`는 동일한 차원의 두 지오메트리(폴리곤과 폴리곤, 또는 선과 선)가 서로 겹치지만 어느 한쪽이 다른 쪽을 완전히 포함하지는 않을 때 `TRUE`를 반환합니다.

![같은 차원의 두 지오메트리가 일부 겹치는 ST_Overlaps 관계](spatial_relationships/st_overlaps.png)

#### 교차 분석 예시: Broad St 역이 위치한 근린지역 찾기

```sql
SELECT name, boroname
FROM nyc_neighborhoods
WHERE ST_Intersects(
  geom,
  ST_GeomFromText('POINT(583571 4506714)', 26918)
);
```

```text
        name        | boroname
--------------------+-----------
 Financial District | Manhattan
```

---

## 3. ST_Touches

`ST_Touches(geometry A, geometry B)`는 두 지오메트리의 경계(Boundary)는 서로 맞닿아 있지만 내부(Interior)는 전혀 겹치지 않을 때 `TRUE`를 반환합니다.

![내부는 겹치지 않고 경계만 맞닿는 ST_Touches 관계](spatial_relationships/st_touches.png)

---

## 4. ST_Within 및 ST_Contains

`ST_Within`과 `ST_Contains`는 한 지오메트리가 다른 지오메트리 내부에 완전히 포함되어 있는지를 검사합니다.

![한 지오메트리가 다른 지오메트리 내부에 포함된 ST_Within 관계](spatial_relationships/st_within.png)

- `ST_Within(geometry A, geometry B)`: 지오메트리 A가 지오메트리 B 내부에 완전히 들어있는 경우 `TRUE`를 반환합니다.
- `ST_Contains(geometry A, geometry B)`: 지오메트리 A가 지오메트리 B를 완전히 포함하는 경우 `TRUE`를 반환합니다 (`ST_Within`과 인자 순서가 반대).

---

## 5. ST_Distance 및 ST_DWithin

거리 기반 공간 질의는 GIS 분석에서 가장 빈번하게 사용됩니다.

### ST_Distance
`ST_Distance(geometry A, geometry B)`는 두 지오메트리 간의 **최단 직교 거리**를 계산하여 부동소수점 실수값으로 반환합니다 (투영 좌표계의 단위인 미터).

```sql
SELECT ST_Distance(
  ST_GeometryFromText('POINT(0 5)'),
  ST_GeometryFromText('LINESTRING(-2 2, 2 2)')
);
```

```text
3
```

### ST_DWithin (인덱스 가속 반경 검색)
"특정 객체로부터 반경 X미터 이내에 있는 모든 객체 찾기"와 같은 조건을 판별할 때 `ST_DWithin(geometry A, geometry B, distance)`을 사용합니다.

![두 지오메트리가 지정 반경 이내인지 검사하는 ST_DWithin 관계](spatial_relationships/st_dwithin.png)

> [!IMPORTANT]
> `ST_Distance(A, B) < distance` 대신 반드시 **`ST_DWithin(A, B, distance)`**을 사용해야 합니다. `ST_DWithin`은 공간 인덱스(GiST)를 통해 바운딩 박스를 자동으로 확장하여 검색 대상을 고속으로 필터링하므로 수천 배 이상 빠릅니다.

#### 예시: Broad St 지하철역 반경 10m 이내의 도로 검색

```sql
SELECT name
FROM nyc_streets
WHERE ST_DWithin(
  geom,
  ST_GeomFromText('POINT(583571 4506714)', 26918),
  10
);
```

```text
   name
-----------
 Wall St
 Broad St
 Nassau St
```

실제 지도를 확인해 보면 Broad St 지하철역은 Wall St, Broad St, Nassau St가 만나는 교차로에 위치해 있음을 알 수 있습니다.

![Broad St 지하철역과 주변 근린지역의 공간 관계](spatial_relationships/broad_st.jpg)

---

## 함수 목록 (Function List)

- [ST_Contains(geometry A, geometry B)](http://postgis.net/docs/ST_Contains.html): B의 어떤 점도 A의 외부에 있지 않고, B 내부의 점이 A 내부와 공유될 때 `TRUE`를 반환합니다 (A가 B를 완전히 포함).
- [ST_Crosses(geometry A, geometry B)](http://postgis.net/docs/ST_Crosses.html): 두 지오메트리의 내부가 교차하지만 한쪽이 다른 쪽을 포함하지 않는 경우 `TRUE`를 반환합니다.
- [ST_Disjoint(geometry A, geometry B)](http://postgis.net/docs/ST_Disjoint.html): 두 지오메트리가 어떤 공간도 공유하지 않고 완전히 분리되어 있을 때 `TRUE`를 반환합니다.
- [ST_Distance(geometry A, geometry B)](http://postgis.net/docs/ST_Distance.html): 두 지오메트리 사이의 최단 2차원 거리를 투영 좌표계 단위로 반환합니다.
- [ST_DWithin(geometry A, geometry B, radius)](http://postgis.net/docs/ST_DWithin.html): 두 지오메트리 간의 최단 거리가 지정된 반경(radius) 이내이면 공간 인덱스를 활용하여 빠르게 `TRUE`를 반환합니다.
- [ST_Equals(geometry A, geometry B)](http://postgis.net/docs/ST_Equals.html): 두 지오메트리가 공간적으로 동일한 구조와 위치를 가질 때 `TRUE`를 반환합니다.
- [ST_Intersects(geometry A, geometry B)](http://postgis.net/docs/ST_Intersects.html): 두 지오메트리가 공간의 일부를 조금이라도 공유하면 `TRUE`, 완전히 분리되어 있으면 `FALSE`를 반환합니다.
- [ST_Overlaps(geometry A, geometry B)](http://postgis.net/docs/ST_Overlaps.html): 동일한 차원의 두 지오메트리가 일부 겹치지만 서로를 완전히 포함하지 않는 경우 `TRUE`를 반환합니다.
- [ST_Touches(geometry A, geometry B)](http://postgis.net/docs/ST_Touches.html): 두 지오메트리의 경계가 서로 접촉하고 내부는 교차하지 않을 때 `TRUE`를 반환합니다.
- [ST_Within(geometry A, geometry B)](http://postgis.net/docs/ST_Within.html): 지오메트리 A가 지오메트리 B 내부에 완전히 포함되어 있을 때 `TRUE`를 반환합니다.


---

[← 이전](10_geometries_exercises.md) · [목차](00_index.md) · [다음 →](12_spatial_relationships_exercises.md)
