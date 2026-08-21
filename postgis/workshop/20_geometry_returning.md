# 20. 지오메트리 생성 함수 (Geometry Constructing Functions)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geometry_returning.html>](https://postgis.net/workshops/postgis-intro/geometry_returning.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지금까지 다룬 공간 함수들은 입력 지오메트리의 형태를 바꾸지 않고 속성을 측정(`ST_Area`, `ST_Length`)하거나, 포맷을 직렬화(`ST_AsText`)하거나, 공간적 관계를 검사(`ST_Intersects`, `ST_Contains`)하는 용도였습니다.

이번 장에서 다룰 **지오메트리 생성 및 가공 함수(Geometry Constructing Functions)**는 기존 지오메트리를 입력받아 새로운 형태의 지오메트리(중심점, 버퍼 영역, 교집합, 합집합 등)를 연산하여 출력합니다.

---

## 1. 중심점과 표면 내부점: ST_Centroid vs ST_PointOnSurface

폴리곤 피처를 대표하는 단일 포인트(Proxy Point)를 추출해야 할 때가 많습니다. 특히 두 개의 폴리곤 레이어를 공간 조인할 때 경계선에 걸친 폴리곤들이 양쪽 모두와 교차하여 중복 집계되는 문제를 방지할 때 유용합니다.

- **`ST_Centroid(geometry)`**: 입력 지오메트리의 기하학적 무게중심(질량 중심) 점을 반환합니다. 계산 속도가 매우 빠르지만, 'C'자 형태나 도넛 형태처럼 오목한(Concave) 폴리곤의 경우 무게중심이 폴리곤 외부에 위치할 수 있습니다.
- **`ST_PointOnSurface(geometry)`**: 계산 결과 점이 **반드시 입력 폴리곤 내부에 위치함이 보장**되는 대표점을 반환합니다. 공간 조인의 중복을 방지하는 프록시 포인트로 사용하기에 가장 안전합니다.

![image](geometry_returning/centroid.jpg)

```sql
-- 오목한 'C'자 형태의 폴리곤에서 Centroid와 PointOnSurface 비교
SELECT
  ST_Intersects(geom, ST_Centroid(geom)) AS centroid_inside,
  ST_Intersects(geom, ST_PointOnSurface(geom)) AS pos_inside
FROM (VALUES
  ('POLYGON ((30 0, 30 10, 10 10, 10 40, 30 40, 30 50, 0 50, 0 0, 30 0))'::geometry)
) AS t(geom);
```

```text
 centroid_inside | pos_inside
-----------------+------------
 f               | t
```

---

## 2. 완충 구역 생성: ST_Buffer

**버퍼링(Buffering)**은 공간 객체로부터 일정 거리만큼 확장된 영역 폴리곤을 생성하는 대표적인 GIS 연산입니다.

`ST_Buffer(geometry, distance)`는 입력 지오메트리와 버퍼 반경 거리를 받아 해당 거리만큼 확장된 외곽 폴리곤을 생성합니다.

![image](geometry_returning/st_buffer.png)

> **활용 예시**: 미국 국립공원관리청이 자유의 여신상이 위치한 리버티 섬(Liberty Island) 주변 500m 해상 통제 구역을 설정하는 경우

```sql
-- Liberty Island 주변 500m 완충 구역 테이블 생성
CREATE TABLE liberty_island_zone AS
SELECT ST_Buffer(geom, 500)::geometry(Polygon, 26918) AS geom
FROM nyc_census_blocks
WHERE blkid = '360610001001001';
```

![image](geometry_returning/liberty_positive.jpg)

`ST_Buffer`에 음수(`-`) 거리를 입력하면 폴리곤 내부로 축소된 내접 폴리곤(Inscribed Polygon)을 생성합니다. (점이나 선에 음수 버퍼를 적용하면 빈 지오메트리가 반환됩니다.)

![image](geometry_returning/liberty_negative.jpg)

---

## 3. 공간 교집합: ST_Intersection

**오버레이(Overlay)** 분석의 핵심으로, 두 지오메트리가 서로 겹치는 공통 영역(점, 선, 면)만을 잘라내어 새로운 지오메트리로 추출합니다.

`ST_Intersection(geometry A, geometry B)`는 두 객체의 공통 공간을 반환하며, 겹치지 않으면 빈 지오메트리(`GEOMETRYCOLLECTION EMPTY`)를 반환합니다.

