# 18. 지오그래피 (Geography)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geography.html>](https://postgis.net/workshops/postgis-intro/geography.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

GPS나 웹 지도에서 가장 흔히 접하는 좌표는 경도/위도(Longitude/Latitude) 형식의 지리 좌표입니다.

그러나 UTM이나 State Plane과 같은 평면 직교 좌표와 달리, 지리 좌표는 **데카르트 평면 직교 좌표(Cartesian Coordinates)가 아닙니다**. 지리 좌표는 평면상의 선형 거리(m, km)를 나타내는 것이 아니라, 둥근 지구본 상에서의 **구면 각도(Spherical Angles, 단위: 도/Degree)**를 나타냅니다.

![image](geography/cartesian_spherical.jpg)

지리 좌표를 평면 데카르트 좌표로 간주하고 평면 거리 함수를 호출하면 왜곡이 발생하여 현실과 동떨어진 결과가 나옵니다. 위도에 따라 1도($1^\circ$)가 나타내는 실제 지상 거리가 크게 변하기 때문입니다(적도 부근에서는 약 111km이지만, 극지방으로 갈수록 0에 수렴).

---

## 평면 지오메트리 vs 구면 지오그래피 거리 계산 비교

로스앤젤레스(LAX)와 파리(CDG) 공항의 좌표를 예로 들어보겠습니다.

- 로스앤젤레스(LAX): `POINT(-118.4079 33.9434)`
- 파리(CDG): `POINT(2.5559 49.0083)`

먼저 평면 지오메트리(`geometry`) 타입으로 거리를 계산해 봅니다.

```sql
SELECT ST_Distance(
  'SRID=4326;POINT(-118.4079 33.9434)'::geometry,
  'SRID=4326;POINT(2.5559 49.0083)'::geometry
);
```

```text
121.898285970107
```

결과로 나온 **121.89**라는 숫자는 실제 미터나 킬로미터 거리가 아니라, 경위도 각도 평면에서의 **$121.89^\circ$(도)**를 의미합니다. 지구 곡률과 위도별 거리 차이가 전혀 반영되지 않은 무의미한 수치입니다.

실제 현실의 거리를 구하려면 구면 또는 회전타원체 상의 최단 경로인 **대권 항로(Great-Circle Arc)**를 따라 계산해야 합니다.

PostGIS는 이러한 측지선(Geodetic) 연산을 위해 **`geography` 타입**을 제공합니다.

```sql
SELECT ST_Distance(
  'SRID=4326;POINT(-118.4079 33.9434)'::geography,
  'SRID=4326;POINT(2.5559 49.0083)'::geography
);
```

```text
9124665.27317673
```

`geography` 타입을 사용한 연산 결과는 **미터(Meter)** 단위로 정확히 반환됩니다. 즉, LAX에서 CDG까지의 실제 비행 거리는 약 **9,125km**입니다.

---

## 대권 항로와 국제날짜변경선 문제

지오그래피의 진가는 선형 객체와 전 지구적 스케일의 공간 분석에서 더욱 두드러집니다.

> "로스앤젤레스(LAX)에서 파리(CDG)로 향하는 항공로는 아이슬란드(KEF)에 얼마나 가깝게 접근할까요?"

![이미지](geography/lax_cdg.jpg)

위 그림에서 평면 좌표계로 단순 직선을 그은 보라색 선은 실제 비행 경로와 완전히 다릅니다. 반면 타원체 곡면을 따른 대권 항로(붉은색 선)는 캐나다 북부와 그린란드를 거쳐 아이슬란드 인근을 통과합니다.

```sql
SELECT ST_Distance(
  ST_GeographyFromText('LINESTRING(-118.4079 33.9434, 2.5559 49.0083)'), -- LAX-CDG 항로
  ST_GeographyFromText('POINT(-22.6056 63.9850)')                         -- 아이슬란드 KEF 공항
) / 1000.0 AS distance_km;
```

```text
502.454906643729
```

실제 최단 접근 거리는 약 **502km**로 측정됩니다.

또한 평면 지오메트리는 **국제날짜변경선(경도 $\pm 180^\circ$)**을 가로지르는 순간 완전히 잘못된 경로를 계산합니다. 로스앤젤레스에서 도쿄(NRT)로 갈 때 평면 지오메트리는 대서양과 인도양을 거쳐 지구를 반대로 돌아가는 경로를 계산하지만, 지오그래피는 태평양을 건너는 실제 최단 거리를 정확히 계산합니다.

![이미지](geography/lax_nrt.png)

