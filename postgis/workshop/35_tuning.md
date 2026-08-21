# 35. PostgreSQL 기본 튜닝 (Basic PostgreSQL Tuning)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/tuning.html>](https://postgis.net/workshops/postgis-intro/tuning.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostgreSQL의 기본 설정값(Default Configuration)은 최소 사양의 하드웨어에서도 안정적으로 구동되도록 매우 보수적으로 책정되어 있습니다.

하지만 GIS 공간 데이터베이스는 복잡한 다각형, 수만 개의 정점, 래스터 타일 등 일반적인 비공간 데이터보다 **레코드당 크기가 훨씬 크고 CPU 및 메모리 연산 집약적인 워크로드**를 가집니다. 따라서 프로덕션 환경에서 최적의 공간 쿼리 성능을 얻으려면 하드웨어 사양에 맞춰 데이터베이스 매개변수를 튜닝해야 합니다.

주요 설정 파일인 **`postgresql.conf`**를 텍스트 편집기로 수정하고 서비스를 재시작하여 적용합니다.

---

## 주요 튜닝 매개변수 가이드

### 1. shared_buffers (공유 메모리 버퍼)
PostgreSQL 서버 프로세스들이 디스크 I/O를 줄이고 데이터 페이지를 공유하기 위해 사용하는 메인 캐시 메모리입니다.
- **기본값**: 128MB (또는 32MB)
- **권장값**: **시스템 전체 RAM의 약 $25\%$** (예: 16GB RAM 서버인 경우 `4GB`, 64GB RAM 서버인 경우 `16GB`)

---

### 2. effective_cache_size (예상 가용 파일시스템 캐시)
비용 기반 쿼리 플래너(Query Planner)가 인덱스 스캔과 순차 스캔의 비용을 계산할 때, 운영체제(OS)의 가상 파일시스템 버퍼 캐시에 얼마나 많은 데이터가 상주해 있을지 예측하는 지표입니다. (실제 메모리를 직접 할당하는 것은 아님)
- **기본값**: 4GB
- **권장값**: **시스템 전체 RAM의 약 $50\% \sim 75\%$** (예: 16GB RAM 서버인 경우 `12GB`)

---

### 3. work_mem (작업 메모리)
`ORDER BY`, `DISTINCT`, 공간 조인, 해시 조인, 해시 집계 등의 연산을 디스크 임시 파일로 전환하지 않고 메모리 내에서 고속 처리하기 위해 할당하는 작업 메모리입니다.
- **기본값**: 4MB (또는 1MB)
- **권장값**: **`16MB` ~ `64MB`**
- *주의*: 이 메모리는 연결 세션당이 아니라 **쿼리 내의 개별 정렬/해시 연산마다 독립적으로 할당**되므로, 동시 접속자 수가 많은 환경에서는 시스템 전체 가용 메모리를 초과하지 않도록 주의해야 합니다.

---

### 4. maintenance_work_mem (유지보수 작업 메모리)
`VACUUM`, `CREATE INDEX`(GiST 인덱스 생성), `ALTER TABLE`, 외래 키 추가 등 대규모 배치 유지보수 작업에서 사용하는 메모리입니다.
- **기본값**: 64MB (또는 16MB)
- **권장값**: **`256MB` ~ `1GB`** (대용량 공간 인덱스를 생성할 때 일시적으로 세션 레벨에서 `SET maintenance_work_mem TO '1GB';`로 상향하면 인덱스 생성 속도가 획기적으로 향상됩니다.)

---

### 5. wal_buffers (WAL 버퍼)
트랜잭션 커밋 시 변경 사항을 기록하는 미리 쓰기 로그(WAL, Write-Ahead Logging) 전용 버퍼입니다. 공간 데이터는 트랜잭션 크기가 크므로 기본값보다 여유 있게 설정하는 것이 좋습니다.
- **기본값**: -1 (shared_buffers의 1/32로 자동 설정)
- **권장값**: **`16MB`**

---

### 6. random_page_cost (랜덤 디스크 읽기 비용 계수)
순차 페이지 읽기 비용(`seq_page_cost = 1.0`)을 기준으로 디스크의 무작위 페이지를 읽어오는 상대적 비용 계수입니다.
- **기본값**: `4.0` (회전형 자기 디스크 HDD 기준)
- **권장값**:
  - 고속 **NVMe / SATA SSD**: **`1.1` ~ `1.5`**
  - 고성능 Enterprise SAN / RAM 드라이브: **`1.0` ~ `1.1`**
- *효과*: SSD 환경에서 이 값을 1.1 수준으로 낮추면, 쿼리 플래너가 공간 인덱스(GiST)를 훨씬 더 적극적으로 활용하여 쿼리 응답 속도가 크게 개선됩니다.

---

## 설정 적용 및 서비스 재시작

`postgresql.conf` 수정 후 설정을 적용합니다.

- **Windows**: 서비스 관리자(`services.msc`)에서 **postgresql-x64-[버전]** 서비스를 찾아 *다시 시작(Restart)*을 클릭합니다.
- **Linux**: `sudo systemctl restart postgresql` 명령을 실행합니다.
- pgAdmin에서 데이터베이스 연결을 새로고침하여 튜닝된 파라미터가 정상 반영되었는지 확인합니다:

```sql
SHOW shared_buffers;
SHOW work_mem;
SHOW random_page_cost;
```


---

[← 이전](34_history_tracking.md) · [목차](00_index.md) · [다음 →](36_security.md)