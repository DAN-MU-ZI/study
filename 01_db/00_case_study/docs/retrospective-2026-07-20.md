# Stack Overflow PostgreSQL 마이그레이션 복기

## 결과

- 일시: 2026-07-20
- 원본: Stack Overflow 2010 SQL Server MDF/LDF
- 대상: PostgreSQL 18.4
- 대상 DB: `stackoverflow_base`, `stackoverflow_lab`
- PostgreSQL DB 크기: 각각 약 4,615MB
- 검증 결과: 9개 테이블, 19,310,703행 모두 일치

| 테이블 | SQL Server | PostgreSQL |
|---|---:|---:|
| badges | 1,102,019 | 1,102,019 |
| comments | 3,875,183 | 3,875,183 |
| linktypes | 2 | 2 |
| postlinks | 161,519 | 161,519 |
| posts | 3,729,195 | 3,729,195 |
| posttypes | 8 | 8 |
| users | 299,398 | 299,398 |
| votes | 10,143,364 | 10,143,364 |
| votetypes | 15 | 15 |

## 실제 소요 시간

관측한 주요 구간은 다음과 같다. Docker Desktop의 Windows 파일 I/O와 최초 이미지 다운로드 시간에 따라 달라질 수 있다.

| 구간 | 이번 실행 |
|---|---:|
| MDF/LDF를 SQL Server named volume으로 최초 복사 | 약 8분 45초 |
| pgloader 데이터·인덱스 이관 | 약 40분 |
| PostgreSQL 스키마 정규화와 `ANALYZE` | 약 5분 |
| 양쪽 DB의 정확한 행 수 비교 | 약 2분 45초 |
| `stackoverflow_lab` 템플릿 복제 | 약 11분 |

최초 정상 경로도 약 60~70분이 필요하다. 9GB MDF, 1,931만 행, PostgreSQL 데이터·인덱스 약 4.6GB를 처리하고 검증용 DB까지 복제하기 때문이다.

## 시간이 더 길어진 원인

### 1. 비싼 단계 전에 환경 제약을 충분히 검증하지 않았다

- SQL Server 최초 기동 시 시스템 DB 업그레이드에 약 3분이 걸렸지만 health 대기 시간이 2분이었다.
- Windows bind mount의 MDF/LDF가 컨테이너에서 `root:root`, `0644`로 보여 SQL Server가 파일을 갱신할 수 없었다.
- bind mount에서는 `DBCC CHECKDB` 내부 스냅샷 생성이 운영체제 오류 31로 실패했다.
- Docker 전체 메모리는 약 7.8GiB였고 SQL Server가 5GiB 이상 사용했다. pgloader 기본 SBCL 힙 1GiB는 메타데이터·복사 단계에서 소진됐다.

이 제약들은 전체 실행 전에 짧은 preflight로 확인할 수 있었다.

### 2. 재개 지점이 없었다

기존 `setup.ps1`은 압축 해제, SQL Server 검사, pgloader, 후처리, 검증, 실습 DB 복제를 한 흐름으로 묶었다. 후처리에서 실패해도 사용자가 어느 명령부터 재개해야 하는지 알기 어려웠다.

### 3. 후처리 SQL을 작은 스키마에서 먼저 검증하지 않았다

- 소유 시퀀스를 테이블보다 먼저 다른 스키마로 이동해 PostgreSQL 오류가 발생했다.
- SQL Server 행 수 조회에 `sys.dm_db_partition_stats.rows`를 사용했다. 올바른 컬럼은 `row_count`다.

두 오류 모두 작은 임시 테이블과 정적 쿼리 검사로 먼저 잡을 수 있었다.

### 4. 통계 값을 잘못 해석해 추가 작업을 수행했다

템플릿 복제 직후 `pg_stat_user_tables.n_live_tup`가 0으로 표시됐다. 이 값은 누적 통계 뷰의 추정치이며, 템플릿에 포함된 플래너 통계 확인에는 `pg_class.reltuples`가 적합하다. 실습 DB에서 전체 `ANALYZE`를 한 번 더 실행할 필요가 없었다.

