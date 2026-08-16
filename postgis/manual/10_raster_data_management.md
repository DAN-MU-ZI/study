# 제 10 장. 래스터 데이터 관리 및 분석 (Raster Data Management)

PostGIS Raster(`postgis_raster`) 모듈을 활용한 대용량 위성/항공 사진 및 지형 격자 데이터 관리 기법입니다.

---

## 10.1. 래스터 타일링 및 로딩 (`raster2pgsql`)

대용량 래스터 이미지는 메모리 및 공간 인덱스 효율을 위해 작은 타일(예: 100x100 픽셀) 단위로 분할하여 데이터베이스 테이블에 저장합니다:

```bash
raster2pgsql -s 5179 -I -C -M -t 100x100 dem_korea.tif public.korea_dem | psql -U postgres -d gisdb
```
- `-s 5179`: 좌표계 SRID 지정
- `-t 100x100`: 타일 크기
- `-C`: 공간 참조 및 정렬 제약 조건 자동 등록
- `-I`: GiST 공간 인덱스 자동 생성

---

| [⬅️ 제 9 장. 토폴로지 (Topology)](09_topology.md) | [🏠 매뉴얼 목차](README.md) | [제 11 장. 래스터 함수 레퍼런스 (Raster Functions Reference) ➡️](11_reference_raster.md) |
| :--- | :---: | ---: |
