# 36. PostgreSQL 보안 및 권한 관리 (PostgreSQL Security)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/security.html>](https://postgis.net/workshops/postgis-intro/security.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostgreSQL은 세분화된 **역할(Role) 기반 접근 제어(RBAC)** 시스템과 유연한 인증 인프라(`pg_hba.conf`), 그리고 강력한 데이터 암호화 기능을 제공합니다.

---

## 1. 사용자와 역할 (Users and Roles)

PostgreSQL에서 사용자와 역할은 본질적으로 동일한 객체입니다. "사용자(User)"는 단순히 **로그인 권한(`LOGIN`)을 가진 역할**을 의미합니다.

```sql
-- 아래 두 SQL 문은 완전히 동일하게 동작합니다.
CREATE ROLE app_user WITH LOGIN;
CREATE USER app_user;
```

실무에서는 권한 템플릿 역할을 하는 **그룹 역할(Role)**을 먼저 생성하고, 실제 계정(User)에 해당 역할을 부여(Grant)하여 권한을 체계적으로 상속 관리합니다.

---

## 2. 실습: 공간 데이터베이스 역할 모델링

### 1) 읽기 전용 역할: postgis_reader
웹 지도 서비스나 대시보드 애플리케이션을 위한 읽기 전용 역할입니다.

```sql
-- 일반 PostGIS 읽기 전용 역할 생성
CREATE ROLE postgis_reader INHERIT;

-- PostGIS 필수 메타데이터 뷰 및 좌표계 테이블 읽기 권한 부여
GRANT SELECT ON geometry_columns TO postgis_reader;
GRANT SELECT ON geography_columns TO postgis_reader;
GRANT SELECT ON spatial_ref_sys TO postgis_reader;

-- 웹 애플리케이션 사용자 계정 생성 및 역할 부여
CREATE USER app1 WITH PASSWORD 'secret123';
GRANT postgis_reader TO app1;

-- 특정 비즈니스 공간 테이블에 대한 조회 권한 부여
GRANT SELECT ON nyc_streets TO app1;
```

> [!NOTE]
> `spatial_ref_sys` 테이블에 대한 `SELECT` 권한이 없으면 `ST_Transform` 좌표계 변환 함수 호출 시 `permission denied for relation spatial_ref_sys` 오류가 발생합니다. 따라서 모든 PostGIS 조회 계정에는 메타데이터 테이블 접근 권한이 필수적입니다.

---

### 2) 읽기/쓰기 역할: postgis_writer
GIS 분석가 및 개발자를 위한 테이블 생성 및 편집 역할입니다.

```sql
-- 쓰기 전용 역할 생성 및 postgis_reader 권한 상속
CREATE ROLE postgis_writer;
GRANT postgis_reader TO postgis_writer;

-- 메타데이터 수정 권한 부여
GRANT INSERT, UPDATE, DELETE ON spatial_ref_sys TO postgis_writer;

-- 데이터 테이블에 대한 DML 쓰기 권한 부여
GRANT INSERT, UPDATE, DELETE ON nyc_streets TO app1;
```

---

## 3. 네트워크 전송 및 데이터 암호화

### 1) SSL/TLS 전송 구간 암호화
`postgresql.conf`에서 `ssl = on`을 활성화하고 서버 인증서(`server.crt`)와 개인키(`server.key`)를 구성하면 클라이언트-서버 간 모든 패킷이 TLS로 암호화됩니다.

pgAdmin 및 클라이언트 연결 설정에서 SSL 모드를 `require` 또는 `verify-full`로 지정하여 안전하게 통신합니다.

![PostgreSQL 서버 연결에서 SSL 속성을 설정하는 화면](screenshots/ssl_props.jpg)

### 2) pgcrypto 모듈을 통한 컬럼 레벨 암호화
민감한 개인정보나 기밀 공간 속성은 `pgcrypto` 확장의 대칭키 암호화(AES, Blowfish)로 암호화하여 저장할 수 있습니다.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 문자열 암호화 및 복호화 실습
SELECT decrypt(
  encrypt('보호할 민감한 공간 정보', 'MySecretKey', 'aes'),
  'MySecretKey',
  'aes'
);
```

---

## 4. 호스트 기반 클라이언트 인증 (pg_hba.conf)

클라이언트의 IP 대역과 인증 방식은 PostgreSQL 데이터 디렉터리의 **`pg_hba.conf`** 파일에서 제어합니다.

```text
# TYPE  DATABASE    USER        CIDR-ADDRESS          METHOD
# 1. 로컬 소켓 연결 (신뢰 모드)
local   all         all                               trust

# 2. 로컬호스트 IPv4/IPv6 연결 (비밀번호 인증)
host    all         all         127.0.0.1/32          scram-sha-256
host    all         all         ::1/128               scram-sha-256

# 3. 특정 사내 네트워크(192.168.1.x)에서 nyc DB 접근 시 LDAP 인증
host    nyc         all         192.168.1.0/24        ldap ...
```

- **`scram-sha-256` / `md5`**: 안전한 패스워드 해시 인증
- **`gssapi` / `sspi`**: Kerberos 및 Windows Active Directory 통합 인증
- **`cert`**: 클라이언트 SSL 인증서 기반 비밀번호 없는 상호 인증


---

[← 이전](35_tuning.md) · [목차](00_index.md) · [다음 →](37_schemas.md)
