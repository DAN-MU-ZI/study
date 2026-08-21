# 39. 소프트웨어 업그레이드 (Software Upgrades)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/upgrades.html>](https://postgis.net/workshops/postgis-intro/upgrades.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostGIS는 PostgreSQL RDBMS 내에 확장(Extension)으로 설치되어 구동되므로, 업그레이드는 **PostgreSQL 데이터베이스 엔진 업그레이드**와 **PostGIS 확장 라이브러리 업그레이드** 두 가지 측면에서 진행됩니다.

---

## 1. PostgreSQL 업그레이드

### 1) 마이너 버전 업그레이드 (Minor Upgrade / 패치 업데이트)
- 예: PostgreSQL `15.1` $\rightarrow$ `15.4`, `16.0` $\rightarrow$ `16.2`
- **특징**: 내부 저장 구조나 DDL 문법 변경 없이 버그 수정 및 보안 패치만 포함됩니다.
- **절차**: 서비스를 중지하고 최신 패치 패키지를 덮어씌운 뒤 서비스를 다시 시작하기만 하면 됩니다.

---

### 2) 메이저 버전 업그레이드 (Major Upgrade)
- 예: PostgreSQL `14` $\rightarrow$ `16`
- **특징**: 시스템 카탈로그와 내부 데이터 페이지 물리 포맷이 변경될 수 있으므로 마이그레이션 절차가 필요합니다.

#### 방법 A: 덤프 및 복원 (Dump & Restore)
- `pg_dumpall`로 전체 데이터를 텍스트/바이너리로 덤프한 후 신규 버전 데이터베이스에 복원합니다.
- OS나 CPU 아키텍처가 바뀌는 대규모 서버 이전 시 가장 안정적인 표준 방식입니다.

#### 방법 B: pg_upgrade 도구를 활용한 고속 인플레이스 업그레이드 (권장)
- 대용량 데이터베이스(수백 GB ~ 수 TB)를 덤프/복원 없이 파일 시스템의 하드 링크(`--link`)를 사용하여 단 수 분 만에 업그레이드합니다.

```sh
pg_upgrade \
  --old-datadir "/var/lib/postgresql/14/data" \
  --new-datadir "/var/lib/postgresql/16/data" \
  --old-bindir "/usr/lib/postgresql/14/bin" \
  --new-bindir "/usr/lib/postgresql/16/bin" \
  --link
```

---

## 2. PostGIS 확장 업그레이드 (PostGIS Upgrades)

PostgreSQL의 `EXTENSION` 메커니즘을 사용하므로, OS 패키지 관리자로 새 버전의 PostGIS 공유 라이브러리를 설치한 뒤 SQL 함수 하나로 즉시 업그레이드할 수 있습니다.

### 최신 버전으로 원클릭 업그레이드 (PostGIS 3+)

```sql
-- 설치된 모든 PostGIS 관련 확장(postgis, postgis_raster, postgis_topology 등)을 최신 버전으로 일괄 업그레이드
SELECT postgis_extensions_upgrade();
```

### 특정 타겟 버전으로 수동 지정 업그레이드

```sql
ALTER EXTENSION postgis UPDATE TO '3.4.2';
ALTER EXTENSION postgis_raster UPDATE TO '3.4.2';
ALTER EXTENSION postgis_topology UPDATE TO '3.4.2';
```

### 현재 설치된 버전 확인

```sql
SELECT postgis_full_version();
```


---

[← 이전](38_backup.md) · [목차](00_index.md) · [다음 →](40_advanced_geometry_construction.md)