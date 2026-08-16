# 19. 지오그래피 실습 (Geography Exercises)

`geography` 타입과 구면 거리 연산을 활용한 실습 문제입니다.

---

## 실습 문제

### 문제 1
**서울(경도 126.9780, 위도 37.5665)과 뉴욕 JFK 공항(경도 -73.7789, 위도 40.6397) 사이의 대권 거리는 몇 킬로미터인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT ST_Distance(
  'SRID=4326;POINT(126.9780 37.5665)'::geography,
  'SRID=4326;POINT(-73.7789 40.6397)'::geography
) / 1000.0 AS distance_km;
```

**결과**: 약 `11050` km
</details>

---

### 문제 2
**뉴욕 "Times Square" 지하철역의 geom(26918)을 WGS84 geography로 변환하여 1,000미터 이내에 있는 다른 지하철역의 개수를 구하세요.**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT count(*)
FROM nyc_subway_stations s, nyc_subway_stations ts
WHERE ts.name = 'Times Sq - 42 St'
  AND ST_DWithin(
    ST_Transform(s.geom, 4326)::geography,
    ST_Transform(ts.geom, 4326)::geography,
    1000
  );
```
</details>

---

| [⬅️ 18. 지오그래피 (Geography)](18_geography.md) | [🏠 워크숍 목차](README.md) | [20. 지오메트리 생성 함수 (Geometry Constructing Functions) ➡️](20_geometry_returning.md) |
| :--- | :---: | ---: |
