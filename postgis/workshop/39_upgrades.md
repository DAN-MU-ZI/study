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
