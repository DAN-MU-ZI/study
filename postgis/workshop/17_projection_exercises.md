# 17. 투영 실습 (Projection Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/projection_exercises.html>](https://postgis.net/workshops/postgis-intro/projection_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

앞서 학습한 투영 및 좌표 변환 함수들을 활용하여 다음 실습 문제를 직접 해결해 보세요.

### 실습 참조 함수 요약
- `sum(expression)`: 수치 집합의 총합계
- `ST_Length(linestring)`: 선의 2차원 길이 계산
- `ST_SRID(geometry)`: 지오메트리의 SRID 번호 반환
- `ST_Transform(geometry, srid)`: 다른 좌표계로 재투영 변환
- `ST_GeomFromText(text)`: WKT 문자열로부터 지오메트리 생성
- `ST_Segmentize(geometry, max_segment_length)`: 선분에 중간 정점들을 촘촘하게 추가하여 분할

### 실습 대상 테이블
- `nyc_streets`: `name`, `type`, `geom` (SRID: 26918)
- `nyc_neighborhoods`: `name`, `boroname`, `geom` (SRID: 26918)

---

## 연습 문제 및 정답

### 1. 현재 UTM Zone 18N (SRID 26918) 좌표계로 측정한 뉴욕시 전체 도로의 총 연장은 얼마입니까?

```sql
SELECT sum(ST_Length(geom)) AS total_length_meters
FROM nyc_streets;
```

```text
10418904.7172
```

---

### 2. SRID 2831의 WKT(Well-Known Text) 좌표계 정의는 어떻게 됩니까?

```sql
SELECT srtext
FROM spatial_ref_sys
WHERE srid = 2831;
```

또는 온라인 좌표계 레퍼런스 사이트(<https://epsg.io/2831>)에서 확인:

```text
PROJCS["NAD83(HARN) / New York Long Island",
  GEOGCS["NAD83(HARN)",
    DATUM["NAD83 (High Accuracy Regional Network)",
      SPHEROID["GRS 1980", 6378137.0, 298.257222101,
        AUTHORITY["EPSG","7019"]],
      TOWGS84[-0.991, 1.9072, 0.5129, 0.0257899075194932, -0.009650098960270402, -0.011659943232342112, 0.0],
      AUTHORITY["EPSG","6152"]],
    PRIMEM["Greenwich", 0.0,
      AUTHORITY["EPSG","8901"]],
    UNIT["degree", 0.017453292519943295],
    AXIS["Geodetic longitude", EAST],
    AXIS["Geodetic latitude", NORTH],
    AUTHORITY["EPSG","4152"]],
  PROJECTION["Lambert Conic Conformal (2SP)",
    AUTHORITY["EPSG","9802"]],
  PARAMETER["central_meridian", -74.0],
  PARAMETER["latitude_of_origin", 40.166666666666664],
  PARAMETER["standard_parallel_1", 41.03333333333333],
  PARAMETER["false_easting", 300000.0],
  PARAMETER["false_northing", 0.0],
  PARAMETER["scale_factor", 1.0],
  PARAMETER["standard_parallel_2", 40.666666666666664],
  UNIT["m", 1.0],
  AXIS["Easting", EAST],
  AXIS["Northing", NORTH],
  AUTHORITY["EPSG","2831"]]
```

---

### 3. 뉴욕 롱아일랜드 지역 평면 투영 좌표계(SRID 2831)로 재투영하여 측정한 뉴욕시 전체 도로의 총 연장은 얼마입니까?

```sql
SELECT sum(ST_Length(ST_Transform(geom, 2831))) AS total_length_meters
FROM nyc_streets;
```

```text
10421993.706374
```

> [!NOTE]
> UTM Zone 18N 투영(10,418,904m)과 뉴욕 롱아일랜드 State Plane 투영(10,421,993m) 간의 차이는 약 $0.02\%$에 불과합니다. State Plane 투영은 뉴욕시와 롱아일랜드 지역에 특화되어 최적 보정된 투영법이므로 해당 국소 지역에서는 UTM보다 더 정확한 거리 측정을 제공합니다.

---

### 4. 서경 74도 자오선(Longitude $-74^\circ$)을 가로지르는 도로는 총 몇 개입니까?

**방법 A (경위도 EPSG:4326으로 변환하여 교차 검사)**:

```sql
SELECT count(*)
FROM nyc_streets
WHERE ST_Intersects(
  ST_Transform(geom, 4326),
  'SRID=4326;LINESTRING(-74 20, -74 60)'
);
```

```text
223
```

**방법 B (자오선 선분을 촘촘하게 분할(Segmentize)한 후 UTM 26918로 변환하여 교차 검사)**:

```sql
SELECT count(*)
FROM nyc_streets
WHERE ST_Intersects(
  geom,
  ST_Transform(
    ST_Segmentize('SRID=4326;LINESTRING(-74 20, -74 60)'::geometry, 0.001),
    26918
  )
);
```

```text
223
```

> [!NOTE]
> 2차원 평면 투영 공간에서 경도 자오선은 완만한 곡선으로 투영됩니다. 따라서 단 2개의 점으로 이루어진 직선을 그대로 투영하면 곡률 왜곡이 발생하므로, `ST_Segmentize`로 중간 정점들을 촘촘하게 생성한 후 투영해야 정확한 교차 결과를 얻을 수 있습니다.


---

[← 이전](16_projection.md) · [목차](00_index.md) · [다음 →](18_geography.md)
