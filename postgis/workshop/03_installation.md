# 3. 설치 (Installation)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/installation.html>](https://postgis.net/workshops/postgis-intro/installation.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostgreSQL/PostGIS 데이터베이스를 탐색하고 SQL로 공간 쿼리를 작성하는 방법을 배우려면 로컬에 설치되거나 클라우드에서 원격으로 사용할 수 있는 소프트웨어가 필요합니다.

- Windows 또는 MacOS에 설치하기 위해 PostgreSQL에 액세스하는 방법은 다음과 같습니다. Windows 및 MacOS용 PostgreSQL에는 PostGIS가 포함되어 있거나 이를 쉽게 추가할 수 있는 방법이 있습니다.
- [PgAdmin](https://www.pgadmin.org/) 설치 방법은 아래와 같습니다. PgAdmin은 모든 작업을 수행하는 데이터베이스 엔진에 "사용자용" 인터페이스를 제공하는 그래픽 데이터베이스 탐색기이자 SQL 편집기입니다.

PostgreSQL 설치에 대한 최신 지침을 보려면 [PostgreSQL 다운로드 페이지](https://www.postgresql.org/download/)로 이동하여 사용 중인 운영 체제를 선택하세요.

## 마이크로소프트 윈도우용 PostgreSQL

Windows 설치의 경우:

1.  [Windows PostgreSQL 다운로드 페이지](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads)로 이동합니다.

2.  최신 버전의 PostgreSQL을 선택하고 설치 프로그램을 디스크에 저장합니다.

3.  설치 프로그램을 실행하고 기본값을 수락합니다.

4.  데이터베이스와 함께 설치된 "StackBuilder" 프로그램을 찾아 실행합니다.

5.  "공간 확장" 섹션을 선택하고 최신 "PostGIS ..Bundle" 옵션을 선택합니다.

![image](screenshots/install_windows_01.png)

6.  기본값을 수락하고 설치합니다.

## Apple MacOS용 PostgreSQL

MacOS 설치의 경우:

1.  [Postgres.app](https://postgresapp.com/) 사이트로 이동하여 최신 릴리스를 다운로드합니다.

2.  디스크 이미지를 열고 **Postgres** 아이콘을 **Applications** 폴더로 드래그합니다.

![image](screenshots/install_macos_01.png)

3.  **Applications** 폴더에서 **Postgres** 아이콘을 두 번 클릭하여 서버를 시작합니다.

4.  **Initialize** 버튼을 클릭하여 새로운 빈 데이터베이스 인스턴스를 생성합니다.

![image](screenshots/install_macos_02.png)

5.  **Applications** 폴더에서 **Utilities** 폴더로 이동하여 **Terminal**를 엽니다.

6.  편의를 위해 <span class="title-ref">PATH</span>에 명령줄 유틸리티를 추가합니다.

>     sudo mkdir -p /etc/paths.d
>     echo /Applications/Postgres.app/Contents/Versions/latest/bin | sudo tee /etc/paths.d/postgresapp

## Windows 및 MacOS용 PgAdmin

PgAdmin은 <https://www.pgadmin.org/download/>에서 여러 플랫폼에서 사용할 수 있습니다.

1.  귀하의 플랫폼에 맞는 최신 버전을 다운로드하여 설치하십시오.

2.  PgAdmin을 시작하세요!

![image](screenshots/install_pgadmin_01.png)

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

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

</details>

---

[← 이전](02_introduction.md) · [목차](00_index.md) · [다음 →](04_creating_db.md)