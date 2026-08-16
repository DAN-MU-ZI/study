# PostGIS 입문 워크숍 (Introduction to PostGIS)

PostGIS 공식 입문 튜토리얼 워크숍을 바탕으로 작성한 한국어 요약 문서입니다.
본 워크숍은 공간 데이터베이스의 기초 개념부터 PostgreSQL/PostGIS를 활용한 실무 공간 쿼리, 인덱싱, 공간 조인, 래스터 및 토폴로지 분석까지 단계별로 다룹니다.

---

## 시작하기 전에

- 본 워크숍은 **[실습 데이터 번들](https://s3.amazonaws.com/s3.cleverelephant.ca/postgis-workshop-2020.zip)**을 사용합니다. 다운로드 후 편리한 위치에 압축을 해제하세요.
- 워크숍과 함께 제공되는 **[프레젠테이션 슬라이드(Google Slides)](https://docs.google.com/presentation/d/1qYXdeCIymLl32uoAHvAPrp1r-hK-_4Z8InG7sHEo6vc/edit)**를 참고할 수 있습니다.
- 워크숍에서 실행하는 모든 SQL 명령어 요약본은 **[`static/workshop-sql.txt`](static/workshop-sql.txt)** 파일에서 확인할 수 있습니다.

### 데이터 번들 구성
- `data/`: 로딩할 공간 데이터 파일(Shapefile 등)이 포함된 디렉터리
- `printing/`: 실습 문제지 및 데이터 모델 다이어그램이 포함된 디렉터리

### 라이선스 및 저작권
패키지의 모든 데이터는 퍼블릭 도메인으로 자유롭게 재배포할 수 있습니다. 모든 소프트웨어는 오픈 소스입니다.
본 워크숍 문서는 [Creative Commons 저작자표시-동일조건변경허락 3.0(CC BY-SA 3.0)](http://creativecommons.org/licenses/by-sa/3.0/us/) 라이선스에 따라 자유롭게 사용 및 배포할 수 있습니다.

---

## 목차 (Table of Contents)

1. [환영합니다 (Welcome)](01_welcome.md)
2. [소개 (Introduction)](02_introduction.md)
3. [설치 (Installation)](03_installation.md)
4. [공간 데이터베이스 생성 (Creating a Spatial Database)](04_creating_db.md)
5. [공간 데이터 로딩 (Loading spatial data)](05_loading_data.md)
6. [실습 데이터 소개 (About our data)](06_about_data.md)
7. [단순 SQL (Simple SQL)](07_simple_sql.md)
8. [단순 SQL 실습 (Simple SQL Exercises)](08_simple_sql_exercises.md)
9. [지오메트리 (Geometries)](09_geometries.md)
10. [지오메트리 실습 (Geometry Exercises)](10_geometries_exercises.md)
11. [공간 관계 (Spatial Relationships)](11_spatial_relationships.md)
12. [공간 관계 실습 (Spatial Relationships Exercises)](12_spatial_relationships_exercises.md)
13. [공간 조인 (Spatial Joins)](13_joins.md)
14. [공간 조인 실습 (Spatial Joins Exercises)](14_joins_exercises.md)
15. [공간 인덱싱 (Spatial Indexing)](15_indexing.md)
16. [데이터 투영 (Projecting Data)](16_projection.md)
17. [투영 실습 (Projection Exercises)](17_projection_exercises.md)
18. [지오그래피 (Geography)](18_geography.md)
19. [지오그래피 실습 (Geography Exercises)](19_geography_exercises.md)
20. [지오메트리 생성 함수 (Geometry Constructing Functions)](20_geometry_returning.md)
21. [지오메트리 생성 실습 (Geometry Constructing Exercises)](21_geometry_returning_exercises.md)
22. [고급 공간 조인 (More Spatial Joins)](22_joins_advanced.md)
23. [유효성 (Validity)](23_validity.md)
24. [동등성 (Equality)](24_equality.md)
25. [선형 참조 (Linear Referencing)](25_linear_referencing.md)
26. [차원 확장 9-교차 모델 (DE-9IM)](26_de9im.md)
27. [인덱스 기반 클러스터링 (Clustering on Indices)](27_clusterindex.md)
28. [3차원 데이터 (3-D)](28_3d.md)
29. [최근접 이웃 탐색 (Nearest-Neighbour Searching / KNN)](29_knn.md)
30. [래스터 (Rasters)](30_rasters.md)
31. [토폴로지 (Topology)](31_topology.md)
32. [토폴로지 기본 타입 (Topology Basic Types)](32_topology_base_types.md)
33. [토폴로지와 지오메트리 표현 (Topology and Geometry Representation)](33_topology_topo_types.md)
34. [트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers)](34_history_tracking.md)
35. [PostgreSQL 기본 튜닝 (Basic PostgreSQL Tuning)](35_tuning.md)
36. [PostgreSQL 보안 (PostgreSQL Security)](36_security.md)
37. [PostgreSQL 스키마 (PostgreSQL Schemas)](37_schemas.md)
38. [PostgreSQL 백업 및 복원 (PostgreSQL Backup and Restore)](38_backup.md)
39. [소프트웨어 업그레이드 (Software Upgrades)](39_upgrades.md)
40. [고급 지오메트리 생성 (Advanced Geometry Constructions)](40_advanced_geometry_construction.md)
41. [부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)](41_postgis_functions.md)
42. [부록 B: 용어 사전 (Appendix B: Glossary)](42_glossary.md)
43. [부록 C: 라이선스 (Appendix C: License)](43_license.md)

---

## 유용한 참고 링크

- **PostGIS**: [https://postgis.net/](https://postgis.net/)
  - 공식 문서: [https://postgis.net/docs/](https://postgis.net/docs/)
- **PostgreSQL**: [https://www.postgresql.org/](https://www.postgresql.org/)
  - 공식 문서: [https://www.postgresql.org/docs/](https://www.postgresql.org/docs/)
  - 다운로드: [https://www.postgresql.org/download/](https://www.postgresql.org/download/)
  - JDBC 드라이버: [https://jdbc.postgresql.org/](https://jdbc.postgresql.org/)
  - .NET 드라이버: [https://www.npgsql.org/](https://www.npgsql.org/)
  - Python 드라이버: [http://www.pygresql.org/](http://www.pygresql.org/)
  - C/C++ 드라이버(libpq): [https://www.postgresql.org/docs/current/static/libpq.html](https://www.postgresql.org/docs/current/static/libpq.html)
- **GUI 클라이언트**:
  - PgAdmin: [https://www.pgadmin.org/](https://www.pgadmin.org/)
  - QGIS: [https://qgis.org/](https://qgis.org/)
  - OpenJUMP: [http://openjump.org/](http://openjump.org/)
  - uDig: [https://udig.github.io/](https://udig.github.io/)
- **온라인 실습 환경 (Online Playground)**:
  - PostgreSQL Playground: [https://sqlize.online/s/4Q](https://sqlize.online/s/4Q)
  - PostGIS 워크숍 데이터 포함 Playground: [https://sqlize.online/s/pQ](https://sqlize.online/s/pQ)
