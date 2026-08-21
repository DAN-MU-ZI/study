# 6. 실습 데이터 소개 (About our data)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/about_data.html>](https://postgis.net/workshops/postgis-intro/about_data.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

본 워크숍에서는 뉴욕시(New York City)를 배경으로 한 4개의 공간 테이블(Shapefile 기반)과 1개의 인구사회학적 속성 테이블을 사용합니다. 4개의 공간 테이블은 앞 장에서 데이터베이스에 이미 로딩되었으며, 속성 테이블은 뒤에서 공간 조인 실습 시 활용됩니다.

각 데이터 세트의 구조, 레코드 수, 컬럼 속성을 이해하는 것은 향후 진행될 공간 분석 쿼리의 기본 토대가 됩니다.

> [!TIP]
> pgAdmin에서 테이블 속성을 확인하려면 왼쪽 브라우저 트리에서 해당 테이블을 마우스 오른쪽 버튼으로 클릭하고 **Properties... > Columns** 탭을 확인하세요.

---

## 1. nyc_census_blocks (인구조사 블록)

**인구조사 블록(Census Block)**은 미국 인구조사국(US Census Bureau)이 통계를 집계하고 공표하는 가장 작은 기본 지리 단위입니다. 상위 집계 단위인 블록 그룹(Block Group), 인구조사구(Census Tract), 카운티(County) 등은 모두 이 블록들의 통합으로 구성됩니다. 이 테이블에는 블록별 기초 인구 통계가 포함되어 있습니다.

- **레코드 수**: 38,794건
- **지오메트리 유형**: 폴리곤 (Polygon / MultiPolygon)

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| **blkid** | character(15) | 각 인구조사 블록을 고유하게 식별하는 15자리 FIPS 코드 (예: `360050001009000`) |
| **popn_total** | double precision | 블록 내 총 거주 인구수 |
| **popn_white** | double precision | 백인(White) 인구수 |
| **popn_black** | double precision | 흑인/아프리카계 미국인(Black/African American) 인구수 |
| **popn_nativ** | double precision | 미국 원주민/알래스카 원주민(Native American) 인구수 |
| **popn_asian** | double precision | 아시아계(Asian) 인구수 |
| **popn_other** | double precision | 기타 인종 인구수 |
| **hous_total** | double precision | 블록 내 총 주택(가구) 수 |
| **hous_own** | double precision | 자가 거주 주택 수 |
| **hous_rent** | double precision | 임차(월세/전세) 거주 주택 수 |
| **boroname** | varchar(32) | 뉴욕시 자치구명 (`Manhattan`, `The Bronx`, `Brooklyn`, `Staten Island`, `Queens`) |
| **geom** | geometry | 인구조사 블록의 폴리곤 경계 지오메트리 (SRID: 26918) |

![총 인구 대비 흑인 인구 비율](screenshots/nyc_census_blocks.png)

*인구조사 블록별 인종 분포 시각화 예시*

---

## 2. nyc_neighborhoods (근린지역 / 동네)

뉴욕시는 오랜 역사와 함께 독특한 지역 명칭과 구역 문화를 형성해 왔습니다. **근린지역(Neighborhood)**은 행정구역처럼 법적 경계선이 명확히 고정된 것이 아니라, 주민들의 생활권과 부동산 시장 등에서 통용되는 사회·문화적 공간 단위입니다. 예를 들어 브루클린의 Carroll Gardens, Red Hook, Cobble Hill 등은 시대와 맥락에 따라 경계가 조금씩 다르게 인식되기도 합니다.

- **레코드 수**: 129건
- **지오메트리 유형**: 폴리곤 (MultiPolygon)

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| **name** | varchar(64) | 근린지역(동네) 이름 (예: `Financial District`, `West Village`, `Harlem` 등) |
| **boroname** | varchar(32) | 소속 자치구명 (`Manhattan`, `The Bronx`, `Brooklyn`, `Staten Island`, `Queens`) |
| **geom** | geometry | 근린지역의 폴리곤 경계 지오메트리 (SRID: 26918) |

![뉴욕시 이웃 지역 경계](screenshots/nyc_neighborhoods.png)

*뉴욕시 근린지역(Neighborhoods) 공간 경계*

---

## 3. nyc_streets (도로망)

뉴욕시 전체 도로망의 중심선(Centerline) 네트워크를 나타냅니다. 고속도로(Motorway), 주요 간선도로(Primary), 보조 간선도로(Secondary), 주거지 생활도로(Residential) 등 도로 등급과 일방통행 여부 속성이 포함되어 있습니다.

