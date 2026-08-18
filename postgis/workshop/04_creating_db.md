# 4. 공간 데이터베이스 생성 (Creating a Spatial Database)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/creating_db.html>](https://postgis.net/workshops/postgis-intro/creating_db.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## PgAdmin

PostgreSQL에는 다양한 관리 프런트엔드가 있습니다. 기본 도구는 SQL 쿼리를 입력하기 위한 명령줄 도구인 [psql](http://www.postgresql.org/docs/current/static/app-psql.html)입니다. 또 다른 인기 있는 PostgreSQL 프런트엔드는 무료 오픈 소스 그래픽 도구인 [pgAdmin](http://www.pgadmin.org/)입니다. pgAdmin에서 수행된 모든 쿼리는 `psql`를 사용하여 명령줄에서도 수행할 수 있습니다. pgAdmin에는 PostGIS 쿼리를 공간적으로 보는 데 사용할 수 있는 지오메트리 뷰어도 포함되어 있습니다.

1.  pgAdmin을 찾아서 시작하세요.

![image](screenshots/pgadmin_01.png)

2.  pgAdmin을 처음 실행하는 경우 서버가 구성되어 있지 않을 수 있습니다. 브라우저 패널에서 `Servers` 항목을 마우스 오른쪽 버튼으로 클릭합니다.

    서버 이름을 **PostGIS**로 지정하겠습니다. 연결 탭에서 `Host name/address`를 입력합니다. 로컬 PostgreSQL 설치로 작업하는 경우 `localhost`를 사용할 수 있습니다. 클라우드 서비스를 사용하는 경우 계정에서 호스트 이름을 검색할 수 있습니다.

    **Port**는 `5432`로 설정하고 **유지 관리 데이터베이스**와 **Username**는 모두 `postgres`로 둡니다. **Password**는 로컬 설치 또는 클라우드 서비스에서 지정한 것이어야 합니다.

![image](screenshots/pgadmin_02a.png)

## 데이터베이스 생성

1.  데이터베이스 트리 항목을 열고 사용 가능한 데이터베이스를 살펴보세요. `postgres` 데이터베이스는 기본 postgres 사용자를 위한 사용자 데이터베이스이며 우리에게는 그다지 흥미롭지 않습니다.

2.  `Databases` 항목을 마우스 오른쪽 버튼으로 클릭하고 `New Database`를 선택합니다.

![image](screenshots/pgadmin_02.png)

3.  아래와 같이 `Create Database` 양식을 작성하고 **OK**를 클릭합니다.

    |           |            |
    |-----------|------------|
    | **Name**  | `nyc`      |
    | **Owner** | `postgres` |

![image](screenshots/pgadmin_03.png)

4.  새 `nyc` 데이터베이스를 선택하고 열어 개체 트리를 표시합니다. `public` 스키마가 표시됩니다.

    ![이미지](screenshots/pgadmin_04.png)

5.  아래 표시된 SQL 쿼리 버튼을 클릭합니다(또는 *도구 \> 쿼리 도구*로 이동).

    ![이미지](screenshots/pgadmin_05.png)

6.  쿼리 텍스트 필드에 다음 쿼리를 입력하여 PostGIS 공간 확장을 로드합니다:

    ```sql
    CREATE EXTENSION postgis;
    ```

7.  도구 모음에서 **Play** 버튼을 클릭하거나 **F5**를 눌러 "쿼리를 실행합니다."

8.  이제 PostGIS 기능을 실행하여 PostGIS가 설치되었는지 확인합니다.

    ```sql
    SELECT postgis_full_version();
    ```

PostGIS 공간 데이터베이스를 성공적으로 생성했습니다!!

## 기능 목록

[PostGIS_Full_Version](http://postgis.net/docs/PostGIS_Full_Version.html): 전체 PostGIS 버전 및 빌드 구성 정보를 보고합니다.


---

[← 이전](03_installation.md) · [목차](00_index.md) · [다음 →](05_loading_data.md)