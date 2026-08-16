# 19. 지리 연습 (Geography Exercises)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/geography_exercises.html>](https://postgis.net/workshops/postgis-intro/geography_exercises.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

지금까지 본 모든 기능을 상기시켜 보겠습니다. 운동에 유용할 것 같아요!

- `Sum(number)`는 결과 집합의 모든 숫자를 더합니다.
- `ST_GeogFromText(text)`는 지리를 반환합니다.
- `ST_Distance(geography, geography)`는 지역 간 거리를 반환합니다.
- `ST_Transform(geometry, srid)`는 새 투영에서 형상을 반환합니다.
- `ST_Length(geography)`는 줄의 길이를 반환합니다.
- 객체가 평면 공간에서 분리되지 않은 경우 `ST_Intersects(geometry, geometry)`는 true를 반환합니다.
- `ST_Intersects(geography, geography)`는 객체가 회전타원체 공간에서 분리되지 않은 경우 true를 반환합니다.

또한 사용 가능한 테이블을 기억하십시오.

- `nyc_streets`
  - 이름, 유형, 지리
- `nyc_neighborhoods`
  - 이름, 보로나메, 검

## 연습

- **뉴욕은 시애틀에서 얼마나 멀나요? 답의 단위는 무엇입니까?**

  > [!NOTE]
  > 뉴욕 = `POINT(-74.0064 40.7142)`, 시애틀 = `POINT(-122.3331 47.6097)`

  ```sql
  SELECT ST_Distance(
    'POINT(-74.0064 40.7142)'::geography,
    'POINT(-122.3331 47.6097)'::geography
    );
  ```

      3875538.57141352

- **뉴욕의 모든 거리의 총 길이는 회전타원체로 계산하면 얼마입니까?**

  ```sql
  SELECT Sum(
    ST_Length(Geography(
      ST_Transform(geom,4326)
    )))
  FROM nyc_streets;
  ```

      10421999.666

  > [!NOTE]
  > 평면 "UTM Zone 18" 투영에서 계산된 길이는 10418904.717로 0.02% 다릅니다. UTM은 구역 경계 내에서 면적과 거리를 보존하는 데 적합합니다.

- **'POINT(1 2.0001)'는 지리학적으로 'POLYGON((0 0, 0 2, 2 2, 2 0, 0 0))'과 교차합니까? 기하학에서? 왜 차이점이 있나요?**

  ```sql
  SELECT ST_Intersects(
    'POINT(1 2.0001)'::geography,
    'POLYGON((0 0,0 2,2 2,2 0,0 0))'::geography
  );

  SELECT ST_Intersects(
    'POINT(1 2.0001)'::geometry,
    'POLYGON((0 0,0 2,2 2,2 0,0 0))'::geometry
  );
  ```

      true and false

  > [!NOTE]
  > 사각형의 위쪽 가장자리는 기하학에서 직선이고 **below** 점을 통과하므로 사각형에는 점이 포함되지 않습니다. 사각형의 위쪽 가장자리는 지리학적으로 큰 원이고 **above** 점을 통과하므로 사각형에는 점이 포함됩니다.

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

```sql
SELECT ST_Distance(
  'SRID=4326;POINT(126.9780 37.5665)'::geography,
  'SRID=4326;POINT(-73.7789 40.6397)'::geography
) / 1000.0 AS distance_km;
```

**결과**: 약 `11050` km

</details>

---

[← 이전](18_geography.md) · [목차](00_index.md) · [다음 →](20_geometry_returning.md)