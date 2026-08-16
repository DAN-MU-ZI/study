# 17. 투영 실습 (Projection Exercises)

좌표계 변환(`ST_Transform`)을 활용한 실습 문제입니다.

---

## 실습 문제

### 문제 1
**"Broad St" 지하철역의 WGS84 경도(Longitude)와 위도(Latitude) 값은 각각 얼마인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  name,
  ST_X(ST_Transform(geom, 4326)) AS longitude,
  ST_Y(ST_Transform(geom, 4326)) AS latitude
FROM nyc_subway_stations
WHERE name = 'Broad St';
```

**결과**: 경도 약 `-74.011...`, 위도 약 `40.707...`
</details>

---

### 문제 2
**뉴욕시 인구조사 블록 데이터를 전 세계 웹 지도 표준인 EPSG:3857(Web Mercator)로 변환했을 때, "Central Park"의 중심점 좌표(X, Y)는 얼마인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  name,
  ST_AsText(ST_Centroid(ST_Transform(geom, 3857))) AS web_mercator_centroid
FROM nyc_neighborhoods
WHERE name = 'Central Park';
```
</details>

---

| [⬅️ 16. 데이터 투영 (Projecting Data)](16_projection.md) | [🏠 워크숍 목차](README.md) | [18. 지오그래피 (Geography) ➡️](18_geography.md) |
| :--- | :---: | ---: |
