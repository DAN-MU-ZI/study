# 제 12 장. 부가 기능 (PostGIS Extras)

PostGIS와 함께 제공되는 유용한 확장 도구 모음입니다.

---

## 1. Address Standardizer (`address_standardizer`)
비정형 주소 문자열을 파싱하여 건물번호, 도로명, 구역 등으로 구조화된 정규 필드로 분리하는 텍스트 분석 엔진입니다.

```sql
CREATE EXTENSION address_standardizer;
```

---

## 2. TIGER Geocoder (`postgis_tiger_geocoder`)
미국 인구조사국의 TIGER/Line 데이터를 활용한 표준 지오코딩 및 역지오코딩 모듈입니다.

---

| [⬅️ 제 11 장. 래스터 함수 레퍼런스 (Raster Functions Reference)](11_reference_raster.md) | [🏠 매뉴얼 목차](README.md) | [제 13 장. 특수 함수 색인 (Special Functions Index) ➡️](13_special_functions_index.md) |
| :--- | :---: | ---: |
