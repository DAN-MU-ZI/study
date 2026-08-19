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

1.  사용 중인 플랫폼에 맞는 최신 버전을 내려받아 설치합니다.

2.  PgAdmin을 시작하세요!

![image](screenshots/install_pgadmin_01.png)


---

[← 이전](02_introduction.md) · [목차](00_index.md) · [다음 →](04_creating_db.md)
