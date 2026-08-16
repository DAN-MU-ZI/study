# 14. 조인 연습 (Joins Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/joins_exercises.html>](https://postgis.net/workshops/postgis-intro/joins_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

다음은 우리가 본 기능 중 일부를 상기시켜주는 것입니다. 힌트: 연습에 유용할 것입니다!

- `sum(expression)`: 레코드 세트에 대한 합계를 반환하기 위한 집계
- `count(expression)`: 레코드 세트의 크기를 반환하기 위해 집계
- `ST_Area(geometry)`는 다각형의 면적을 반환합니다.
- `ST_AsText(geometry)`는 WKT `text`를 반환합니다.
- `ST_Contains(geometry A, geometry B)`는 기하학 A에 기하학 B가 포함된 경우 true를 반환합니다.
- `ST_Distance(geometry A, geometry B)`는 형상 A와 형상 B 사이의 최소 거리를 반환합니다.
- `ST_DWithin(geometry A, geometry B, radius)`는 형상 A가 형상 B로부터 반경 거리 이하인 경우 true를 반환합니다.
- `ST_GeomFromText(text)`는 `geometry`를 반환합니다.
- `ST_Intersects(geometry A, geometry B)`는 기하학 A가 기하학 B와 교차하는 경우 true를 반환합니다.
- `ST_Length(linestring)`는 선스트링의 길이를 반환합니다.
- `ST_Touches(geometry A, geometry B)`는 기하학 A의 경계가 기하학 B에 닿으면 true를 반환합니다.
- `ST_Within(geometry A, geometry B)`는 기하학 A가 기하학 B 내에 있는 경우 true를 반환합니다.

또한 사용 가능한 테이블을 기억하십시오.

- `nyc_census_blocks`
  - 이름, popn_total, 보로나메, 검
- `nyc_streets`
  - 이름, 유형, 지리
- `nyc_subway_stations`
  - 이름, 경로, 지리
- `nyc_neighborhoods`
  - 이름, 보로나메, 검

## 연습

- **'리틀 이탈리아'에는 어떤 지하철역이 있나요? 어떤 지하철 노선에 있나요?**

  ```sql
  SELECT s.name, s.routes
  FROM nyc_subway_stations AS s
  JOIN nyc_neighborhoods AS n
  ON ST_Contains(n.geom, s.geom)
  WHERE n.name = 'Little Italy';
  ```

      name    | routes
      -----------+--------
      Spring St | 6

- **6번 열차가 운행하는 지역은 모두 어디입니까?** (힌트: `nyc_subway_stations` 테이블의 `routes` 열에는 'B,D,6,V' 및 'C,6'과 같은 값이 있습니다.)

  ```sql
  SELECT DISTINCT n.name, n.boroname
  FROM nyc_subway_stations AS s
  JOIN nyc_neighborhoods AS n
  ON ST_Contains(n.geom, s.geom)
  WHERE strpos(s.routes,'6') > 0;
  ```

      name        | boroname
      --------------------+-----------
      Midtown            | Manhattan
      Hunts Point        | The Bronx
      Gramercy           | Manhattan
      Little Italy       | Manhattan
      Financial District | Manhattan
      South Bronx        | The Bronx
      Yorkville          | Manhattan
      Murray Hill        | Manhattan
      Mott Haven         | The Bronx
      Upper East Side    | Manhattan
      Chinatown          | Manhattan
      East Harlem        | Manhattan
      Greenwich Village  | Manhattan
      Parkchester        | The Bronx
      Soundview          | The Bronx

  > [!NOTE]
  > 우리는 `DISTINCT` 키워드를 사용하여 한 동네에 지하철 역이 두 개 이상 있는 결과 집합에서 중복된 값을 제거했습니다.

- **9/11 이후 '배터리 파크' 인근 지역은 며칠 동안 출입이 통제되었습니다. 얼마나 많은 사람들이 대피해야 했나요?**

  ```sql
  SELECT Sum(popn_total)
  FROM nyc_neighborhoods AS n
  JOIN nyc_census_blocks AS c
  ON ST_Intersects(n.geom, c.geom)
  WHERE n.name = 'Battery Park';
  ```

      17153

- **인구 밀도(인/km2)가 가장 높은 지역은 어디입니까?**

  ```sql
  SELECT
    n.name,
    Sum(c.popn_total) / (ST_Area(n.geom) / 1000000.0) AS popn_per_sqkm
  FROM nyc_census_blocks AS c
  JOIN nyc_neighborhoods AS n
  ON ST_Intersects(c.geom, n.geom)
  GROUP BY n.name, n.geom
  ORDER BY popn_per_sqkm DESC LIMIT 2;
  ```

      name       |  popn_per_sqkm
      -------------------+------------------
      North Sutton Area | 68435.13283772678
      East Village      | 50404.48341332535

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

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

[← 이전](13_joins.md) · [목차](00_index.md) · [다음 →](15_indexing.md)