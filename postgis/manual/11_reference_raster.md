# 제 11 장. 래스터 함수 레퍼런스 (Raster Functions Reference)

래스터 데이터 조작, 픽셀 값 추출, 맵 대수(MapAlgebra), 지형 분석 함수 레퍼런스입니다.

---

## 주요 래스터 함수
- `ST_Value(rast, [band], pt_geom)`: 특정 점(Point) 위치의 픽셀 값 추출
- `ST_SetValue(rast, band, x, y, new_val)`: 특정 픽셀 값 갱신
- `ST_Slope(rast, ...)`: 고도 래스터(DEM)로부터 경사도 계산
- `ST_Aspect(rast, ...)`: 사면 방위각 계산
- `ST_Hillshade(rast, ...)`: 지형 음영기복도 생성
- `ST_MapAlgebra(rast1, rast2, expression)`: 래스터 밴드 간 사용자 정의 수식 맵 대수 연산
- `ST_Clip(rast, geom)`: 벡터 폴리곤 모양으로 래스터 영역 자르기
- `ST_AsPNG(rast)` / `ST_AsTIFF(rast)`: 래스터 타일을 이미지 바이너리로 인코딩

---

| [⬅️ 제 10 장. 래스터 데이터 관리 및 분석 (Raster Data Management)](10_raster_data_management.md) | [🏠 매뉴얼 목차](README.md) | [제 12 장. 부가 기능 (PostGIS Extras) ➡️](12_extras.md) |
| :--- | :---: | ---: |