- **레코드 수**: 19,091건
- **지오메트리 유형**: 라인스트링 (MultiLineString)

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| **name** | varchar(64) | 도로/거리 이름 (예: `Broadway`, `Wall St`, `5th Ave` 등) |
| **oneway** | varchar(10) | 일방통행 여부 (`YES` = 일방통행, ` ` = 양방향) |
| **type** | varchar(32) | 도로 유형 (`primary`, `secondary`, `residential`, `motorway` 등) |
| **geom** | geometry | 도로의 라인스트링 중심선 지오메트리 (SRID: 26918) |

![뉴욕시 도로망과 빨간색으로 표시된 주요 도로](screenshots/nyc_streets.png)

*뉴욕시 도로 네트워크 (주요 도로는 붉은색으로 강조)*

---

## 4. nyc_subway_stations (지하철역)

지상 도시 공간과 지하 교통망을 연결하는 뉴욕시 지하철 역사의 위치를 나타냅니다. 대중교통 접근성 분석과 반경 내 인구수 집계 등 다양한 공간 쿼리에 활용됩니다.

- **레코드 수**: 491건
- **지오메트리 유형**: 포인트 (Point)

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| **name** | varchar(64) | 지하철역 이름 (예: `Broad St`, `Times Sq - 42 St` 등) |
| **borough** | varchar(32) | 소속 자치구명 |
| **routes** | varchar(24) | 해당 역을 경유하는 지하철 노선 (콤마로 구분, 예: `A,C,E`, `1,2,3`) |
| **transfers** | varchar(24) | 환승 가능한 노선 정보 |
| **express** | varchar(10) | 급행 열차 정차 여부 (`express` = 정차, ` ` = 일반) |
| **geom** | geometry | 지하철역의 포인트 위치 지오메트리 (SRID: 26918) |

![뉴욕시 지하철역 위치](screenshots/nyc_subway_stations.png)

*뉴욕시 지하철역 포인트 위치*

---

## 5. nyc_census_sociodata (인구사회 통계 테이블)

인구조사(Census) 과정에서 수집된 상세 사회경제학적 데이터(교통수단, 가구 소득, 학력 등)는 블록 단위보다 넓은 **인구조사구(Census Tract)** 수준에서 집계됩니다. 공간 경계 지오메트리는 없으며, 공간 분석 시 `tractid` 식별자를 매개로 공간 테이블과 결합(Join)하여 사용합니다.

- **레코드 수**: 2,166건
- **테이블 유형**: 비공간 속성 테이블 (공간 지오메트리 컬럼 없음)

| 컬럼명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| **tractid** | character(11) | 인구조사구(Census Tract) 고유 11자리 FIPS 코드 (예: `36005000100`) |
| **transit_total** | double precision | 조사구 내 전체 통근 근로자 수 |
| **transit_private** | double precision | 자가용/오토바이/승용차 통근 근로자 수 |
| **transit_public** | double precision | 지하철/버스 등 대중교통 통근 근로자 수 |
| **transit_walk** | double precision | 도보(걸어서) 통근 근로자 수 |
| **transit_other** | double precision | 자전거, 페리, 택시 등 기타 수단 통근 근로자 수 |
| **transit_none** | double precision | 재택근무자 수 |
| **transit_time_mins** | double precision | 조사구 내 전체 근로자의 총 통근 소요 시간(분) 합계 |
| **family_count** | double precision | 조사구 내 총 가구(가족) 수 |
| **family_income_median** | double precision | 가구 중위(중간값) 소득 (달러, USD) |
| **family_income_mean** | double precision | 가구 평균 소득 (달러, USD) |
| **family_income_aggregate**| double precision | 조사구 내 전체 가구의 총 소득 합계 (달러, USD) |
| **edu_total** | double precision | 25세 이상 학력 조사 대상 총 인구수 |
| **edu_no_highschool_dipl** | double precision | 고등학교 미졸업자 수 |
| **edu_highschool_dipl** | double precision | 최종 학력이 고등학교 졸업인 인구수 |
| **edu_college_dipl** | double precision | 대학교(학사) 학위 소지자 수 |
| **edu_graduate_dipl** | double precision | 대학원(석사/박사/전문직) 학위 소지자 수 |


---

[← 이전](05_loading_data.md) · [목차](00_index.md) · [다음 →](07_simple_sql.md)
