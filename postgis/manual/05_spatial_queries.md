# 제 5 장. 공간 쿼리 및 분석 (Spatial Queries)

PostGIS 공간 쿼리의 핵심 메커니즘과 공간 인덱스(GiST) 활용 기법입니다.

---

## 5.1. 공간 인덱스(GiST) 생성

```sql
CREATE INDEX airports_geom_idx
ON airports
USING GIST (geom);
```

---

## 5.2. 인덱스 기반 공간 검색 연산자

- `geom1 && geom2`: 두 객체의 최소 경계 사각형(Bounding Box)이 교차하는지 검사 (인덱스 1차 필터링)
- `geom1 <-> geom2`: 두 객체의 바운딩 박스 간 거리를 인덱스 기반으로 측정 (KNN 초고속 정렬)

```sql
-- 지정한 바운딩 박스 영역 내에 있는 공항 검색
SELECT name, iata_code
FROM airports
WHERE geom && ST_MakeEnvelope(126.0, 37.0, 128.0, 38.0, 4326);
```

---

| [⬅️ 제 4 장. 공간 데이터 관리 (Data Management)](04_data_management.md) | [🏠 매뉴얼 목차](README.md) | [제 6 장. 성능 팁 및 튜닝 (Performance Tips) ➡️](06_performance_tips.md) |
| :--- | :---: | ---: |
