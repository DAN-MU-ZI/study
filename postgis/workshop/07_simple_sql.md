# 7. 기본 SQL (Simple SQL)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/simple_sql.html>](https://postgis.net/workshops/postgis-intro/simple_sql.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

**SQL(Structured Query Language, 구조화 질의 언어)**은 관계형 데이터베이스에서 데이터를 질의하고 조작하기 위한 표준 언어입니다. 4장에서 PostGIS 확장을 활성화하고 버전을 확인할 때 이미 첫 SQL 명령을 실행해 보았습니다.

```sql
SELECT postgis_full_version();
```

데이터베이스에 실습 데이터가 준비되었으므로, 이제 본격적으로 SQL을 사용하여 실제 데이터를 질의해 보겠습니다.

> **질문**: "뉴욕시의 모든 근린지역(Neighborhoods) 이름은 무엇인가요?"

pgAdmin에서 상단의 **Query Tool** 아이콘을 클릭하여 SQL 쿼리 편집기를 엽니다.

![이미지](screenshots/pgadmin_05.png)

쿼리 창에 다음 SQL을 입력합니다.

```sql
SELECT name FROM nyc_neighborhoods;
```

**Execute Query** 버튼(▶)을 클릭하거나 **F5** 키를 누릅니다.

![이미지](screenshots/pgadmin_08.png)

쿼리가 몇 밀리초(ms) 만에 실행되고 총 129건의 결과가 반환됩니다.

![image](screenshots/pgadmin_09.png)

### SQL의 4대 핵심 명령어 (DML)

- `SELECT`: 데이터베이스에서 조건에 맞는 행(Row)을 검색하여 반환합니다.
- `INSERT`: 테이블에 새로운 행을 추가합니다.
- `UPDATE`: 테이블의 기존 행 데이터를 수정합니다.
- `DELETE`: 테이블에서 특정 행을 삭제합니다.

본 워크숍의 공간 데이터 분석 및 공간 함수 실습에서는 대부분 `SELECT` 문을 사용합니다.

---

## SELECT 쿼리 기본 구조

`SELECT` 쿼리의 가장 기본적인 형태는 다음과 같습니다.

```sql
SELECT some_columns FROM some_data_source WHERE some_condition;
```

- `some_columns`: 조회하려는 컬럼명 또는 컬럼 값을 가공하는 함수/수식입니다.
- `some_data_source`: 데이터를 가져올 대상 테이블이거나, 조인(Join)된 복합 테이블입니다.
- `some_condition`: 반환할 행을 제한하는 조건 필터식입니다.

> **질문**: "브루클린(Brooklyn) 자치구에 속한 근린지역 이름은 무엇인가요?"

`WHERE` 절에 조건을 추가하여 브루클린에 속한 동네만 필터링합니다.

```sql
SELECT name
  FROM nyc_neighborhoods
  WHERE boroname = 'Brooklyn';
```

실행하면 브루클린에 속한 23개의 근린지역 이름이 반환됩니다.

---

## 함수 활용 및 집계 (Aggregation)

쿼리 결과에 문자열이나 수치 계산 함수를 적용할 수 있습니다.

> **질문**: "브루클린의 각 근린지역 이름은 몇 글자(문자 수)인가요?"

PostgreSQL 내장 문자열 함수인 `char_length(string)`를 사용합니다.

```sql
SELECT name, char_length(name)
  FROM nyc_neighborhoods
  WHERE boroname = 'Brooklyn';
```

실무에서는 개별 행의 값뿐만 아니라 전체 데이터의 요약 통계(평균, 합계, 표준편차 등)가 필요할 때가 많습니다. 여러 행의 데이터를 입력받아 단일 요약 값을 계산하는 함수를 **집계 함수(Aggregate Function)**라고 합니다.

PostgreSQL은 평균을 구하는 `avg()`, 표준편차를 구하는 `stddev()`, 합계를 구하는 `sum()`, 개수를 세는 `count()` 등 풍부한 내장 집계 함수를 제공합니다.

> **질문**: "브루클린 전체 근린지역 이름 길이의 평균과 표준편차는 얼마인가요?"

```sql
SELECT avg(char_length(name)), stddev(char_length(name))
  FROM nyc_neighborhoods
  WHERE boroname = 'Brooklyn';
```

```text
        avg         |       stddev
--------------------+--------------------
11.7391304347826087 | 3.9105613559407395
```

---

## 그룹화 (GROUP BY)

위의 집계 함수는 결과 전체에 대해 단일 통계를 냈습니다. 만약 자치구(Borough)별로 각각 평균과 표준편차를 구하고 싶다면 어떻게 해야 할까요? 이럴 때 `GROUP BY` 절을 사용합니다.

> **질문**: "뉴욕시의 5개 자치구별 근린지역 이름 길이의 평균과 표준편차는 얼마인가요?"

```sql
SELECT boroname, avg(char_length(name)), stddev(char_length(name))
  FROM nyc_neighborhoods
  GROUP BY boroname;
```

```text
   boroname    |         avg         |       stddev
---------------+---------------------+--------------------
 Brooklyn      | 11.7391304347826087 | 3.9105613559407395
 Manhattan     | 11.8214285714285714 | 4.3123729948325257
 The Bronx     | 12.0416666666666667 | 3.6651017740975152
 Queens        | 11.6666666666666667 | 5.0057438272815975
 Staten Island | 12.2916666666666667 | 5.2043390480959474
```

> [!NOTE]
> 집계 쿼리(`GROUP BY`)를 실행할 때 `SELECT` 절에는 (1) `GROUP BY`에 지정된 그룹화 컬럼이거나 (2) `avg()`, `sum()`과 같은 집계 함수만 올 수 있습니다.

---

## 함수 목록 (Function List)

- [avg(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 수치 컬럼 또는 표현식의 산술 평균값을 계산합니다.
- [char_length(string)](http://www.postgresql.org/docs/current/static/functions-string.html): 문자열의 글자 수를 반환합니다.
- [stddev(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-STATISTICS-TABLE): 입력 값 집합의 표본 표준편차를 계산합니다.


---

[← 이전](06_about_data.md) · [목차](00_index.md) · [다음 →](08_simple_sql_exercises.md)