```sql
-- 두 원형 버퍼 간의 겹치는 교집합 영역 구하기
SELECT ST_AsText(
  ST_Intersection(
    ST_Buffer('POINT(0 0)'::geometry, 2),
    ST_Buffer('POINT(3 0)'::geometry, 2)
  )
);
```

![image](geometry_returning/intersection.jpg)

---

## 4. 공간 합집합: ST_Union

`ST_Union`은 입력된 여러 지오메트리들을 하나로 결합하고 내부의 공유 경계선을 제거하여 통합된 단일 지오메트리를 생성합니다.

### 1) 2항 함수 버전: `ST_Union(geometry A, geometry B)`
두 개의 지오메트리를 합쳐 병합된 결과를 반환합니다.

```sql
SELECT ST_AsText(
  ST_Union(
    ST_Buffer('POINT(0 0)'::geometry, 2),
    ST_Buffer('POINT(3 0)'::geometry, 2)
  )
);
```

![image](geometry_returning/union.jpg)

### 2) 집계 함수 버전: `ST_Union(geometry set)`
`GROUP BY`와 함께 사용하여 그룹에 속한 수많은 행의 지오메트리를 하나로 병합합니다.

> **활용 예시**: 인구조사 블록(`nyc_census_blocks`)들을 카운티(County) 단위로 병합하여 뉴욕시 5개 카운티 지도를 구축

미국 인구조사 블록의 `blkid` 15자리 코드 중 앞 5자리는 주(36)와 카운티 번호를 나타냅니다 (예: `36061` = 맨해튼 카운티).

```sql
-- 인구조사 블록을 카운티 ID별로 병합하여 nyc_census_counties 테이블 생성
CREATE TABLE nyc_census_counties AS
SELECT
  ST_Union(geom)::Geometry(MultiPolygon, 26918) AS geom,
  substr(blkid, 1, 5) AS countyid
FROM nyc_census_blocks
GROUP BY countyid;
```

![image](geometry_returning/union_counties.png)

병합 전후의 면적을 비교하면 지오메트리가 소실 없이 정확하게 합쳐졌음을 검증할 수 있습니다.

```sql
-- 1. 개별 블록 면적의 합계
SELECT substr(blkid, 1, 5) AS countyid, sum(ST_Area(geom)) AS area
FROM nyc_census_blocks
GROUP BY countyid
ORDER BY countyid;

-- 2. 병합된 카운티 폴리곤의 면적
SELECT countyid, ST_Area(geom) AS area
FROM nyc_census_counties
ORDER BY countyid;
```

```text
 countyid |       area
----------+------------------
 36005    | 110196022.906507
 36047    | 181927497.678367
 36061    | 59091860.6261324
 36081    | 283194473.593646
 36085    | 150758328.111199
```

---

## 함수 목록 (Function List)

- [ST_Buffer(geometry, distance)](http://postgis.net/docs/ST_Buffer.html): 입력 지오메트리로부터 지정된 거리만큼 확장(또는 축소)된 완충 구역 폴리곤을 반환합니다.
- [ST_Centroid(geometry)](http://postgis.net/docs/ST_Centroid.html): 지오메트리의 무게중심(질량 중심) 포인트를 반환합니다.
- [ST_Intersection(geometry A, geometry B)](http://postgis.net/docs/ST_Intersection.html): 두 지오메트리가 공통으로 공유하는 영역(교집합)을 반환합니다.
- [ST_PointOnSurface(geometry)](http://postgis.net/docs/ST_PointOnSurface.html): 입력 지오메트리의 표면 내부(Interior)에 위치함이 보장되는 대표 포인트를 반환합니다.
- [ST_Union(geometry A, geometry B)](http://postgis.net/docs/ST_Union.html): 두 지오메트리를 병합하여 공통 경계를 제거한 합집합 지오메트리를 반환합니다.
- [ST_Union(geometry set)](http://postgis.net/docs/ST_Union.html): `GROUP BY`와 함께 집계 함수로 동작하며, 그룹 내 모든 지오메트리를 하나로 병합합니다.


---

[← 이전](19_geography_exercises.md) · [목차](00_index.md) · [다음 →](21_geometry_returning_exercises.md)
