# 13. 공간 조인 (Spatial Joins)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/joins.html>](https://postgis.net/workshops/postgis-intro/joins.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

공간 조인은 공간 데이터베이스의 핵심입니다. 공간 관계를 조인 키로 사용하여 다양한 테이블의 정보를 결합할 수 있습니다. 우리가 "표준 GIS 분석"이라고 생각하는 것의 대부분은 공간 조인으로 표현될 수 있습니다.

이전 섹션에서는 2단계 프로세스를 사용하여 공간 관계를 탐색했습니다. 먼저 'Broad St'에 대한 지하철역 지점을 추출했습니다. 그런 다음 그 포인트를 사용하여 "'Broad St' 역은 어느 동네에 있나요?"와 같은 추가 질문을 했습니다.

공간 조인을 사용하면 지하철 역과 그 역이 포함된 동네에 대한 정보를 검색하여 한 단계로 질문에 답할 수 있습니다.

```sql
SELECT
  subways.name AS subway_name,
  neighborhoods.name AS neighborhood_name,
  neighborhoods.boroname AS borough
FROM nyc_neighborhoods AS neighborhoods
JOIN nyc_subway_stations AS subways
ON ST_Contains(neighborhoods.geom, subways.geom)
WHERE subways.name = 'Broad St';
```

    subway_name | neighborhood_name  |  borough
    -------------+--------------------+-----------
    Broad St    | Financial District | Manhattan

우리는 모든 지하철역을 포함된 지역에 연결할 수 있었지만 이 경우에는 단 하나의 지하철역에 대한 정보를 원했습니다. 두 테이블 간의 참/거짓 관계를 제공하는 모든 함수를 사용하여 공간 조인을 구동할 수 있지만 가장 일반적으로 사용되는 함수는 `ST_Intersects`, `ST_Contains` 및 `ST_DWithin`입니다.

## 가입 및 요약

`JOIN`와 `GROUP BY`의 조합은 일반적으로 GIS 시스템에서 수행되는 종류의 분석을 제공합니다.

예: **"맨해튼 지역의 인구 및 인종 구성은 무엇입니까?"** 여기에 인구 조사의 인구 정보와 지역 경계를 결합하고 맨해튼의 한 자치구로 제한하는 질문이 있습니다.

```sql
SELECT
  neighborhoods.name AS neighborhood_name,
  Sum(census.popn_total) AS population,
  100.0 * Sum(census.popn_white) / Sum(census.popn_total) AS white_pct,
  100.0 * Sum(census.popn_black) / Sum(census.popn_total) AS black_pct
FROM nyc_neighborhoods AS neighborhoods
JOIN nyc_census_blocks AS census
ON ST_Intersects(neighborhoods.geom, census.geom)
WHERE neighborhoods.boroname = 'Manhattan'
GROUP BY neighborhoods.name
ORDER BY white_pct DESC;
```

    neighborhood_name  | population | white_pct | black_pct
    ---------------------+------------+-----------+-----------
    Carnegie Hill       |      18763 |      90.1 |       1.4
    North Sutton Area   |      22460 |      87.6 |       1.6
    West Village        |      26718 |      87.6 |       2.2
    Upper East Side     |     203741 |      85.0 |       2.7
    Soho                |      15436 |      84.6 |       2.2
    Greenwich Village   |      57224 |      82.0 |       2.4
    Central Park        |      46600 |      79.5 |       8.0
    Tribeca             |      20908 |      79.1 |       3.5
    Gramercy            |     104876 |      75.5 |       4.7
    Murray Hill         |      29655 |      75.0 |       2.5
    Chelsea             |      61340 |      74.8 |       6.4
    Upper West Side     |     214761 |      74.6 |       9.2
    Midtown             |      76840 |      72.6 |       5.2
    Battery Park        |      17153 |      71.8 |       3.4
    Financial District  |      34807 |      69.9 |       3.8
    Clinton             |      32201 |      65.3 |       7.9
    East Village        |      82266 |      63.3 |       8.8
    Garment District    |      10539 |      55.2 |       7.1
    Morningside Heights |      42844 |      52.7 |      19.4
    Little Italy        |      12568 |      49.0 |       1.8
    Yorkville           |      58450 |      35.6 |      29.7
    Inwood              |      50047 |      35.2 |      16.8
    Washington Heights  |     169013 |      34.9 |      16.8
    Lower East Side     |      96156 |      33.5 |       9.1
    East Harlem         |      60576 |      26.4 |      40.4
    Hamilton Heights    |      67432 |      23.9 |      35.8
    Chinatown           |      16209 |      15.2 |       3.8
    Harlem              |     134955 |      15.1 |      67.1

