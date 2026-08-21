# PostGIS 입문 워크숍 (Introduction to PostGIS)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/index.html>](https://postgis.net/workshops/postgis-intro/index.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 시작하기

- 본 워크숍에서는 **[실습 데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)**을 사용합니다. 다운로드 후 실습하기 편한 디렉터리에 압축을 해제하세요.
- 이 워크숍과 함께 제공되는 **[프레젠테이션 슬라이드(Google Slides)](https://docs.google.com/presentation/d/1qYXdeCIymLl32uoAHvAPrp1r-hK-_4Z8InG7sHEo6vc/edit)**를 참고할 수 있습니다.
- 워크숍에서 실행하는 모든 SQL 명령어 요약본은 **[`static/workshop-sql.txt`](static/workshop-sql.txt)** 파일에서 확인할 수 있습니다.

## 실습 데이터 번들 구성

압축 파일 내부에는 다음 폴더가 포함되어 있습니다.

- **`data/`**: PostgreSQL/PostGIS로 로딩할 공간 데이터 파일(Shapefile, 백업 덤프 등)이 포함된 디렉터리
- **`printing/`**: 실습 문제지 및 데이터 모델 다이어그램 인쇄용 PDF 자료가 포함된 디렉터리

## 라이선스 및 저작권

패키지의 모든 실습 데이터는 퍼블릭 도메인(Public Domain)으로 자유롭게 재배포할 수 있습니다. 사용되는 모든 소프트웨어는 오픈 소스입니다. 본 워크숍 문서는 [Creative Commons 저작자표시-동일조건변경허락 3.0(CC BY-SA 3.0)](http://creativecommons.org/licenses/by-sa/3.0/us/) 라이선스를 따릅니다.

## 워크숍 목차

전체 43개 모듈과 부록은 [워크숍 목차(README.md)](README.md#목차-table-of-contents)에서 확인할 수 있습니다.

## 주요 참고 링크

- **PostGIS 공식 사이트**: <https://postgis.net/>
  - 공식 문서: <https://postgis.net/docs/>
- **PostgreSQL 공식 사이트**: <https://www.postgresql.org/>
  - 공식 문서: <https://www.postgresql.org/docs/>
  - 다운로드: <https://www.postgresql.org/download/>
  - JDBC 드라이버: <https://jdbc.postgresql.org/>
  - .NET 드라이버 (Npgsql): <https://www.npgsql.org/>
  - Python 드라이버 (psycopg/pygresql): <http://www.pygresql.org/>
  - C/C++ 드라이버 (libpq): <https://www.postgresql.org/docs/current/static/libpq.html>
- **GUI 및 GIS 클라이언트**:
  - pgAdmin: <https://www.pgadmin.org/>
  - QGIS: <https://qgis.org/>
  - OpenJUMP: <http://openjump.org/>
  - uDig: <https://udig.github.io/>
- **온라인 실습 환경 (Playground)**:
  - PostgreSQL Playground: <https://sqlize.online/s/4Q>
  - PostGIS 워크숍 데이터 포함 Playground: <https://sqlize.online/s/pQ>


---

[목차](README.md) · [다음 →](01_welcome.md)
