# 25. 선형 참조 (Linear Referencing)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/linear_referencing.html>](https://postgis.net/workshops/postgis-intro/linear_referencing.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

선형 참조(때때로 "동적 분할"이라고도 함)는 선형 특징의 기본 세트를 참조하여 설명할 수 있는 특징을 나타내는 수단입니다. 선형 참조를 사용하여 모델링되는 기능의 일반적인 예는 다음과 같습니다.

- 고속도로 네트워크를 따라 마일을 사용하여 참조되는 고속도로 자산
- 한 쌍의 마일 측정 사이의 도로 네트워크를 따라 발생하는 것으로 참조되는 도로 유지 관리 작업.
- 어류의 존재가 마일리지 상류 측정 쌍 사이에 존재하는 것으로 기록되는 수생 목록입니다.
- 시작 및 종료 마일리지로 기록된 하천의 수문학적 특성("도달 범위").

선형 참조 모델의 이점은 종속 공간 관측이 기본 관측과 별도로 기록될 필요가 없으며 종속 관측이 자동으로 새 형상을 추적한다는 것을 알고 기본 관측 레이어에 대한 업데이트를 수행할 수 있다는 것입니다.

> [!NOTE]
> 선형 참조에 대한 Esri 용어 규칙은 선형 공간 특성의 기본 테이블과 공간 특성에 대한 외래 키 참조 및 참조된 특성에 따른 측정값을 포함하는 "이벤트"의 비공간 테이블을 갖는 것입니다. 우리가 구축한 비공간 테이블을 지칭하기 위해 "이벤트 테이블"이라는 용어를 사용할 것입니다.

## 선형 참조 생성

선형 네트워크를 참조하려는 기존 포인트 테이블이 있는 경우 선과 점을 가져와 선을 따라 점을 찾을 수 있는 비율을 반환하는 `ST_LineLocatePoint` 함수를 사용하십시오.

```sql
-- Simple example of locating a point half-way along a line
SELECT ST_LineLocatePoint('LINESTRING(0 0, 2 2)', 'POINT(1 1)');
-- Answer 0.5

-- What if the point is not on the line? It projects to closest point
SELECT ST_LineLocatePoint('LINESTRING(0 0, 2 2)', 'POINT(0 2)');
-- Answer 0.5
```

`ST_LineLocatePoint`를 사용하여 **nyc_subway_stations**를 거리와 관련된 "이벤트 테이블"로 변환할 수 있습니다.

```sql
-- All the SQL below is in aid of creating the new event table
CREATE TABLE nyc_subway_station_events AS
-- We first need to get a candidate set of maybe-closest
-- streets, ordered by id and distance...
WITH ordered_nearest AS (
SELECT
  ST_GeometryN(streets.geom,1) AS streets_geom,
  streets.gid AS streets_gid,
  subways.geom AS subways_geom,
  subways.gid AS subways_gid,
  ST_Distance(streets.geom, subways.geom) AS distance
FROM nyc_streets streets
  JOIN nyc_subway_stations subways
  ON ST_DWithin(streets.geom, subways.geom, 200)
ORDER BY subways_gid, distance ASC
)
-- We use the 'distinct on' PostgreSQL feature to get the first
-- street (the nearest) for each unique street gid. We can then
-- pass that one street into ST_LineLocatePoint along with
-- its candidate subway station to calculate the measure.
SELECT
  DISTINCT ON (subways_gid)
  subways_gid,
  streets_gid,
  ST_LineLocatePoint(streets_geom, subways_geom) AS measure,
  distance
FROM ordered_nearest;

-- Primary keys are useful for visualization softwares
ALTER TABLE nyc_subway_station_events ADD PRIMARY KEY (subways_gid);
```

이벤트 테이블이 있으면 다시 공간 뷰로 전환하여 이벤트가 파생된 원래 지점을 기준으로 이벤트를 시각화할 수 있습니다.

