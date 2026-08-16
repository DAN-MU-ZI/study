# 제 3 장. PostGIS 관리 (Administration)

공간 데이터베이스의 생성, 확장 프로그램 활성화, 백업 및 업그레이드 관리 절차입니다.

---

## 3.1. 공간 데이터베이스 활성화

데이터베이스 생성 후 `CREATE EXTENSION` 명령으로 필요한 모듈을 로드합니다:

```sql
-- 1. 기본 벡터 공간 확장
CREATE EXTENSION postgis;

-- 2. (선택) 래스터 확장
CREATE EXTENSION postgis_raster;

-- 3. (선택) 토폴로지 확장
CREATE EXTENSION postgis_topology;

-- 4. (선택) 3D SFCGAL 확장
CREATE EXTENSION postgis_sfcgal;
```

### 설치 상태 점검
```sql
SELECT postgis_full_version();
```

---

## 3.2. 업그레이드 관리

설치된 패키지가 업데이트된 경우, 기존 데이터베이스 세션에서 확장을 갱신합니다:

```sql
-- 확장 바이너리 및 함수 정의 일괄 업그레이드
ALTER EXTENSION postgis UPDATE;
ALTER EXTENSION postgis_raster UPDATE;
ALTER EXTENSION postgis_topology UPDATE;

-- 업그레이드 정상 완료 검증
SELECT postgis_extensions_upgrade();
```

---

| [⬅️ 제 2 장. PostGIS 설치 (Installation)](02_installation.md) | [🏠 매뉴얼 목차](README.md) | [제 4 장. 공간 데이터 관리 (Data Management) ➡️](04_data_management.md) |
| :--- | :---: | ---: |
