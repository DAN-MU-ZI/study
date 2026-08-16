# 6. 실습 데이터 소개 (About our data)

본 워크숍에서는 미국 뉴욕시(New York City)의 실제 오픈 지리 데이터를 사용하여 다양한 공간 쿼리와 분석을 실습합니다.

모든 데이터는 **SRID 26918 (NAD83 / UTM Zone 18N, 단위: 미터)** 좌표계로 투영되어 있습니다.

---

## 실습 테이블 개요

### 1. `nyc_census_blocks` (인구조사 블록)
미국 인구조사국(US Census Bureau)에서 집계한 뉴욕시의 가장 세부적인 인구 통계 블록 폴리곤 데이터입니다.

- **지오메트리 타입**: `MultiPolygon`
- **주요 컬럼**:
  - `blkid`: 15자리 고유 블록 코드
  - `popn_total`: 총 인구수
  - `popn_white`: 백인 인구수
  - `popn_black`: 흑인 인구수
  - `popn_asian`: 아시아계 인구수
  - `popn_hispanic`: 히스패닉 인구수
  - `geom`: 공간 지오메트리

![인구조사 블록](screenshots/nyc_census_blocks.png)

---

### 2. `nyc_neighborhoods` (동/이웃 지역 경계)
뉴욕시의 전통적인 이웃/마을 구역 경계 데이터입니다 (예: Greenwich Village, Harlem, Soho, Astoria 등).

- **지오메트리 타입**: `MultiPolygon`
- **주요 컬럼**:
  - `name`: 지역/동 이름 (예: `West Village`, `Chinatown`)
  - `boroname`: 5대 자치구 이름 (`Manhattan`, `Brooklyn`, `Queens`, `Bronx`, `Staten Island`)
  - `geom`: 공간 지오메트리

---

### 3. `nyc_streets` (도로망)
뉴욕시 전체 도로의 중심선(Centerline) 데이터입니다.

- **지오메트리 타입**: `MultiLineString`
- **주요 컬럼**:
  - `name`: 도로명 (예: `Broadway`, `5th Ave`, `Wall St`)
  - `type`: 도로 유형 (예: `residential`, `primary`, `motorway`)
  - `geom`: 공간 지오메트리

---

### 4. `nyc_subway_stations` (지하철역)
뉴욕시 지하철역의 정확한 위치 점(Point) 데이터입니다.

- **지오메트리 타입**: `Point`
- **주요 컬럼**:
  - `name`: 지하철역 이름 (예: `Times Sq - 42 St`, `Wall St`)
  - `routes`: 정차하는 지하철 노선 (예: `N,Q,R,W`, `1,2,3`)
  - `borough`: 자치구 이름
  - `geom`: 공간 지오메트리

![지하철역 위치](screenshots/nyc_subway_stations.png)

---

### 5. `nyc_homicides` (강력사건 발생 위치)
뉴욕시 경찰국(NYPD)에서 공개한 강력사건(살인 사건) 발생 위치 점 데이터입니다.

- **지오메트리 타입**: `Point`
- **주요 컬럼**:
  - `incident_date`: 사건 발생 날짜
  - `weapon`: 사용된 흉기/도구
  - `victim_race`: 피해자 인종
  - `geom`: 공간 지오메트리

---

| [⬅️ 5. 공간 데이터 로딩 (Loading spatial data)](05_loading_data.md) | [🏠 워크숍 목차](README.md) | [7. 단순 SQL (Simple SQL) ➡️](07_simple_sql.md) |
| :--- | :---: | ---: |