여기서 무슨 일이 일어나고 있는 걸까요? 개념적으로(실제 평가 순서는 데이터베이스에 의해 최적화됩니다) 다음과 같은 일이 발생합니다.

1.  `JOIN` 절은 이웃 테이블과 인구 조사 테이블의 열을 모두 포함하는 가상 테이블을 생성합니다.
2.  `WHERE` 절은 가상 테이블을 맨해튼의 행으로 필터링합니다.
3.  나머지 행은 동네 이름별로 그룹화되고 집계 함수를 통해 `Sum()` 인구 값에 공급됩니다.
4.  최종 숫자에 대해 약간의 산술 및 형식 지정(예: `GROUP BY`, `ORDER BY`)을 수행한 후 쿼리를 통해 백분율이 표시됩니다.

> [!NOTE]
> `JOIN` 절은 두 개의 `FROM` 항목을 결합합니다. 기본적으로 `INNER JOIN`를 사용하지만 조인에는 네 가지 다른 유형이 있습니다. 자세한 내용은 PostgreSQL 설명서의 [join_type](http://www.postgresql.org/docs/9.1/interactive/sql-select.html#SQL-FROM) 정의를 참조하세요.

또한 거리 테스트를 조인 키로 사용하여 요약된 "반경 내의 모든 항목" 쿼리를 생성할 수도 있습니다. 거리 쿼리를 사용하여 뉴욕의 인종 지리를 살펴보겠습니다.

먼저, 도시의 기본 인종 구성을 살펴보겠습니다.

```sql
SELECT
  100.0 * Sum(popn_white) / Sum(popn_total) AS white_pct,
  100.0 * Sum(popn_black) / Sum(popn_total) AS black_pct,
  Sum(popn_total) AS popn_total
FROM nyc_census_blocks;
```

    white_pct     |    black_pct     | popn_total
    ------------------+------------------+------------
    44.0039500762811 | 25.5465789002416 |    8175032

따라서 뉴욕의 800만 명 중 약 44%가 "백인"으로 기록되고 26%가 "흑인"으로 기록됩니다.

Duke Ellington은 할렘의 Sugar Hill에 가려면 A 열차를 타야 한다고 노래했습니다. 앞에서 보았듯이 할렘은 맨해튼에서 아프리카계 미국인 인구 비율이 가장 높은 지역입니다(80.5%). A 열차가 지나는 지역도 같은 특성을 보일까요?

먼저 `nyc_subway_stations` 테이블 `routes` 필드의 내용이 A-train을 찾는 데 관심이 있는 내용입니다. 거기에 있는 값은 약간 복잡합니다.

```sql
SELECT DISTINCT routes FROM nyc_subway_stations;
```

    A,C,G
    4,5
    D,F,N,Q
    5
    E,F
    E,J,Z
    R,W

> [!NOTE]
> `DISTINCT` 키워드는 결과에서 중복 행을 제거합니다. `DISTINCT` 키워드가 없으면 위 쿼리는 73개가 아닌 491개의 결과를 식별합니다.

따라서 A-열을 찾으려면 `routes`에 'A'가 있는 행이 필요합니다. 이 작업은 여러 가지 방법으로 수행할 수 있지만 오늘은 `routes` 필드에 'A'가 있는 경우에만 `strpos(routes,'A')`가 0이 아닌 숫자를 반환한다는 사실을 활용하겠습니다.

```sql
SELECT DISTINCT routes
FROM nyc_subway_stations AS subways
WHERE strpos(subways.routes,'A') > 0;
```

    A,B,C
    A,C
    A
    A,C,G
    A,C,E,L
    A,S
    A,C,F
    A,B,C,D
    A,C,E

A열차선 반경 200m 이내의 인종 구성을 정리해보자.

```sql
SELECT
  100.0 * Sum(popn_white) / Sum(popn_total) AS white_pct,
  100.0 * Sum(popn_black) / Sum(popn_total) AS black_pct,
  Sum(popn_total) AS popn_total
FROM nyc_census_blocks AS census
JOIN nyc_subway_stations AS subways
ON ST_DWithin(census.geom, subways.geom, 200)
WHERE strpos(subways.routes,'A') > 0;
```

    white_pct     |    black_pct     | popn_total
    ------------------+------------------+------------
    45.5901255900202 | 22.0936235670937 |     189824

따라서 A-트레인의 인종 구성은 뉴욕시 전체의 구성과 근본적으로 다르지 않습니다.

## 고급 조인

마지막 섹션에서 우리는 A-트레인이 도시의 나머지 지역의 인종 구성과 크게 다른 인구에게 서비스를 제공하지 않는다는 것을 확인했습니다. 평균적이지 않은 인종 구성을 지닌 열차가 있습니까?

해당 질문에 답하기 위해 쿼리에 또 다른 조인을 추가하여 여러 지하철 노선의 구성을 동시에 계산할 수 있습니다. 그렇게 하려면 요약하려는 모든 줄을 열거하는 새 테이블을 만들어야 합니다.

```sql
CREATE TABLE subway_lines ( route char(1) );
INSERT INTO subway_lines (route) VALUES
  ('A'),('B'),('C'),('D'),('E'),('F'),('G'),
  ('J'),('L'),('M'),('N'),('Q'),('R'),('S'),
  ('Z'),('1'),('2'),('3'),('4'),('5'),('6'),
  ('7');
```

이제 지하철 노선 테이블을 원래 쿼리에 결합할 수 있습니다.

```sql
SELECT
  lines.route,
  100.0 * Sum(popn_white) / Sum(popn_total) AS white_pct,
  100.0 * Sum(popn_black) / Sum(popn_total) AS black_pct,
  Sum(popn_total) AS popn_total
FROM nyc_census_blocks AS census
JOIN nyc_subway_stations AS subways
ON ST_DWithin(census.geom, subways.geom, 200)
JOIN subway_lines AS lines
ON strpos(subways.routes, lines.route) > 0
GROUP BY lines.route
ORDER BY black_pct DESC;
```

    route | white_pct | black_pct | popn_total
    -------+-----------+-----------+------------
    S     |      39.8 |      46.5 |      33301
    3     |      42.7 |      42.1 |     223047
    5     |      33.8 |      41.4 |     218919
    2     |      39.3 |      38.4 |     291661
    C     |      46.9 |      30.6 |     224411
    4     |      37.6 |      27.4 |     174998
    B     |      40.0 |      26.9 |     256583
    A     |      45.6 |      22.1 |     189824
    J     |      37.6 |      21.6 |     132861
    Q     |      56.9 |      20.6 |     127112
    Z     |      38.4 |      20.2 |      87131
    D     |      39.5 |      19.4 |     234931
    L     |      57.6 |      16.8 |     110118
    G     |      49.6 |      16.1 |     135012
    6     |      52.3 |      15.7 |     260240
    1     |      59.1 |      11.3 |     327742
    F     |      60.9 |       7.5 |     229439
    M     |      56.5 |       6.4 |     174196
    E     |      66.8 |       4.7 |      90958
    R     |      58.5 |       4.0 |     196999
    N     |      59.7 |       3.5 |     147792
    7     |      35.7 |       3.5 |     102401

이전과 마찬가지로 조인은 `JOIN ON` 제한 사항 내에서 사용 가능한 모든 조합의 가상 테이블을 생성한 다음 해당 행을 `GROUP` 요약에 제공합니다. 공간 마법은 `ST_DWithin` 함수에 있으며, 해당 지하철 역에 가까운 인구 조사 블록만 계산에 포함되도록 합니다.

## 기능 목록

[ST_Contains(기하학 A, 기하학 B)](http://postgis.net/docs/ST_Contains.html): B의 포인트가 A의 외부에 있지 않고 B 내부의 최소 하나의 포인트가 A의 내부에 있는 경우에만 true를 반환합니다.

[ST_DWithin(기하학 A, 기하학 B, 반경)](http://postgis.net/docs/ST_DWithin.html): 기하학이 서로 지정된 거리 내에 있는 경우 true를 반환합니다.

[ST_Intersects(geometry A, geometry B)](http://postgis.net/docs/ST_Intersects.html): 두 지오메트리 또는 지오그래피가 공간의 일부를 공유하면 `TRUE`, 서로 분리되어 있으면 `FALSE`를 반환합니다.

[round(v 숫자, s 정수)](http://www.postgresql.org/docs/current/interactive/functions-math.html): s 소수점 이하 자릿수로 반올림하는 PostgreSQL 수학 함수

[strpos(string, substring)](http://www.postgresql.org/docs/current/static/functions-string.html): 지정된 하위 문자열의 정수 위치를 반환하는 PostgreSQL 문자열 함수입니다.

[sum(expression)](http://www.postgresql.org/docs/current/static/functions-aggregate.html#FUNCTIONS-AGGREGATE-TABLE): 레코드 집합의 레코드 합계를 반환하는 PostgreSQL 집계 함수입니다.


---

[← 이전](12_spatial_relationships_exercises.md) · [목차](00_index.md) · [다음 →](14_joins_exercises.md)
