# 42. 부록 B: 용어 사전 (Appendix B: Glossary)

공간 데이터베이스 및 PostGIS에서 사용하는 핵심 기술 용어 정리입니다.

---

- **Bounding Box (MBR, 최소 경계 사각형)**: 지오메트리를 감싸는 $X_{min}, Y_{min}, X_{max}, Y_{max}$ 직사각형 경계로, 공간 인덱스의 1단계 고속 필터링에 사용됩니다.
- **CRS / SRS (좌표 참조계)**: 지표면 상의 위치를 수치 좌표로 정의하기 위한 기준 좌표계입니다.
- **EPSG 코드**: 석유 탐사 표준 기구(EPSG)에서 전 세계 좌표계에 부여한 고유 식별 번호입니다 (예: 4326 = WGS84, 3857 = Web Mercator).
- **Geography**: 지구 곡면(타원체)을 기준으로 하는 구면 지리 좌표 데이터 타입입니다 (항상 미터 단위 계산).
- **Geometry**: 2차원 또는 3차원 유클리드 평면 좌표계를 기준으로 하는 공간 데이터 타입입니다.
- **GiST (Generalized Search Tree)**: PostgreSQL에서 R-Tree 계층형 공간 인덱스를 구현하기 위해 사용하는 확장 인덱스 프레임워크입니다.
- **KNN (K-Nearest Neighbors)**: 특정 지점에서 가장 가까운 $K$개의 이웃 객체를 찾는 공간 쿼리 기법입니다 (`<->` 연산자 사용).
- **Raster (래스터)**: 위성 사진, 표고 모델처럼 격자(Grid) 픽셀 형태로 표현되는 공간 데이터입니다.
- **SRID (Spatial Reference System Identifier)**: 공간 데이터베이스에서 좌표 참조계를 식별하기 위한 고유 정수 ID입니다.
- **Topology (토폴로지)**: 노드(Node), 에지(Edge), 면(Face) 간의 위상학적 연결 관계를 저장하여 경계 불일치를 방지하는 데이터 모델입니다.
- **Vector (벡터)**: 점(Point), 선(LineString), 면(Polygon)의 정점 좌표들의 집합으로 표현되는 공간 데이터입니다.
- **WKB (Well-Known Binary)**: 지오메트리를 데이터베이스 내부에 저장하거나 전송할 때 사용하는 표준 바이너리 인코딩 포맷입니다.
- **WKT (Well-Known Text)**: 지오메트리를 사람이 읽을 수 있는 텍스트 형식으로 표현하는 OGC 표준 표기법입니다 (예: `POINT(1 2)`).

---

| [⬅️ 41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)](41_postgis_functions.md) | [🏠 워크숍 목차](README.md) | [43. 부록 C: 라이선스 (Appendix C: License) ➡️](43_license.md) |
| :--- | :---: | ---: |
