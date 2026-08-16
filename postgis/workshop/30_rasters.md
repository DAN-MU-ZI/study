# 30. 래스터 (Rasters)

PostGIS는 점, 선, 면과 같은 벡터(Vector) 데이터뿐만 아니라, 위성 사진, 항공 사진, 수치표고모델(DEM), 기상 데이터와 같은 격자형 **래스터(Raster)** 데이터 타입을 완벽하게 지원합니다 (`postgis_raster` 확장).

![래스터 데이터 개념도](rasters/postgis_raster.jpg)

---

## 1. PostGIS Raster 확장 활성화

```sql
CREATE EXTENSION postgis_raster;
```

---

## 2. raster2pgsql 도구를 통한 래스터 로딩

`raster2pgsql`은 GeoTIFF 등의 래스터 파일을 타일(Tile) 단위로 분할하여 PostGIS 테이블로 로드하는 유틸리티입니다.

```bash
# 100x100 픽셀 타일로 분할하여 인덱스(-I)와 함께 로드
raster2pgsql -s 26918 -I -C -M -t 100x100 ny_dem.tif nyc_dem | psql -U postgres -d nyc
```

- `-s <SRID>`: 좌표계 지정
- `-t <WxH>`: 래스터 타일 크기 (타일 단위로 나누어 저장해야 초고속 공간 인덱싱 가능)
- `-C`: 공간 참조 및 해상도 제약조건 자동 추가
- `-I`: GiST 공간 인덱스 자동 생성

---

## 3. 래스터 분석 함수

- `ST_Value(rast, band, pt_geom)`: 특정 지점(Point) 위치의 픽셀 값(예: 고도, 온도, 반사율) 추출
- `ST_Slope(rast, ...)`: 고도 래스터(DEM)로부터 경사도 래스터 자동 계산
- `ST_Aspect(rast, ...)`: 사면 방위각 계산
- `ST_Hillshade(rast, ...)`: 지형 음영기복도(Hillshade) 생성
- `ST_Clip(rast, geom)`: 벡터 폴리곤 영역에 맞춰 래스터 잘라내기
- `ST_Intersection(rast, geom)`: 래스터와 벡터 간의 교차 통계 분석 (Zonal Statistics)

```sql
-- 특정 지하철역 위치의 해발 고도(Elevation) 추출
SELECT
  s.name,
  ST_Value(r.rast, 1, s.geom) AS elevation_meters
FROM nyc_subway_stations s
JOIN nyc_dem r
  ON ST_Intersects(r.rast, s.geom)
LIMIT 5;
```

---

| [⬅️ 29. 최근접 이웃 탐색 (Nearest-Neighbour Searching / KNN)](29_knn.md) | [🏠 워크숍 목차](README.md) | [31. 토폴로지 (Topology) ➡️](31_topology.md) |
| :--- | :---: | ---: |
