# PostGIS 입문 워크숍 (Introduction to PostGIS)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/index.html>](https://postgis.net/workshops/postgis-intro/index.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 시작하기

- 본 워크숍에서는 [데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)을 사용합니다. 다운로드하여 편리한 위치에 압축을 풀어보세요.
- 이 워크숍과 함께 제공되는 **프레젠테이션 데크**는 [Google 슬라이드](https://docs.google.com/presentation/d/1qYXdeCIymLl32uoAHvAPrp1r-hK-_4Z8InG7sHEo6vc/edit)에서 볼 수 있습니다.
- 이 워크숍에서 실행되는 SQL 명령의 **치트 시트**는 [여기에서 확인 가능](static/workshop-sql.txt)합니다.

## 데이터 번들

데이터 번들 내에는 다음이 포함됩니다.

**데이터/**\
로드할 데이터 파일이 포함된 디렉토리

**인쇄/**\
연습 페이지와 데이터 모델 페이지가 포함된 디렉토리

## 라이선스 및 복사

패키지의 모든 데이터는 공개 도메인이며 자유롭게 재배포할 수 있습니다. 패키지의 모든 소프트웨어는 오픈 소스이며 자유롭게 재배포할 수 있습니다. 이 워크샵은 Creative Commons "\`share alike with attribution \<[http://creativecommons.org/licenses/by-sa/3.0/us/\\\\\\](http://creativecommons.org/licenses/by-sa/3.0/us/\>\`\)\_" 라이선스가 부여되었으며 해당 라이선스 조건에 따라 자유롭게 재배포할 수 있습니다.

## 워크숍 모듈

<div class="toctree" data-maxdepth="1" data-numbered="">

환영합니다 소개 설치 Creating_db loading_data about_data simple_sql simple_sql_exercises 기하학 geometries_exercises Spatial_relationships Spatial_relationships_exercises 조인 Joins_exercises 인덱싱 투영 투영_운동 지리학 geography_exercises 기하학_반환 기하학_returning_exercises 조인_고급 유효성 평등 선형_참조 de9im 클러스터 인덱스 3d knn 래스터 토폴로지 topology_base_types topology_topo_types History_tracking 보안 스키마 튜닝 백업 업그레이드 Advanced_geometry_construction

Postgis-functions 용어집 라이센스

</div>

## 가지고 있어야 할 링크

- PostGIS - <https://postgis.net/>
  - 문서 - <https://postgis.net/docs/>
- PostgreSQL - <https://www.postgresql.org/>
  - 문서 - <https://www.postgresql.org/docs/>
  - 다운로드 - <https://www.postgresql.org/download/>
  - JDBC 드라이버 - <https://jdbc.postgresql.org/>
  - .Net 드라이버 - <https://www.npgsql.org/>
  - 파이썬 드라이버 - <http://www.pygresql.org/>
  - C/C++ 드라이버 - <https://www.postgresql.org/docs/current/static/libpq.html> -PgAdmin-<https://www.pgadmin.org/>
- 오픈 소스 데스크톱 클라이언트 -QGIS-<https://qgis.org/>
  - 오픈점프 - <http://openjump.org/>
  - uDig - <https://udig.github.io/>
- 온라인 놀이터
  - PostgreSQL 플레이그라운드 - <https://sqlize.online/s/4Q>
  - 워크숍 데이터가 포함된 PostGIS 플레이그라운드 - <https://sqlize.online/s/pQ>


---

[목차](00_index.md) · [다음 →](01_welcome.md)
