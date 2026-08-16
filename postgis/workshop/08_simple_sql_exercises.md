# 8. 단순 SQL 실습 (Simple SQL Exercises)

앞서 배운 SQL 문법을 활용하여 다음 질문에 답하는 쿼리를 작성해 보세요.

---

## 실습 문제

### 문제 1
**"West Village" 이웃 지역(neighborhood)의 자치구(borough) 이름은 무엇인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT boroname
FROM nyc_neighborhoods
WHERE name = 'West Village';
```

**결과**: `Manhattan`
</details>

---

### 문제 2
**맨해튼(Manhattan)의 총 인구수는 몇 명인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT SUM(popn_total) AS total_population
FROM nyc_census_blocks
WHERE boroname = 'Manhattan';
```

**결과**: `1537195` (약 153만 명)
</details>

---

### 문제 3
**뉴욕시에 있는 "B" 노선(B train)이 정차하는 지하철역은 총 몇 개인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT COUNT(*)
FROM nyc_subway_stations
WHERE routes LIKE '%B%';
```

**결과**: `56`
</details>

---

### 문제 4
**뉴욕시 5개 자치구(borough) 중에서 흑인(Black) 인구 비율이 가장 높은 자치구는 어디인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  boroname,
  ROUND(100.0 * SUM(popn_black) / SUM(popn_total), 2) AS pct_black
FROM nyc_census_blocks
GROUP BY boroname
ORDER BY pct_black DESC
LIMIT 1;
```

**결과**: `Brooklyn` (약 35.79%)
</details>

---

| [⬅️ 7. 단순 SQL (Simple SQL)](07_simple_sql.md) | [🏠 워크숍 목차](README.md) | [9. 지오메트리 (Geometries) ➡️](09_geometries.md) |
| :--- | :---: | ---: |
