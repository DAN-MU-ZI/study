# 10. 지오메트리 실습 (Geometry Exercises)

지오메트리 생성 및 기본 속성 추출 함수들을 실습해 봅니다.

---

## 실습 문제

### 문제 1
**"Upper West Side" 이웃 지역의 면적(제곱미터)은 얼마인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT ST_Area(geom) AS area_sq_meters
FROM nyc_neighborhoods
WHERE name = 'Upper West Side';
```

**결과**: 약 `4842188` ㎡ (약 4.84 ㎢)
</details>

---

### 문제 2
**뉴욕시 전체 도로(`nyc_streets`)의 총 연장(길이, 킬로미터)은 얼마인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT SUM(ST_Length(geom)) / 1000.0 AS total_length_km
FROM nyc_streets;
```

**결과**: 약 `10421` km
</details>

---

### 문제 3
**"Columbus Circle" 지하철역의 X, Y 좌표는 각각 얼마인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  name,
  ST_X(geom) AS x_coord,
  ST_Y(geom) AS y_coord
FROM nyc_subway_stations
WHERE name = 'Columbus Circle';
```

**결과**: X = `586725.89...`, Y = `4513364.63...` (SRID 26918 기준)
</details>

---

### 문제 4
**"Central Park"의 지오메트리 타입과 구성 정점(Vertex)의 개수는 몇 개인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT
  ST_GeometryType(geom) AS geom_type,
  ST_NPoints(geom) AS vertex_count
FROM nyc_neighborhoods
WHERE name = 'Central Park';
```

**결과**: `ST_MultiPolygon`, 정점 수 `67`개
</details>

---

| [⬅️ 9. 지오메트리 (Geometries)](09_geometries.md) | [🏠 워크숍 목차](README.md) | [11. 공간 관계 (Spatial Relationships) ➡️](11_spatial_relationships.md) |
| :--- | :---: | ---: |
