# PostGIS 공식 개발자 매뉴얼 (PostGIS Reference Manual)

PostGIS 공식 개발자 매뉴얼([postgis-ko_KR.html](https://postgis.net/docs/manual-dev/postgis-ko_KR.html))의 전체 한국어 번역 및 기술 레퍼런스입니다.  
본 매뉴얼은 기존 웹 문서에 남아 있던 **미번역 영문 문장**과 **기계 번역 오역**을 전면 교정하여, 실제 개발 및 운영 환경에서 바로 활용할 수 있는 표준 한국어 기술 문서로 재구성되었습니다.

---

## 📚 전체 목차 (Table of Contents)

| 챕터 | 문서명 | 주요 내용 |
| :--- | :--- | :--- |
| **제 1 장** | [1. PostGIS 소개](01_introduction.md) | 공간 데이터베이스 정의, PostGIS 아키텍처, OGC/ISO 표준 지원, 프로젝트 패밀리 |
| **제 2 장** | [2. PostGIS 설치](02_installation.md) | OS별 설치(Windows, Linux, macOS), 소스코드 컴파일 빌드, 의존성 라이브러리(GEOS, PROJ, GDAL) |
| **제 3 장** | [3. PostGIS 관리](03_administration.md) | 공간 DB 생성/활성화, 백업 및 복원, 마이너/메이저 버전 업그레이드 |
| **제 4 장** | [4. 공간 데이터 관리](04_data_management.md) | 공간 테이블 정의, SRID 메타데이터, `geometry_columns` 뷰, 데이터 로딩/덤프 도구 |
| **제 5 장** | [5. 공간 쿼리 및 분석](05_spatial_queries.md) | 바운딩 박스 연산자(`&&`), GiST 인덱스 검색, 공간 조인 및 거리 쿼리 최적화 |
| **제 6 장** | [6. 성능 팁 및 튜닝](06_performance_tips.md) | PostgreSQL 서버 파라미터 튜닝, `CLUSTER`, 공간 통계 수집, 대용량 데이터 인덱싱 전략 |
| **제 7 장** | [7. 벡터 함수 레퍼런스](07_reference_vector.md) | 지오메트리 생성, 변환, 공간 관계 판별, 공간 연산(버퍼, 인터섹트, 유니온 등) 핵심 함수 명세 |
| **제 8 장** | [8. SFCGAL 3D 함수 레퍼런스](08_reference_sfcgal.md) | SFCGAL 확장, 3차원 솔리드(Solid), Polyhedral Surface, TIN, 3D 불리언 연산 |
| **제 9 장** | [9. 토폴로지 (Topology)](09_topology.md) | **토폴로지 도메인(`TopoElement`), 타입(`TopoGeometry`), 기본 테이블, 편집/관리 함수 총괄** |
| **제 10 장** | [10. 래스터 데이터 관리](10_raster_data_management.md) | 래스터 데이터 모델, `raster2pgsql` 타일링 로딩, 래스터 카탈로그 및 래스터-벡터 결합 분석 |
| **제 11 장** | [11. 래스터 함수 레퍼런스](11_reference_raster.md) | 래스터 생성, 픽셀 접근, MapAlgebra 맵 대수 연산, 수치표고모델(DEM) 경사도/음영기복도 함수 |
| **제 12 장** | [12. 부가 기능 (Extras)](12_extras.md) | Address Standardizer (주소 정규화 엔진), TIGER 지오코더 (TIGER Geocoder) |
| **제 13 장** | [13. 특수 함수 색인](13_special_functions_index.md) | 공간 집계 함수, 윈도우 함수, SQL-MM 호환 함수, 곡선(Curve) 지오메트리 지원 |
| **제 14 장** | [14. 문제 보고 및 기여](14_reporting_problems.md) | 버그 리포트 작성 가이드, 성능 이슈 프로파일링, PostGIS 오픈소스 커뮤니티 기여 방법 |
| **제 15 장** | [15. 크레딧 및 라이선스](15_credits.md) | 저작권자, 개발팀 크레딧, GNU GPL v2 / CC BY-SA 라이선스 안내 |

---

## 🔍 [토폴로지(Topology) 상세 서브 모듈](topology/README.md)
토폴로지 관련 도메인, 데이터 타입, 테이블 구조의 상세 레퍼런스는 `topology/` 서브 디렉터리에서도 확인할 수 있습니다:
- [9.4. 토폴로지 도메인 (`TopoElement`, `TopoElementArray`)](topology/01_topology_domains.md)
- [9.3. 토폴로지 데이터 타입 (`TopoGeometry`, `getfaceedges_returntype` 등)](topology/02_topology_types.md)
- [9.2. 토폴로지 기본 테이블 (`node`, `edge_data`, `face`, `relation`)](topology/03_topology_primitive_tables.md)
- [9.5 ~ 9.16. 토폴로지 함수 레퍼런스](topology/04_topology_functions.md)
