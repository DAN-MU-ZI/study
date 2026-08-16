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
