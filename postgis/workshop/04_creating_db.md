# 4. 공간 데이터베이스 생성 (Creating a Spatial Database)

PostgreSQL에서는 일반 데이터베이스를 생성한 후, SQL 명령어로 `postgis` 확장을 활성화하여 공간 데이터베이스로 전환합니다.

---

## 1. 데이터베이스 생성

명령줄(psql) 또는 pgAdmin에서 새 데이터베이스를 생성합니다. 본 워크숍에서는 `nyc`라는 이름의 데이터베이스를 사용합니다.

### psql에서 생성:
```sql
CREATE DATABASE nyc;
```

또는 터미널 명령어로:
```bash
createdb -U postgres nyc
```

---

## 2. PostGIS 확장(Extension) 활성화

생성한 `nyc` 데이터베이스에 접속한 후, `CREATE EXTENSION` 명령을 실행합니다.

```sql
\c nyc

-- PostGIS 기본 공간 확장 활성화
CREATE EXTENSION postgis;
```

> [!NOTE]
> PostgreSQL 9.1 이후 버전부터는 `CREATE EXTENSION postgis;` 한 줄로 PostGIS의 모든 데이터 타입과 수백 개의 함수가 현재 데이터베이스에 로드됩니다.

---

## 3. 설치 확인

PostGIS가 성공적으로 활성화되었는지 버전을 조회하여 확인합니다:

```sql
SELECT postgis_full_version();
```

출력 예시:
```text
POSTGIS="3.4.1" [EXTENSION] PGSQL="160" GEOS="3.12.1-CAPI-1.18.1" PROJ="9.3.1" LIBXML="2.9.14" LIBJSON="0.17" ...
```

위와 같이 GEOS, PROJ 등의 공간 연산 및 투영 엔진 라이브러리 버전 정보가 표시되면 성공적으로 준비된 것입니다.

---

## 4. 메타데이터 테이블 확인

PostGIS 확장이 활성화되면 두 개의 메타데이터 뷰가 자동으로 생성됩니다:
- `spatial_ref_sys`: 전 세계 좌표 참조계(SRID) 정의 정보를 담고 있는 테이블 (EPSG 코드 등 5,000개 이상의 좌표계 포함)
- `geometry_columns`: 데이터베이스 내 모든 지오메트리 컬럼의 메타데이터 뷰

---

| [⬅️ 3. 설치 (Installation)](03_installation.md) | [🏠 워크숍 목차](README.md) | [5. 공간 데이터 로딩 (Loading spatial data) ➡️](05_loading_data.md) |
| :--- | :---: | ---: |
