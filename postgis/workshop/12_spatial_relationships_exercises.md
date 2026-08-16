# 12. 공간 관계 실습 (Spatial Relationships Exercises)

공간 관계 함수(`ST_Intersects`, `ST_Contains`, `ST_DWithin` 등)를 활용한 실습 문제입니다.

---

## 실습 문제

### 문제 1
**"Broad St" 지하철역에서 100미터 이내에 있는 모든 도로의 이름은 무엇인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT DISTINCT str.name
FROM nyc_streets str, nyc_subway_stations sub
WHERE sub.name = 'Broad St'
  AND ST_DWithin(str.geom, sub.geom, 100)
ORDER BY str.name;
```
</details>

---

### 문제 2
**"Battery Park" 이웃 지역 안에 위치한 지하철역들의 이름은 무엇인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT sub.name
FROM nyc_subway_stations sub, nyc_neighborhoods n
WHERE n.name = 'Battery Park'
  AND ST_Within(sub.geom, n.geom);
```
</details>

---

### 문제 3
**"Central Park" 경계와 맞닿아(Touches) 있는 모든 이웃 지역들의 이름은 무엇인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT n.name
FROM nyc_neighborhoods n, nyc_neighborhoods cp
WHERE cp.name = 'Central Park'
  AND ST_Touches(n.geom, cp.geom);
```

**결과**: `Upper East Side`, `Upper West Side`, `Harlem`, `Midtown` 등
</details>

---

### 문제 4
**"Atlantic Ave" 도로와 교차(Intersects)하는 모든 인구조사 블록들의 총 인구수는 몇 명인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT SUM(c.popn_total) AS total_population
FROM nyc_census_blocks c, nyc_streets s
WHERE s.name = 'Atlantic Ave'
  AND ST_Intersects(c.geom, s.geom);
```
</details>

---

| [⬅️ 11. 공간 관계 (Spatial Relationships)](11_spatial_relationships.md) | [🏠 워크숍 목차](README.md) | [13. 공간 조인 (Spatial Joins) ➡️](13_joins.md) |
| :--- | :---: | ---: |
