# 37. PostgreSQL 스키마 (PostgreSQL Schemas)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/schemas.html>](https://postgis.net/workshops/postgis-intro/schemas.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

프로덕션 데이터베이스에는 필연적으로 많은 수의 테이블과 뷰가 있으며 이를 모두 하나의 스키마에서 관리하는 것은 빠르게 다루기 어려워질 수 있습니다. 다행히 [PostgreSQL](http://www.postgresql.org/)에는 "\_Schema" 개념이 포함되어 있습니다.

스키마는 폴더와 같으며 테이블, 뷰, 함수, 시퀀스 및 기타 관계를 보유할 수 있습니다. 모든 데이터베이스는 `public` 스키마라는 하나의 스키마로 시작됩니다.

![이미지](screenshots/schemas.jpg)

해당 스키마 내에서 PostGIS의 기본 설치는 `geometry_columns`, `geography_columns` 및 `spatial_ref_sys` 메타데이터 관계뿐만 아니라 PostGIS에서 사용되는 모든 유형과 기능을 생성합니다. 따라서 PostGIS 사용자는 항상 공개 스키마에 접근해야 합니다.

공개 스키마에서는 지금까지 워크숍에서 생성한 모든 테이블을 볼 수도 있습니다.

## 스키마를 사용하는 이유는 무엇입니까?

스키마를 사용하는 데에는 두 가지 좋은 이유가 있습니다.

- 스키마로 관리되는 데이터는 일괄 작업을 적용하기가 더 쉽습니다.
  - 별도의 스키마에 있는 데이터를 백업하는 것이 더 쉽습니다. 따라서 휘발성 데이터는 비휘발성 데이터와 다른 백업 일정을 가질 수 있습니다.
  - 별도의 스키마에 있는 데이터를 복원하는 것이 더 쉽습니다. 따라서 시간 여행 및 복구를 위해 애플리케이션 중심 스키마를 별도로 복원하고 백업할 수 있습니다.
  - 애플리케이션 데이터가 스키마에 있으면 애플리케이션 차이점을 관리하기가 더 쉽습니다. 따라서 새 버전의 소프트웨어는 새 스키마의 테이블 구조에서 작동할 수 있으며, 컷오버에는 스키마 이름에 대한 간단한 변경이 포함됩니다.
- 사용자는 작업을 단일 스키마로 제한하여 분석 및 테스트 테이블을 프로덕션 테이블에서 격리할 수 있습니다.

따라서 프로덕션 목적으로 애플리케이션 데이터를 스키마에서 별도로 유지하면 관리가 향상됩니다. 사용자 목적을 위해 사용자를 별도의 스키마에 유지하면 서로를 밟는 것을 방지할 수 있습니다.

## 데이터 스키마 생성

새 스키마를 생성하고 그 스키마로 테이블을 이동해 보겠습니다. 먼저 데이터베이스에 새 스키마를 만듭니다.

```sql
CREATE SCHEMA census;
```

다음으로 `nyc_census_blocks` 테이블을 `census` 스키마로 이동합니다.

```sql
ALTER TABLE nyc_census_blocks SET SCHEMA census;
```

`psql` 명령줄 프로그램을 사용하는 경우 이제 `nyc_census_blocks`가 테이블 목록에서 사라진 것을 확인할 수 있습니다! PgAdmin을 사용하는 경우 새 스키마와 그 안에 있는 테이블을 보려면 보기를 새로 고쳐야 할 수도 있습니다.

다음 두 가지 방법으로 스키마 내부의 테이블에 액세스할 수 있습니다.

- `schema.table` 표기법을 사용하여 참조합니다.
- `search_path`에 스키마를 추가하여

명시적 참조는 쉽지만 잠시 후에 입력하는 것이 피곤해집니다.

```sql
SELECT * FROM census.nyc_census_blocks LIMIT 1;
```

`search_path`를 조작하는 것은 추가 입력을 많이 하지 않고도 여러 스키마의 테이블에 대한 액세스를 제공할 수 있는 좋은 방법입니다.

`SET` 명령을 사용하여 런타임에 `search_path`를 설정할 수 있습니다.

```sql
SET search_path = census, public;
```

이렇게 하면 관계 및 함수에 대한 모든 참조가 `census` 및 `public` 스키마 모두에서 검색됩니다. 모든 PostGIS 함수와 유형은 `public`에 있으므로 검색 경로에서 이를 삭제하고 싶지 않다는 점을 기억하세요.

연결할 때마다 검색 경로를 설정하는 것도 피곤할 수 있지만 다행히도 사용자에 대한 검색 경로를 영구적으로 설정하는 것이 가능합니다.

```sql
ALTER USER postgres SET search_path = census, public;
```

이제 postgres 사용자는 검색 경로에 항상 `census` 스키마를 갖게 됩니다.

## 사용자 스키마 생성

사용자는 테이블 생성을 좋아하며 PostGIS 사용자는 특히 그렇게 합니다. SQL을 사용한 분석 작업에는 시각화 또는 중간 결과를 위한 임시 테이블이 필요하므로 공간 SQL에서는 사용자에게 일반 데이터베이스 작업 부하보다 더 많은 CREATE 권한이 있어야 하는 경향이 있습니다.

기본적으로 Oracle의 모든 역할에는 개인 스키마가 제공됩니다. 이는 PostgreSQL 사용자에게도 사용하기에 좋은 방법이며 PostgreSQL 역할, 스키마 및 검색 경로를 사용하여 쉽게 복제할 수 있습니다.

테이블 생성 권한이 있는 새 사용자를 생성한 다음(`postgis_writer` 역할에 대한 자세한 내용은 `security` 참조) 해당 사용자를 권한 부여로 사용하여 스키마를 생성합니다.

```sql
CREATE USER myuser WITH ROLE postgis_writer;
CREATE SCHEMA myuser AUTHORIZATION myuser;
```

해당 사용자로 로그인하면 PostgreSQL의 기본 `search_path`가 실제로 다음과 같습니다.

```sql
show search_path;
```

    search_path
    ----------------
    "$user",public

검색 경로의 첫 번째 스키마는 사용자의 명명된 스키마입니다! 이제 다음과 같은 조건이 존재합니다.

- 공간 테이블을 생성할 수 있는 능력을 가진 사용자가 존재합니다.
- 사용자의 명명된 스키마가 존재하고 사용자가 이를 소유합니다.
- 사용자의 검색 경로에는 사용자 스키마가 먼저 있으므로 해당 위치에 새 테이블이 자동으로 생성되고, 쿼리가 자동으로 해당 경로를 먼저 검색합니다.

그게 전부입니다. 사용자의 기본 작업 영역은 이제 다른 스키마의 테이블과 잘 분리됩니다.

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 37. PostgreSQL 스키마 (PostgreSQL Schemas)

PostgreSQL의 스키마(Schema)는 파일 시스템의 폴더처럼 데이터베이스 내부에서 테이블, 뷰, 함수들을 논리적으로 분리하고 구성하는 네임스페이스입니다.

---

## 1. 스키마 생성 및 활용

```sql
-- 프로젝트별 스키마 생성
CREATE SCHEMA census;
CREATE SCHEMA transport;
CREATE SCHEMA staging;

-- 특정 스키마에 테이블 생성 또는 이동
ALTER TABLE nyc_census_blocks SET SCHEMA census;
ALTER TABLE nyc_subway_stations SET SCHEMA transport;
ALTER TABLE nyc_streets SET SCHEMA transport;
```

---

## 2. 검색 경로 (search_path)

테이블명 앞에 스키마명을 붙이지 않고 조회할 때 참조하는 우선순위 경로입니다:

```sql
-- 검색 경로 설정
SET search_path TO transport, census, public;

-- 이제 'transport.nyc_subway_stations' 대신 'nyc_subway_stations'로 조회 가능
SELECT * FROM nyc_subway_stations LIMIT 1;
```

---

| [⬅️ 36. PostgreSQL 보안 (PostgreSQL Security)](36_security.md) | [🏠 워크숍 목차](README.md) | [38. PostgreSQL 백업 및 복원 (PostgreSQL Backup and Restore) ➡️](38_backup.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](36_security.md) · [목차](00_index.md) · [다음 →](38_backup.md)