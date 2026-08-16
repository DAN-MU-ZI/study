# 제 4 장. 공간 데이터 관리 (Data Management)

공간 데이터 테이블 설계, 좌표계(SRID) 정의, 메타데이터 뷰 활용법입니다.

---

## 4.1. 공간 테이블 생성 문법

PostgreSQL의 표준 `CREATE TABLE` 문에서 `geometry` 또는 `geography` 타입을 지정합니다:

```sql
CREATE TABLE public.airports (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100),
  iata_code CHAR(3),
  geom GEOMETRY(Point, 4326) -- WGS84 좌표계 Point 지오메트리
);
```

다양한 지오메트리 타입 제약:
- `GEOMETRY(LineString, 26918)`: UTM 18N 선형 지오메트리
- `GEOMETRY(MultiPolygon, 5179)`: UTM-K 다중 폴리곤 지오메트리
- `GEOGRAPHY(Point, 4326)`: 구면 지오그래피 포인트

---

## 4.2. 메타데이터 뷰 (`geometry_columns`)

데이터베이스 내의 모든 지오메트리 컬럼 정보는 `geometry_columns` 뷰를 통해 일괄 조회할 수 있습니다:

```sql
SELECT f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type
FROM geometry_columns;
```

---

| [⬅️ 제 3 장. PostGIS 관리 (Administration)](03_administration.md) | [🏠 매뉴얼 목차](README.md) | [제 5 장. 공간 쿼리 및 분석 (Spatial Queries) ➡️](05_spatial_queries.md) |
| :--- | :---: | ---: |
