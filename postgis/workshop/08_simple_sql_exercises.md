# 8. 기본 SQL 실습 (Simple SQL Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/simple_sql_exercises.html>](https://postgis.net/workshops/postgis-intro/simple_sql_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

`nyc_census_blocks`, `nyc_streets`, `nyc_neighborhoods` 테이블을 사용하여 다음 질문에 대한 SQL 쿼리를 직접 작성해 보세요 (정답을 먼저 보지 말고 먼저 풀어보세요!).

### 실습에 필요한 주요 집계 함수 요약
- `avg(expression)`: 값 집합의 산술 평균
- `sum(expression)`: 값 집합의 총합계
- `count(expression)`: 조건에 맞는 레코드(행) 개수

---

## 연습 문제 및 정답

### 1. nyc_streets 테이블에는 총 몇 개의 도로 레코드가 있습니까?

```sql
SELECT count(*)
FROM nyc_streets;
```

```text
19091
```

---

### 2. 뉴욕시에는 'B'로 시작하는 도로가 몇 개나 있습니까?

```sql
SELECT count(*)
FROM nyc_streets
WHERE name LIKE 'B%';
```

```text
1282
```

---

### 3. 뉴욕시의 총 인구수는 얼마입니까?

```sql
SELECT sum(popn_total) AS population
FROM nyc_census_blocks;
```

```text
8175032
```

> [!NOTE]
> `AS` 키워드는 컬럼이나 수식에 알기 쉬운 별칭(Alias)을 부여합니다. 별칭을 지정하면 기본 출력 컬럼명인 `sum` 대신 직관적인 `population`으로 결과를 확인할 수 있습니다.

---

### 4. 브롱크스(The Bronx) 자치구의 인구수는 얼마입니까?

```sql
SELECT sum(popn_total) AS population
FROM nyc_census_blocks
WHERE boroname = 'The Bronx';
```

```text
1385108
```

---

### 5. 각 자치구(Borough)별 근린지역(Neighborhoods)은 각각 몇 개씩 있습니까?

```sql
SELECT boroname, count(*)
FROM nyc_neighborhoods
GROUP BY boroname;
```

```text
   boroname    | count
---------------+-------
 Queens        |    30
 Brooklyn      |    23
 Staten Island |    24
 The Bronx     |    24
 Manhattan     |    28
```

---

### 6. 각 자치구별 백인 인구의 비율(%)은 각각 몇 퍼센트입니까?

```sql
SELECT
  boroname,
  100.0 * sum(popn_white) / sum(popn_total) AS white_pct
FROM nyc_census_blocks
GROUP BY boroname;
```

```text
   boroname    |    white_pct
---------------+------------------
 Brooklyn      | 42.8011737932687
 Manhattan     | 57.4493039480463
 The Bronx     | 27.9037446899448
 Queens        |  39.722077394591
 Staten Island | 72.8942034860154
```

---

## 함수 목록 (Function List)

- [avg(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 숫자 컬럼 또는 표현식의 평균값을 계산하는 집계 함수입니다.
- [count(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 조건을 만족하는 레코드 수를 계산하는 집계 함수입니다.
- [sum(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 숫자 컬럼 또는 표현식의 합계를 계산하는 집계 함수입니다.


---

[← 이전](07_simple_sql.md) · [목차](00_index.md) · [다음 →](09_geometries.md)
