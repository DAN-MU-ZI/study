# 7. 단순 SQL (Simple SQL)

본 챕터에서는 PostGIS 공간 분석으로 나아가기 전, PostgreSQL에서 사용하는 기본적인 SQL 쿼리 문법을 복습합니다.

---

## 1. 데이터 조회 (SELECT)

가장 기본적인 형태의 데이터 조회 쿼리입니다.

```sql
SELECT name, boroname FROM nyc_neighborhoods;
```

조회 결과 개수를 제한하려면 `LIMIT` 절을 사용합니다:

```sql
SELECT name, boroname FROM nyc_neighborhoods LIMIT 5;
```

---

## 2. 조건 필터링 (WHERE)

특정 조건을 만족하는 행만 조회할 때 `WHERE` 절을 사용합니다:

```sql
-- 브루클린(Brooklyn)에 속한 이웃 지역만 조회
SELECT name, boroname
FROM nyc_neighborhoods
WHERE boroname = 'Brooklyn';
```

문자열 부분 일치 검색에는 `LIKE` 또는 대소문자를 구분하지 않는 `ILIKE`를 사용합니다:

```sql
-- 이름이 'West'로 시작하는 이웃 지역 조회
SELECT name, boroname
FROM nyc_neighborhoods
WHERE name ILIKE 'West%';
```

---

## 3. 집계 함수 (Aggregate Functions)

`COUNT`, `SUM`, `AVG`, `MIN`, `MAX` 등의 집계 함수를 사용하여 통계를 계산합니다:

```sql
-- 뉴욕시 전체 인구수 합계 계산
SELECT SUM(popn_total) AS total_population
FROM nyc_census_blocks;
```

```sql
-- 브롱크스(Bronx)의 백인 총 인구수
SELECT SUM(popn_white) AS total_white_population
FROM nyc_census_blocks
WHERE boroname = 'The Bronx';
```

---

## 4. 그룹화 및 정렬 (GROUP BY & ORDER BY)

데이터를 특정 기준 컬럼별로 묶어서 요약할 때 `GROUP BY`를 사용하고, 결과를 정렬할 때 `ORDER BY`를 사용합니다:

```sql
-- 자치구별 총 인구수 및 정렬
SELECT boroname, SUM(popn_total) AS total_population
FROM nyc_census_blocks
GROUP BY boroname
ORDER BY total_population DESC;
```

```sql
-- 자치구별 백인 인구 비율 계산
SELECT
  boroname,
  ROUND(100.0 * SUM(popn_white) / SUM(popn_total), 2) AS pct_white
FROM nyc_census_blocks
GROUP BY boroname
ORDER BY pct_white DESC;
```

---

| [⬅️ 6. 실습 데이터 소개 (About our data)](06_about_data.md) | [🏠 워크숍 목차](README.md) | [8. 단순 SQL 실습 (Simple SQL Exercises) ➡️](08_simple_sql_exercises.md) |
| :--- | :---: | ---: |
