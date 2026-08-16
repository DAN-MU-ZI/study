# 1. 환영합니다 (Welcome)

이 워크숍을 찾아주셔서 감사합니다!

이 워크숍은 **PostgreSQL**과 **PostGIS**를 시작하는 분들을 위해 기획되었습니다. GIS(지리정보시스템) 개념에 대한 기초적인 이해와 기본적인 SQL 지식이 있다면 내용을 이해하는 데 큰 도움이 됩니다.

---

## 워크숍 목표

본 과정을 마치면 다음을 수행할 수 있게 됩니다:
- PostgreSQL 및 PostGIS 설치 및 설정
- 공간 데이터베이스 생성 및 관리
- Shapefile 및 기타 공간 데이터 포맷 가져오기(Import) 및 내보내기(Export)
- PostGIS 공간 쿼리를 통한 공간 분석(거리 계산, 교차, 버퍼 등)
- 공간 인덱스(R-Tree/GiST)를 활용한 대용량 공간 데이터 검색 최적화
- 좌표 참조계(CRS/SRID) 변환 및 지도 투영
- 지오메트리(Geometry)와 지오그래피(Geography) 데이터 타입의 차이점 이해 및 활용
- 공간 조인(Spatial Joins) 및 고급 공간 집계 분석 수행

---

## 전제 조건

- 관계형 데이터베이스(RDBMS) 및 SQL 쿼리 기초 지식 (SELECT, WHERE, JOIN, GROUP BY)
- 지리 좌표(위도, 경도) 및 지도에 대한 기본 개념

---

## 실습 환경 준비

- **실습 데이터 번들**: [postgis-workshop-2020.zip](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip) 다운로드 후 압축 해제
- **SQL 치트 시트**: [`static/workshop-sql.txt`](static/workshop-sql.txt)
- **추천 도구**: `psql` (CLI), **pgAdmin 4** (GUI), **QGIS** (오픈소스 데스크톱 GIS 시각화)

---

| [⬅️ PostGIS 입문 워크숍 (Introduction to PostGIS)](00_index.md) | [🏠 워크숍 목차](README.md) | [2. 소개 (Introduction) ➡️](02_introduction.md) |
| :--- | :---: | ---: |
