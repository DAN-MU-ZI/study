# 24. 동등성 (Equality)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/equality.html>](https://postgis.net/workshops/postgis-intro/equality.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

공간 데이터베이스에서 "두 지오메트리가 서로 같은가?"라는 질문은 단순한 스칼라 값의 비교보다 훨씬 미묘합니다. PostGIS는 서로 다른 수준의 동등성을 판별할 수 있는 3가지 비교 기준을 지원합니다.

1. **정확한 순서 동등성 (Exact / Ordering Equality)**: 모든 정점의 좌표와 정의된 순서가 완벽히 일치하는지 비교
2. **공간적 동등성 (Spatial Equality)**: 정점의 시작 위치나 방향과 무관하게 두 객체가 차지하는 공간 영역이 동일한지 비교
3. **경계 상자 동등성 (Bounding Box Equality)**: 두 객체의 바운딩 박스 범위가 일치하는지 비교

---

## 실습용 다각형 테이블 생성

동등성의 차이를 확인하기 위해 다음과 같은 5가지 폴리곤을 생성합니다.

![image](equality/polygon-table.png)

```sql
CREATE TABLE polygons (id integer, name varchar, poly geometry);

INSERT INTO polygons VALUES
  (1, 'Polygon 1', 'POLYGON((-1 1.732, 1 1.732, 2 0, 1 -1.732, -1 -1.732, -2 0, -1 1.732))'),
  (2, 'Polygon 2', 'POLYGON((-1 1.732, -2 0, -1 -1.732, 1 -1.732, 2 0, 1 1.732, -1 1.732))'),
  (3, 'Polygon 3', 'POLYGON((1 -1.732, 2 0, 1 1.732, -1 1.732, -2 0, -1 -1.732, 1 -1.732))'),
  (4, 'Polygon 4', 'POLYGON((-1 1.732, 0 1.732, 1 1.732, 1.5 0.866, 2 0, 1.5 -0.866, 1 -1.732, 0 -1.732, -1 -1.732, -1.5 -0.866, -2 0, -1.5 0.866, -1 1.732))'),
  (5, 'Polygon 5', 'POLYGON((-2 -1.732, 2 -1.732, 2 1.732, -2 1.732, -2 -1.732))');
```

---

## 1. 정확한 순서 동등성: ST_OrderingEquals

`ST_OrderingEquals(geometry A, geometry B)`는 두 지오메트리의 정점 좌표뿐만 아니라 **정점들의 나열 순서(방향 및 시작점)**까지 정확히 일치하는지 검사합니다.

```sql
SELECT a.name, b.name,
  CASE WHEN ST_OrderingEquals(a.poly, b.poly)
       THEN 'Exactly Equal'
       ELSE 'Not Exactly Equal' END AS result
FROM polygons AS a, polygons AS b;
```

![이미지](equality/start14.png)

- Polygon 1, 2, 3은 동일한 육각형 영역을 나타내지만, 정점의 회전 방향(시계 방향 vs 반시계 방향)과 시작 정점의 위치가 다르므로 `ST_OrderingEquals`에서는 서로 다른 객체로 판정됩니다.
- Polygon 4는 동일 선상에 중복 정점들을 포함하고 있으므로 정점 개수가 달라 불일치합니다.

---

## 2. 공간적 동등성: ST_Equals

`ST_Equals(geometry A, geometry B)`는 정점의 정의 순서, 시작점, 동일선상의 중복 정점 유무와 무관하게 **두 객체가 2차원 평면에서 차지하는 공간 영역이 정확히 일치하는지** 검사합니다.

```sql
SELECT a.name, b.name,
  CASE WHEN ST_Equals(a.poly, b.poly)
       THEN 'Spatially Equal'
       ELSE 'Not Equal' END AS result
FROM polygons AS a, polygons AS b;
```

![이미지](equality/start15.png)

- Polygon 1부터 4까지는 표현 방식만 다를 뿐 모두 정확히 동일한 육각형 영역을 나타내므로 `ST_Equals`에서 모두 `Spatially Equal`로 판정됩니다.
- Polygon 5는 동일한 바운딩 박스를 가진 직사각형이므로 육각형과 공간이 달라 `Not Equal`로 판정됩니다.

---

## 3. 경계 상자 동등성: ~= 연산자

`~=` 연산자는 지오메트리의 실제 형태를 무시하고 **경계 상자(Bounding Box)의 범위가 일치하는지**만 고속으로 비교합니다.

```sql
SELECT a.name, b.name,
  CASE WHEN a.poly ~= b.poly
       THEN 'Equal Bounds'
       ELSE 'Non-equal Bounds' END AS result
FROM polygons AS a, polygons AS b;
```

![이미지](equality/start17.png)

- Polygon 1부터 5까지 모두 외곽 경계 상자(X: -2~2, Y: -1.732~1.732)가 동일하므로 모두 `Equal Bounds`를 반환합니다.
- `~=` 연산자는 공간 인덱스(GiST)를 통해 인덱스 레벨에서 매우 빠르게 동등 여부를 필터링할 때 유용합니다.

---

## 함수 목록 (Function List)

- [ST_OrderingEquals(geometry A, geometry B)](http://postgis.net/docs/ST_OrderingEquals.html): 두 지오메트리의 정점 좌표와 정의 순서가 완벽히 동일하면 `TRUE`를 반환합니다.
- [ST_Equals(geometry A, geometry B)](http://postgis.net/docs/ST_Equals.html): 두 지오메트리가 공간적으로 동일한 영역/형상을 차지하면 `TRUE`를 반환합니다 (정점 순서 및 방향 무시).
- [geometry_a ~= geometry_b](http://postgis.net/docs/ST_Geometry_Same.html): 두 지오메트리의 2차원 경계 상자(Bounding Box)가 일치하면 `TRUE`를 반환합니다.


---

[← 이전](23_validity.md) · [목차](00_index.md) · [다음 →](25_linear_referencing.md)