### 5. 긴 단계의 가시성이 부족했다

도구를 통해 PowerShell을 실행하는 동안 Docker 출력이 종료 시점까지 버퍼링됐다. 별도 `docker stats`, `pg_stat_activity`, `pg_stat_progress_create_index` 조회가 필요했고 진행 확인 메시지가 많아졌다.

## 적용한 개선

### 저장소와 컨테이너

- SQL Server 데이터 파일을 Windows bind mount에서 `sqlserver-data` named volume으로 옮겼다.
- D 드라이브 압축 해제본은 `/source:ro`로 연결하고, 크기가 다를 때만 SQL Server 볼륨으로 복사한다.
- SQL Server 메모리를 3,072MB로 제한했다.
- pgloader SBCL 동적 힙을 4,096MB로 늘렸다.
- SQL Server health 대기를 600초로 늘리고 timeout 시 최근 로그를 출력한다.
- 행 수 검증이 성공하면 SQL Server 컨테이너와 전용 named volume을 제거한다.
- SQL Server를 migration 전용 Compose 프로필로 지정해 기본 실행 대상에서 제외한다.

### 설치 단계

`setup.ps1`에 다음 단계를 추가했다.

| 단계 | 용도 |
|---|---|
| `Full` | 최초 압축 해제 확인, SQL Server 검사, pgloader, 후처리, 검증, 실습 DB 생성 |
| `PostMigration` | pgloader 성공 후 후처리 또는 행 수 검증에서 실패했을 때 재개 |
| `LabOnly` | 검증된 base에서 실습 DB만 다시 생성 |

- `DBCC CHECKDB`를 attach SQL과 분리해 `Full`에서만 실행한다.
- 각 단계 시작 시 누적 경과 시간을 출력한다.
- 오류 메시지에 재개 명령을 포함한다.
- 실습 DB 복제 후 중복 `ANALYZE`를 제거했다.
- 검증용 추정 행 수는 `pg_class.reltuples`를 사용한다.
- PostgreSQL 실습 중에는 SQL Server를 실행하지 않는다. D 드라이브의 원본 압축 파일과 압축 해제본은 재이관을 위해 보존한다.

### 검증

- pgloader는 9개 테이블, 19,310,703행을 오류 0건으로 처리했다.
- SQL Server 메타데이터 행 수와 PostgreSQL의 정확한 `COUNT(*)`를 테이블별로 비교했다.
- `posts`, `users`, `comments`, `votes` 존재와 주요 테이블의 비어 있지 않은 상태를 확인했다.

## 다음 실행 기준

### 최초 실행 또는 pgloader 실패

```powershell
.\scripts\setup.ps1 -Stage Full
```

pgloader가 중간에 종료되면 대상 DB가 부분 적재 상태일 수 있으므로 `Full`로 다시 시작한다.

### pgloader 성공 후 후처리 실패

```powershell
.\scripts\setup.ps1 -Stage PostMigration -SkipExtraction
```

이 경로는 DBCC와 pgloader를 반복하지 않는다.

### 실습 DB만 초기화

```powershell
.\scripts\setup.ps1 -Stage LabOnly
```

또는 다음 단축 스크립트를 사용한다.

```powershell
.\scripts\reset-lab.ps1
```

현재 데이터 크기에서 실습 DB 복제는 약 10분 이상 걸릴 수 있다.

## 남은 개선 후보

- 최초 설치 전에 Docker 메모리, Docker 가상 디스크 여유 공간, MDF/LDF 크기를 표 형태로 출력한다.
- pgloader 로그를 파일로 저장하고 마지막 성공 단계를 상태 파일로 기록한다.
- 실습 DB 초기화 시간이 부담되면 DB 전체 복제 대신 케이스별 스키마 또는 필요한 테이블만 복원하는 방식을 별도 실험한다.
