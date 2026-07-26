# Stack Overflow PostgreSQL DB Case Study

Stack Overflow 2010 데이터를 PostgreSQL 18.4로 변환해 쿼리 작성, 실행 계획 분석, 인덱싱, 부하 테스트, MVCC와 잠금을 학습하는 로컬 실습 환경이다.

## 구성

- `sqlserver`: 마이그레이션과 행 수 검증 때만 실행되는 일회성 원본
- `migrator`: pgloader로 SQL Server 데이터를 PostgreSQL에 이관
- `postgres`: 변경하지 않는 `stackoverflow_base`와 실습용 `stackoverflow_lab`
- `api`: Spring Boot 4.1 + JDBC + HikariCP 검색 API
- `k6`: 동일 API 쿼리를 반복하는 부하 생성기
- `observability`: `pg_stat_statements`, 세션, 잠금, 인덱스 사용량 보고서

원본 7z는 보존된다. 실습 중 스키마나 데이터를 변경해도 `stackoverflow_base`에서 `stackoverflow_lab`을 다시 복제할 수 있다.

## 사전 준비

- Docker Desktop 실행
- Docker 메모리 6GiB 이상
- 저장 공간 40GB 이상
- `StackOverflow2010.7z`를 현재 디렉터리에 배치

호스트에 `7z`가 있으면 이를 사용한다. 없으면 `extractor` 컨테이너를 빌드해 압축을 해제한다.
압축 해제본은 현재 D 드라이브 폴더에 저장되지만 PostgreSQL named volume은 Docker Desktop 가상 디스크에 저장된다. Docker 가상 디스크에도 최소 20GB의 여유 공간을 확보한다.

## 데이터 준비

### 최초 실행

```powershell
Set-Location D:\_Workspace\02_프로젝트\진행중\study\01_db\00_case_study
.\scripts\setup.ps1 -Stage Full
```

스크립트는 다음 작업을 수행한다.

1. 7z를 `data/sqlserver`에 압축 해제한다.
2. SQL Server 2022 Developer에 MDF/LDF를 연결하고 `DBCC CHECKDB`를 수행한다.
3. pgloader로 `stackoverflow_base`에 이관한다.
4. 식별자를 PostgreSQL 방식의 소문자로 정규화하고 통계를 갱신한다.
5. 양쪽 DB의 테이블별 행 수를 비교한다.
6. SQL Server 컨테이너와 전용 named volume을 제거한다.
7. 실습용 `stackoverflow_lab`을 생성한다.

현재 데이터와 Docker Desktop 환경에서는 최초 실행에 약 60~70분이 걸릴 수 있다. 주요 구간은 pgloader 약 40분, 실습 DB 복제 약 11분이었다. 각 단계 시작 시 누적 경과 시간이 출력된다.

비밀번호는 최초 실행 때 생성되는 `.env`에서 바꿀 수 있다. pgloader 연결 문자열 제약 때문에 영문자, 숫자, `!._-`만 사용한다.

### 실패 후 재개

pgloader가 성공한 뒤 스키마 후처리나 행 수 검증에서 실패했다면 다음 명령으로 재개한다.

```powershell
.\scripts\setup.ps1 -Stage PostMigration -SkipExtraction
```

이 경로는 SQL Server 무결성 검사와 pgloader를 반복하지 않는다. pgloader 자체가 실패했으면 부분 적재 DB를 폐기해야 하므로 `-Stage Full`로 다시 실행한다.

정상 완료 후 SQL Server 컨테이너와 전용 데이터 볼륨은 자동으로 제거된다. 장애 분석을 위해 일시적으로 남겨야 할 때만 `-KeepSqlServer`를 사용한다. SQL Server 이미지는 재실행 시 다운로드를 줄이기 위해 Docker 이미지 캐시에 남는다.

실습 DB만 다시 만들 때는 다음 명령을 사용한다.

```powershell
.\scripts\setup.ps1 -Stage LabOnly
```

실제 장애와 소요 시간은 [마이그레이션 복기](docs/retrospective-2026-07-20.md)에 기록했다.

## API와 부하 실행

```powershell
docker compose --profile app up -d --build api
Invoke-RestMethod 'http://localhost:8080/api/posts?tag=postgresql&page=0&pageSize=20'

.\scripts\run-load.ps1 -Tag java -VirtualUsers 20 -Duration 30s
```

부하 실행마다 `artifacts/k6-*.txt`와 `artifacts/db-report-*.txt`가 생성된다. 인덱스 적용 전후에는 태그, VU, 실행 시간을 동일하게 유지한다.

## 실습 초기화

```powershell
.\scripts\reset-lab.ps1
```

`stackoverflow_lab`의 연결을 종료하고 `stackoverflow_base`에서 다시 복제한다. 최초 SQL Server → PostgreSQL 변환은 반복하지 않는다.

## 케이스 진행 순서

| 단계 | 주제 | 완료 기준 |
|---|---|---|
| 01 | 요구사항을 SQL로 변환 | 조건·정렬·경계값을 쿼리로 표현 |
| 02 | 조인과 집계 | 사용자별 답변 성과를 정확히 계산 |
| 03 | 윈도 함수 | 월별 순위를 한 쿼리로 산출 |
| 04 | 실행 계획과 SARGability | 함수 조건을 범위 조건으로 고쳐 비용 비교 |
| 05 | B-tree 설계 | 조건·정렬 순서에 맞는 복합 인덱스 설계 |
| 06 | GIN과 trigram | 선행 와일드카드 검색을 인덱스로 개선 |
| 07 | API 부하 튜닝 | 동일 부하 전후의 p95와 DB 지표 비교 |
| 08 | MVCC·잠금·풀 | 블로킹을 재현하고 세션·잠금에서 원인 확인 |

각 디렉터리의 `problem.md`를 먼저 풀고, 필요할 때 `hint.md`, 마지막에 `solution.sql`을 확인한다. 해설 SQL은 유일한 정답이 아니라 검증 가능한 기준안이다.

## 데이터 출처

Stack Overflow 공개 데이터의 SQL Server 변환본을 사용한다. 배포 페이지 설명에 따르면 2010 Small 데이터는 약 10GB로 확장되며 SQL Server 2016 형식이다. 데이터 라이선스는 CC BY-SA 4.0이다.

- https://www.brentozar.com/archive/2015/10/how-to-download-the-stack-overflow-database-via-bittorrent/
- https://pgloader.readthedocs.io/en/latest/ref/mssql.html
- https://www.postgresql.org/docs/current/