```sql
SELECT
  ST_Distance(
    ST_GeometryFromText('Point(-118.4079 33.9434)'),
    ST_GeometryFromText('Point(139.733 35.567)')
  ) AS geometry_degrees,
  ST_Distance(
    ST_GeographyFromText('Point(-118.4079 33.9434)'),
    ST_GeographyFromText('Point(139.733 35.567)')
  ) AS geography_meters;
```

```text
 geometry_degrees | geography_meters
------------------+------------------
 258.146005837336 |   8833954.76996256
```

---

## 지오그래피 테이블 생성 및 인덱싱

지오그래피 컬럼을 가진 테이블을 생성할 때는 `GEOGRAPHY(Type, SRID)` 문법을 사용합니다 (기본 SRID는 WGS84를 뜻하는 `4326`).

```sql
CREATE TABLE airports (
  code VARCHAR(3),
  geog GEOGRAPHY(Point, 4326)
);

INSERT INTO airports VALUES
  ('LAX', 'POINT(-118.4079 33.9434)'),
  ('CDG', 'POINT(2.5559 49.0083)'),
  ('KEF', 'POINT(-22.6056 63.9850)');
```

지오그래피 컬럼의 메타데이터는 `geometry_columns` 대신 **`geography_columns`** 뷰에 등록됩니다.

```sql
SELECT f_table_name, f_geography_column, srid, type
FROM geography_columns;
```

```text
 f_table_name | f_geography_column | srid | type
--------------+--------------------+------+-------
 airports     | geog               | 4326 | Point
```

지오그래피 테이블에도 동일하게 GiST 공간 인덱스를 생성할 수 있으며, 극지방과 날짜변경선을 가로지르는 쿼리도 안정적으로 가속합니다.

```sql
CREATE INDEX airports_geog_gix ON airports USING GIST (geog);
```

---

## geometry vs geography: 언제 무엇을 써야 할까?

| 비교 항목 | 지오메트리 (`geometry`) | 지오그래피 (`geography`) |
| :--- | :--- | :--- |
| **좌표계 모델** | 2D/3D 평면 데카르트 직교 좌표계 | 둥근 지구 타원체 (WGS84) 구면 좌표계 |
| **거리/면적 단위** | 투영 좌표계 단위 (보통 미터 또는 피트) | 항상 **미터(m)** 및 **제곱미터($\text{m}^2$)** |
| **연산 속도** | 단순 유클리드 기하학 공식 사용으로 **매우 빠름** | 복잡한 구면 삼각법/타원체 적분 공식으로 **연산 비용 높음** |
| **함수 지원 범위** | 수백 종의 모든 PostGIS 공간 함수 완전 지원 | 핵심 기본 함수(거리, 버퍼, 교차 등) 위주 지원 |
| **적합한 데이터** | 특정 시/도, 국가 단위의 **국소적 데이터셋** | 대륙 간, 전 지구적 스케일의 **글로벌 분산 데이터셋** |

> **선택 지침**:
> - 다루는 데이터가 특정 도시, 주, 카운티 등 제한된 지역에 한정되어 있다면, 해당 지역에 최적화된 평면 투영(State Plane, UTM 등)을 적용한 **`geometry` 타입**을 사용하는 것이 성능과 기능 면에서 훨씬 유리합니다.
> - 전 세계를 커버하거나 비행 항로, 대륙 간 거리 측정처럼 위도별 왜곡이 심한 데이터를 다룬다면 **`geography` 타입**을 사용하는 것이 안전합니다.

---

## 함수 목록 (Function List)

- [ST_Distance(geography, geography)](http://postgis.net/docs/ST_Distance.html): 두 지오그래피 객체 간의 회전타원체 최단 거리를 미터(m) 단위로 반환합니다.
- [ST_DWithin(geography, geography, radius_meters)](http://postgis.net/docs/ST_DWithin.html): 두 지오그래피 객체가 지정된 반경(미터) 이내에 있는지 인덱스를 활용하여 빠르게 검사합니다.
- [ST_Area(geography)](http://postgis.net/docs/ST_Area.html): 지오그래피 폴리곤의 면적을 제곱미터($\text{m}^2$) 단위로 반환합니다.
- [ST_Length(geography)](http://postgis.net/docs/ST_Length.html): 지오그래피 라인의 길이를 미터(m) 단위로 반환합니다.
- [ST_GeographyFromText(text)](http://postgis.net/docs/ST_GeographyFromText.html): WKT 문자열로부터 지오그래피 객체를 생성합니다.
- [ST_Azimuth(geography_A, geography_B)](http://postgis.net/docs/ST_Azimuth.html): 지오그래피 A에서 B를 향하는 방위각(Azimuth)을 북쪽 기준 라디안 단위로 반환합니다.


---

[← 이전](17_projection_exercises.md) · [목차](00_index.md) · [다음 →](19_geography_exercises.md)
