# 21. 지오메트리 생성 실습 (Geometry Constructing Exercises)

`ST_Centroid`, `ST_Buffer`, `ST_Union` 등을 활용한 지오메트리 생성 실습 문제입니다.

---

## 실습 문제

### 문제 1
**모든 지하철역 주변 500미터 버퍼 영역들의 전체 합집합(Union) 면적(㎢)은 얼마인가요? (중복 구역 단일화)**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT ST_Area(ST_Union(ST_Buffer(geom, 500))) / 1000000.0 AS subway_coverage_sq_km
FROM nyc_subway_stations;
```

**결과**: 약 `235.8` ㎢
</details>

---

### 문제 2
**맨해튼(Manhattan) 자치구 전체의 기하학적 중심점(Centroid) 좌표는 어디인가요?**

<details>
<summary><b>정답 확인 (SQL)</b></summary>

```sql
SELECT ST_AsText(ST_Centroid(ST_Union(geom))) AS manhattan_centroid
FROM nyc_census_blocks
WHERE boroname = 'Manhattan';
```
</details>

---

| [⬅️ 20. 지오메트리 생성 함수 (Geometry Constructing Functions)](20_geometry_returning.md) | [🏠 워크숍 목차](README.md) | [22. 고급 공간 조인 (More Spatial Joins) ➡️](22_joins_advanced.md) |
| :--- | :---: | ---: |
