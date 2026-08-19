# PostGIS 입문 워크숍 (Introduction to PostGIS)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/index.html>](https://postgis.net/workshops/postgis-intro/index.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 시작하기

- 본 워크숍에서는 [데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)을 사용합니다. 내려받은 뒤 편리한 위치에 압축을 푸세요.
- 이 워크숍과 함께 제공되는 **프레젠테이션 데크**는 [Google 슬라이드](https://docs.google.com/presentation/d/1qYXdeCIymLl32uoAHvAPrp1r-hK-_4Z8InG7sHEo6vc/edit)에서 볼 수 있습니다.
- 이 워크숍에서 실행하는 SQL 명령의 **요약표**는 [`static/workshop-sql.txt`](static/workshop-sql.txt)에서 확인할 수 있습니다.

## 데이터 번들

데이터 번들 내에는 다음이 포함됩니다.

**data/**\
실습에서 불러올 데이터 파일이 들어 있는 디렉터리

**printing/**\
실습 문제와 데이터 모델 자료가 들어 있는 디렉터리

## 라이선스 및 복사

패키지의 모든 데이터는 퍼블릭 도메인이며 자유롭게 재배포할 수 있습니다. 패키지에 포함된 소프트웨어도 오픈 소스로 자유롭게 재배포할 수 있습니다. 이 워크숍은 [Creative Commons 저작자표시-동일조건변경허락 3.0(CC BY-SA 3.0)](http://creativecommons.org/licenses/by-sa/3.0/us/) 라이선스를 따르며, 해당 조건에 따라 자유롭게 재배포할 수 있습니다.

## 워크숍 모듈

전체 모듈과 부록은 [워크숍 목차](README.md#목차-table-of-contents)에서 확인할 수 있습니다.

## 유용한 링크

- PostGIS - <https://postgis.net/>
  - 문서 - <https://postgis.net/docs/>
- PostgreSQL - <https://www.postgresql.org/>
  - 문서 - <https://www.postgresql.org/docs/>
  - 다운로드 - <https://www.postgresql.org/download/>
  - JDBC 드라이버 - <https://jdbc.postgresql.org/>
  - .Net 드라이버 - <https://www.npgsql.org/>
  - 파이썬 드라이버 - <http://www.pygresql.org/>
  - C/C++ 드라이버 - <https://www.postgresql.org/docs/current/static/libpq.html>
- pgAdmin - <https://www.pgadmin.org/>
- 오픈 소스 데스크톱 클라이언트
  - QGIS - <https://qgis.org/>
  - OpenJUMP - <http://openjump.org/>
  - uDig - <https://udig.github.io/>
- 온라인 놀이터
  - PostgreSQL 플레이그라운드 - <https://sqlize.online/s/4Q>
  - 워크숍 데이터가 포함된 PostGIS 플레이그라운드 - <https://sqlize.online/s/pQ>


---

[목차](00_index.md) · [다음 →](01_welcome.md)
