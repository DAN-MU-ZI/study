# 39. 소프트웨어 업그레이드 (Software Upgrades)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/upgrades.html>](https://postgis.net/workshops/postgis-intro/upgrades.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostGIS는 PostgreSQL 내에 상주하기 때문에 모든 PostGIS 설치는 실제로 PostgreSQL 버전과 PostGIS 버전이라는 두 가지 소프트웨어 버전으로 구성됩니다. 일반적으로 PostGIS의 각 버전은 이론적으로 여러 PostgreSQL 버전 내에서 실행될 수 있으며 그 반대의 경우도 마찬가지입니다.

실제로 사용 가능한 정확한 버전 쌍은 PostgreSQL 배포판을 구축한 패키저에 의해 결정됩니다. 대부분의 Linux 패키지에는 각 PostgreSQL 버전 릴리스에 대한 두 개의 PostGIS 버전이 포함되어 있으므로 기본 설정에 따라 해당 부분을 독립적으로 또는 동시에 업그레이드할 수 있습니다.

업그레이드는 각 구성요소를 업그레이드하는 측면에서 고려될 수 있습니다.

## PostgreSQL 업그레이드

PostgreSQL 업그레이드 시나리오에는 두 가지 종류가 있습니다.

- "패치" 수준에서 소프트웨어 버전이 증가하는 경우 "마이너 업그레이드"입니다. 예를 들어 8.4.3에서 8.4.4로, 또는 9.0.1에서 9.0.3으로. 패치 버전을 두 개 이상 늘리는 것은 괜찮습니다. 마이너 업그레이드는 버그를 수정하지만 새로운 기능을 추가하거나 동작을 변경하지는 않습니다.
- "주요" 또는 "부" 버전이 증가하는 경우 "주요 업그레이드"입니다. 예를 들어 8.4.5에서 9.0.0으로 또는 9.0.5에서 9.1.1로. 주요 업그레이드는 새로운 기능을 추가하고 동작을 변경합니다.

### PostgreSQL 마이너 업그레이드

"사소한 업그레이드"의 경우 특별한 프로세스가 필요하지 않습니다. 새 소프트웨어를 설치하고 서버를 다시 시작하기만 하면 됩니다.

### 주요 PostgreSQL 업그레이드

"주요 업그레이드"의 경우 업그레이드를 수행하는 방법에는 두 가지가 있습니다.

#### 덤프/복원

덤프 및 복원에는 덤프 시 모든 데이터를 플랫폼 중립 형식(텍스트 표현)으로 변환하고 복원 시 기본 표현으로 다시 변환하는 작업이 포함되므로 시간이 많이 걸리고 CPU를 많이 소모할 수 있습니다. 그러나 새로운 아키텍처나 운영 체제로 마이그레이션하는 경우에는 필수 프로세스입니다. 또한 오랜 시간에 걸쳐 테스트를 거쳐 잘 이해된 업그레이드 경로이므로 데이터베이스가 너무 크지 않다면 이를 고수하지 않을 이유가 없습니다.

- 이전 데이터베이스에서 데이터 `pg_dumpall`를 덤프합니다.
- 새 버전의 PostgreSQL과 이전 데이터베이스에서 사용하고 있는 동일한 버전의 PostGIS를 설치합니다. 덤프 파일 함수 정의가 예상되는 PostGIS 라이브러리 버전을 참조하도록 PostGIS 버전을 일치시켜야 합니다.
- 새 소프트웨어의 `initdb` 프로그램을 사용하여 새 데이터 영역을 초기화합니다.
- 새 데이터 영역에서 새 서버를 시작합니다.
- `pg_restore`를 사용하여 덤프 파일을 복원합니다.

#### pg_upgrade