측정값에서 특정 지점으로 이동하려면 `ST_LineInterpolatePoint` 기능을 사용합니다. 이전의 간단한 예를 거꾸로 보면 다음과 같습니다.

```sql
-- Simple example of locating a point half-way along a line
SELECT ST_AsText(ST_LineInterpolatePoint('LINESTRING(0 0, 2 2)', 0.5));

-- Answer POINT(1 1)
```

그리고 **nyc_subway_station_events** 테이블을 **nyc_streets** 테이블에 다시 조인하고 **measure** 속성을 사용하여 원래 **nyc_subway_stations** 테이블을 참조하지 않고 공간 이벤트 포인트를 생성할 수 있습니다.

```sql
-- New view that turns events back into spatial objects
CREATE OR REPLACE VIEW nyc_subway_stations_lrs AS
SELECT
  events.subways_gid,
  ST_LineInterpolatePoint(ST_GeometryN(streets.geom, 1), events.measure)AS geom,
  events.streets_gid
FROM nyc_subway_station_events events
JOIN nyc_streets streets
ON (streets.gid = events.streets_gid);
```

거리와 함께 원본(빨간색 별) 및 이벤트(파란색 원) 지점을 보면 이벤트가 가장 가까운 거리 선에 직접 스냅되는 방식을 확인할 수 있습니다.

![이미지](screenshots/lrs1.jpg)

> [!NOTE]
> 선형 참조 기능의 놀라운 용도 중 하나는 선형 참조 모델과 아무 관련이 없습니다. 위에 표시된 대로 기능을 사용하여 점을 선형 기능에 맞추는 것이 가능합니다. 선형 네트워크를 참조해야 하는 GPS 트랙이나 기타 입력과 같은 사용 사례의 경우 스냅은 사용 가능한 편리한 기능입니다.

## 기능 목록

- [ST_LineInterpolatePoint(기하학 A, 이중 측정)](http://postgis.net/docs/ST_LineInterpolatePoint.html): 선을 따라 보간된 점을 반환합니다.

- [ST_LineLocatePoint(기하학 A, 기하학 B)](http://postgis.net/docs/ST_LineLocatePoint.html): LineString에서 지정된 점에 가장 가까운 점의 위치를 ​​나타내는 0과 1 사이의 부동 소수점을 반환합니다.

- [ST_LineSubstring(geometry A, double from, double to)](http://postgis.net/docs/ST_LineSubstring.html): 총 2d 길이의 주어진 분수에서 시작하고 끝나는 입력 문자열의 하위 문자열인 라인스트링을 반환합니다.

- [ST_LocateAlong(기하학 A, 이중 측정)](http://postgis.net/docs/ST_LocateAlong.html): 지정된 측정과 일치하는 요소가 포함된 파생된 기하학 컬렉션 값을 반환합니다.

- [ST_LocateBetween(geometry A, double from, double to)](http://postgis.net/docs/ST_LocateBetween.html): 지정된 측정 범위와 전체적으로 일치하는 요소가 포함된 파생된 기하학 컬렉션 값을 반환합니다.

- [ST_AddMeasure(기하형 A, double from, double to)](http://postgis.net/docs/ST_AddMeasure.html): 선스트링 또는 다중 선스트링을 **측정된 경로**(M 값)로 변환합니다. **집 번호**를 처음부터 끝까지 점진적으로 할당하려는 **road**라고 생각하세요. 이 함수는 선을 따라 선형적으로 증가하는 "측정" 값(집 번호, 킬로미터, 이동 시간 등)을 할당합니다. 기하학에 **M 좌표가 없으면** 추가됩니다. **이미 M 값**이 있는 경우 해당 값을 덮어씁니다. LINESTRING 및 MULTILINESTRING만 지원됩니다.

  **예(집 번호):** START(집 10) --- 중간 지점 --- END(집 100) → 각 지점은 비례 값을 갖습니다.


---

[← 이전](24_equality.md) · [목차](00_index.md) · [다음 →](26_de9im.md)