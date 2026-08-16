# 14. 공간 조인 실습 (Spatial Joins Exercises)

공간 조인과 집계 함수를 활용한 심화 실습 문제입니다.

---

## 실습 문제

### 문제 1
**지하철역이 가장 많은 자치구(borough)는 어디이며, 각각 몇 개의 역이 있나요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  n.boroname,
  COUNT(s.name) AS num_stations
FROM nyc_neighborhoods n
JOIN nyc_subway_stations s
  ON ST_Contains(n.geom, s.geom)
GROUP BY n.boroname
ORDER BY num_stations DESC;
```

**결과**: Brooklyn (173개), Manhattan (149개), Queens (132개), Bronx (70개), Staten Island (0개)
</details>

---

### 문제 2
**"Little Italy" 이웃 지역의 총 인구수는 몇 명인가요? (인구조사 블록과 공간 조인)**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  n.name,
  SUM(c.popn_total) AS total_population
FROM nyc_neighborhoods n
JOIN nyc_census_blocks c
  ON ST_Intersects(n.geom, c.geom)
WHERE n.name = 'Little Italy'
GROUP BY n.name;
```
</details>

---

### 문제 3
**"FDR Drive" 도로에서 50미터 이내에 거주하는 총 인구수는 몇 명인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT SUM(c.popn_total) AS population_near_fdr
FROM nyc_census_blocks c
JOIN nyc_streets s
  ON ST_DWithin(c.geom, s.geom, 50)
WHERE s.name = 'FDR Dr';
```
</details>

---

### 문제 4
**살인 사건(`nyc_homicides`)이 가장 많이 발생한 상위 3개 이웃 지역은 어디인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  n.name,
  n.boroname,
  COUNT(h.incident_date) AS homicide_count
FROM nyc_neighborhoods n
JOIN nyc_homicides h
  ON ST_Contains(n.geom, h.geom)
GROUP BY n.name, n.boroname
ORDER BY homicide_count DESC
LIMIT 3;
```
</details>

---

| [⬅️ 13. 공간 조인 (Spatial Joins)](13_joins.md) | [🏠 워크숍 목차](README.md) | [15. 공간 인덱싱 (Spatial Indexing) ➡️](15_indexing.md) |
| :--- | :---: | ---: |