[pg_upgrade](http://www.postgresql.org/docs/current/static/pgupgrade.html) 유틸리티를 사용하면 덤프/복원 단계 없이 PostgreSQL 데이터 디렉터리를 업그레이드할 수 있습니다. 이 유틸리티는 데이터 파일 자체에 대한 변경 사항을 처리할 수 없지만 PostgreSQL 주요 업그레이드에서 발생하는 시스템 테이블에 대한 보다 일반적이고 빈번한 변경 사항을 처리합니다.

> [!NOTE]
> 업그레이드 프로세스 실행에 대한 전체 지침은 PostgreSQL 사이트의 [pg_upgrade](http://www.postgresql.org/docs/current/static/pgupgrade.html) 웹 페이지에 있습니다.

[pg_upgrade](http://www.postgresql.org/docs/current/static/pgupgrade.html) 프로그램은 작업 중인 PostgreSQL의 두 버전, 즉 이전 버전과 새 버전 모두에 액세스할 수 있을 것으로 예상하므로 두 버전을 모두 설치해야 합니다.

- 사용할 PostgreSQL의 새 버전을 설치합니다.

- 이전 PostgreSQL에서 사용하고 있는 것과 동일한 버전의 PostGIS를 새 PostgreSQL에 설치합니다.

- `initdb`의 새 복사본을 사용하여 새 PostgreSQL 데이터 영역을 초기화합니다.

- 이전 PostgreSQL 서버와 새 PostgreSQL 서버가 모두 꺼져 있는지 확인하세요.

- [pg_upgrade](http://www.postgresql.org/docs/current/static/pgupgrade.html)를 실행하여 새 소프트웨어 설치의 바이너리를 사용해야 합니다.

      pg_upgrade
        --old-datadir "/var/lib/postgres/12/data"
        --new-datadir "/var/lib/postgres/13/data"
        --old-bindir "/usr/pgsql/12/bin"
        --new-bindir "/usr/pgsql/13/bin"

- [pg_upgrade](http://www.postgresql.org/docs/current/static/pgupgrade.html)가 `.sql` 파일을 생성한 경우 지금 실행하세요.

- 새 서버를 시작합니다.

## PostGIS 업그레이드

PostGIS는 `EXTENSION` 메커니즘을 통해 사소한 업그레이드를 처리합니다. `CREATE EXTENSION postgis`를 사용하여 데이터베이스를 공간적으로 활성화한 경우 동일한 기능을 사용하여 데이터베이스를 업데이트할 수 있습니다.

먼저 데이터베이스에서 사용할 수 있도록 새 소프트웨어를 설치합니다.

그런 다음 SQL을 실행하여 PostGIS 확장을 업그레이드하세요.

```sql
-- If you are upgrading from PostGIS 2.5 or later
-- and want the latest installed version
SELECT postgis_extensions_upgrade();

-- If you are upgrading from an earlier version
-- you have to specifically turn on the version you want
ALTER EXTENSION postgis UPDATE TO '2.5.5';
```

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 39. 소프트웨어 업그레이드 (Software Upgrades)

PostgreSQL 및 PostGIS 버전을 최신 기능과 성능 패치가 적용된 상위 버전으로 안전하게 업그레이드하는 절차입니다.

---

## 1. PostGIS 확장 마이너/메이저 업그레이드

PostGIS 바이너리 패키지를 서버에 설치한 후, SQL 세션에서 다음 명령어를 실행하여 확장을 즉시 업그레이드합니다:

```sql
-- 최신 설치 버전으로 PostGIS 라이브러리 및 함수 자동 업그레이드
ALTER EXTENSION postgis UPDATE;
ALTER EXTENSION postgis_raster UPDATE;
ALTER EXTENSION postgis_topology UPDATE;
```

### 업그레이드 상태 점검:
```sql
SELECT postgis_extensions_upgrade();
SELECT postgis_full_version();
```

---

## 2. PostgreSQL 메이저 버전 업그레이드 (pg_upgrade)

PostgreSQL 메이저 버전(예: PG 14 -> PG 16) 전환 시에는 `pg_upgrade --link` 도구를 사용하여 데이터 복사 없이 수 초 만에 마이그레이션할 수 있습니다.

---

| [⬅️ 38. PostgreSQL 백업 및 복원 (PostgreSQL Backup and Restore)](38_backup.md) | [🏠 워크숍 목차](README.md) | [40. 고급 지오메트리 생성 (Advanced Geometry Constructions) ➡️](40_advanced_geometry_construction.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](38_backup.md) · [목차](00_index.md) · [다음 →](40_advanced_geometry_construction.md)