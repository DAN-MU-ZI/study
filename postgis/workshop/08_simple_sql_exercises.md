# 08. 간단한 SQL 연습 (Simple SQL Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/simple_sql_exercises.html>](https://postgis.net/workshops/postgis-intro/simple_sql_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

`nyc_census_blocks` 테이블을 사용하여 다음 질문에 답하십시오(답변을 엿보지 마십시오!).

다음은 시작하는 데 도움이 되는 몇 가지 정보입니다. `About Our Data <about_data>` 섹션의 `nyc_census_blocks` 테이블 정의를 기억해 보세요.

|  |  |
|----|----|
| **blkid** | 모든 인구조사 **block**를 고유하게 식별하는 15자리 코드입니다. ("360050001009000") |
| **popn_total** | 인구 조사 블록의 총 인원 수 |
| **popn_white** | 해당 블록에서 자신을 "백인"으로 식별하는 사람의 수 |
| **popn_black** | 해당 블록에서 스스로를 "흑인"으로 식별하는 사람의 수 |
| **popn_nativ** | 해당 블록에서 "미국 원주민"이라고 스스로 식별하는 사람의 수 |
| **popn_asian** | 해당 블록 내에서 자신을 "asias"로 식별하는 사람의 수 |
| **popn_other** | 블록의 다른 카테고리로 스스로 식별되는 사람의 수 |
| **hous_total** | 해당 블록의 주택 수 |
| **hous_own** | 해당 블록 내 자가 거주 주택 수 |
| **hous_rent** | 해당 블록 내 임차인이 거주하는 주택 수 |
| **boroname** | 뉴욕 자치구의 이름입니다. 맨해튼, 브롱크스, 브루클린, 스태튼 아일랜드, 퀸즈 |
| **geom** | 블록의 다각형 경계 |

다음은 유용할 수 있는 몇 가지 일반적인 SQL 집계 함수입니다.

- avg() - 레코드 집합에 있는 값의 평균(평균)
- sum() - 레코드 집합에 있는 값의 합계
- count() - 레코드 집합의 레코드 수

이제 질문은 다음과 같습니다.

- **nyc_streets 테이블에는 몇 개의 레코드가 있습니까?**

  ```sql
  SELECT Count(*)
  FROM nyc_streets;
  ```

      19091

- **뉴욕에는 'B'로 시작하는 거리가 몇 개나 되나요?**

  ```sql
  SELECT Count(*)
    FROM nyc_streets
    WHERE name LIKE 'B%';
  ```

      1282

- **뉴욕시의 인구는 얼마나 됩니까?**

  ```sql
  SELECT Sum(popn_total) AS population
    FROM nyc_census_blocks;
  ```

      8175032

  > [!NOTE]
  > 이 `AS`는 무엇인가요? 별칭을 사용하여 테이블이나 열에 다른 이름을 지정할 수 있습니다. 별칭을 사용하면 쿼리를 더 쉽게 작성하고 읽을 수 있습니다. 따라서 출력된 열 이름 `sum` 대신 **AS**를 더 읽기 쉬운 `population`로 씁니다.

- **브롱크스의 인구는 얼마나 되나요?**

  ```sql
  SELECT Sum(popn_total) AS population
    FROM nyc_census_blocks
    WHERE boroname = 'The Bronx';
  ```

      1385108

- **각 자치구에는 몇 개의 "이웃"이 있습니까?**

  ```sql
  SELECT boroname, count(*)
    FROM nyc_neighborhoods
    GROUP BY boroname;
  ```

      boroname    | count
      ---------------+-------
      Queens        |    30
      Brooklyn      |    23
      Staten Island |    24
      The Bronx     |    24
      Manhattan     |    28

- **각 자치구에 대해 백인 인구 비율은 몇 퍼센트입니까?**

  ```sql
  SELECT
    boroname,
    100.0 * Sum(popn_white)/Sum(popn_total) AS white_pct
  FROM nyc_census_blocks
  GROUP BY boroname;
  ```

      boroname    |    white_pct
      ---------------+------------------
      Brooklyn      | 42.8011737932687
      Manhattan     | 57.4493039480463
      The Bronx     | 27.9037446899448
      Queens        |  39.722077394591
      Staten Island | 72.8942034860154

## 기능 목록

[avg(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 숫자 열의 평균 값을 반환하는 PostgreSQL 집계 함수입니다.

[count(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 레코드 집합의 레코드 수를 반환하는 PostgreSQL 집계 함수입니다.

[sum(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 레코드 집합의 레코드 합계를 반환하는 PostgreSQL 집계 함수입니다.

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

```sql
SELECT boroname
FROM nyc_neighborhoods
WHERE name = 'West Village';
```

**결과**: `Manhattan`

</details>

---

[← 이전](07_simple_sql.md) · [목차](00_index.md) · [다음 →](09_geometries.md)