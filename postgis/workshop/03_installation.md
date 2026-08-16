# 3. 설치 (Installation)

PostGIS를 시작하려면 먼저 **PostgreSQL**과 **PostGIS 확장 프로그램**을 설치해야 합니다.

---

## 1. 운영체제별 설치 방법

### Windows
1. [PostgreSQL 공식 다운로드 페이지](https://www.postgresql.org/download/windows/)에서 EnterpriseDB(EDB) PostgreSQL 설치 관리자를 다운로드하여 설치합니다.
2. 설치 완료 단계에서 **Stack Builder** 실행 체크박스를 선택합니다.
3. Stack Builder에서 `Spatial Extensions` 카테고리를 펼치고 **PostGIS** 최신 버전을 선택하여 설치합니다.

### macOS
- **Homebrew** 사용 시:
  ```bash
  brew install postgresql postgis
  brew services start postgresql
  ```
- **Postgres.app**: macOS용 올인원 패키지인 [Postgres.app](https://postgresapp.com/)을 설치하면 PostGIS가 기본으로 포함되어 있어 클릭 한 번으로 실행할 수 있습니다.

### Linux (Ubuntu / Debian)
```bash
sudo apt-get update
sudo apt-get install -y postgresql postgresql-contrib postgis postgresql-16-postgis-3
```

### Docker (컨테이너 환경)
공식 PostGIS Docker 이미지를 사용하여 즉시 실행할 수도 있습니다:
```bash
docker run --name postgis-demo -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgis/postgis:16-3.4
```

---

## 2. 클라이언트 도구 준비

- **psql**: PostgreSQL 명령줄 인터페이스 (설치 시 기본 제공)
- **pgAdmin 4**: 공식 웹/데스크톱 GUI 관리 도구
- **QGIS**: 공간 데이터를 지도 위에 시각화하고 PostGIS 테이블을 직접 조회할 수 있는 필수 오픈소스 데스크톱 GIS 프로그램 ([QGIS 다운로드](https://qgis.org/))

---

| [⬅️ 2. 소개 (Introduction)](02_introduction.md) | [🏠 워크숍 목차](README.md) | [4. 공간 데이터베이스 생성 (Creating a Spatial Database) ➡️](04_creating_db.md) |
| :--- | :---: | ---: |
