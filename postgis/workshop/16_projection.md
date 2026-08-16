# 16. 데이터 투영 (Projecting Data)

지구는 둥근 타원체(3D)이지만, 지도와 화면은 평면(2D)입니다. 3차원 지구 표면의 좌표를 2차원 평면으로 변환하는 과정을 **지도 투영(Map Projection)**이라고 합니다.

모든 공간 데이터는 자신이 어떤 기준계로 정의되었는지를 나타내는 **SRID (Spatial Reference System Identifier)**를 가집니다.

---

## 1. 대표적인 좌표계

- **EPSG:4326 (WGS 84)**: GPS에서 사용하는 전 지구 표준 경위도 좌표계 (단위: Degree, 도)
- **EPSG:3857 (Web Mercator / Pseudo-Mercator)**: 구글 지도, OSM, 카카오맵, 네이버맵 등 웹 지도 서비스의 표준 투영 좌표계 (단위: 미터)
- **EPSG:26918 (NAD83 / UTM Zone 18N)**: 뉴욕 지역에 최적화된 UTM 평면 직각 투영 좌표계 (단위: 미터)
- **EPSG:5179 / EPSG:5186 등**: 대한민국 국가 표준 평면 직각 좌표계 (UTM-K, 중부원점 등)

---

## 2. 좌표계 변환 함수 (`ST_Transform`)

`ST_Transform(geom, target_srid)` 함수를 사용하여 지오메트리의 좌표계를 다른 SRID로 실시간 변환할 수 있습니다.

```sql
-- UTM Zone 18N (26918) 좌표를 표준 WGS84 경위도(4326)로 변환하여 조회
SELECT
  name,
  ST_AsText(ST_Transform(geom, 4326)) AS wgs84_coords
FROM nyc_subway_stations
LIMIT 1;
```

출력 결과:
```text
POINT(-73.985130 40.748817) -- (경도, 위도)
```

---

## 3. 좌표계 메타데이터 확인 및 설정

- `ST_SRID(geom)`: 데이터의 현재 SRID 반환
- `ST_SetSRID(geom, srid)`: 데이터의 좌표 숫자는 그대로 두고 메타데이터의 SRID 라벨만 변경 (좌표 변환이 아님에 주의)

> [!WARNING]
> `ST_SetSRID`는 잘못 지정된 SRID 라벨을 바로잡을 때 사용하며, 실제 투영 변환에는 반드시 `ST_Transform`을 사용해야 합니다!

---

| [⬅️ 15. 공간 인덱싱 (Spatial Indexing)](15_indexing.md) | [🏠 워크숍 목차](README.md) | [17. 투영 실습 (Projection Exercises) ➡️](17_projection_exercises.md) |
| :--- | :---: | ---: |
