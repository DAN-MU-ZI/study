# 36. PostgreSQL 보안 (PostgreSQL Security)

PostGIS 데이터베이스를 다중 사용자가 안전하게 공유할 수 있도록 사용자(Role), 권한(Grant), 행 단위 보안(RLS)을 구성하는 방법입니다.

---

## 1. 역할(Role/User) 생성

```sql
-- 읽기 전용 사용자 생성
CREATE ROLE gis_reader WITH LOGIN PASSWORD 'reader_password';

-- 편집 권한 사용자 생성
CREATE ROLE gis_editor WITH LOGIN PASSWORD 'editor_password';
```

---

## 2. 권한 부여 (GRANT)

```sql
-- 데이터베이스 접속 권한 부여
GRANT CONNECT ON DATABASE nyc TO gis_reader;

-- 스키마 사용 권한 부여
GRANT USAGE ON SCHEMA public TO gis_reader;

-- 모든 테이블 조회 권한 부여 (읽기 전용)
GRANT SELECT ON ALL TABLES IN SCHEMA public TO gis_reader;

-- 편집 사용자에게는 데이터 수정 권한까지 부여
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO gis_editor;
```

---

## 3. 행 단위 보안 (Row Level Security - RLS)

특정 사용자가 자신이 담당하는 구역의 데이터만 조회하거나 수정할 수 있도록 제한합니다:

```sql
-- 테이블 RLS 활성화
ALTER TABLE nyc_neighborhoods ENABLE ROW LEVEL SECURITY;

-- 맨해튼 담당자에게 맨해튼 데이터만 보이도록 정책 생성
CREATE POLICY manhattan_only ON nyc_neighborhoods
FOR ALL
TO manhattan_admin
USING (boroname = 'Manhattan');
```

---

| [⬅️ 35. PostgreSQL 기본 튜닝 (Basic PostgreSQL Tuning)](35_tuning.md) | [🏠 워크숍 목차](README.md) | [37. PostgreSQL 스키마 (PostgreSQL Schemas) ➡️](37_schemas.md) |
| :--- | :---: | ---: |
