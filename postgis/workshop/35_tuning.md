# 35. PostgreSQL 기본 튜닝 (Basic PostgreSQL Tuning)

PostgreSQL의 기본 설정값은 매우 보수적(최소한의 하드웨어 리소스 사용)으로 구성되어 있습니다. 대용량 공간 쿼리와 GIS 작업을 원활하게 처리하려면 `postgresql.conf` 설정 파일의 주요 파라미터를 하드웨어 사양에 맞게 최적화해야 합니다.

---

## 주요 튜닝 파라미터

### 1. `shared_buffers`
PostgreSQL이 데이터 캐싱을 위해 사용하는 공유 메모리 크기입니다.
- **권장값**: 전체 시스템 RAM의 **25%** (예: 16GB RAM 환경 -> `4GB`)

### 2. `work_mem`
정렬(`ORDER BY`), 집계(`DISTINCT`, `GROUP BY`), 해시 조인 작업 시 각 쿼리 연산 단위로 할당되는 메모리입니다.
- **권장값**: 16MB ~ 64MB (동시 접속자 수에 따라 조정)

### 3. `maintenance_work_mem`
인덱스 생성(`CREATE INDEX`), `VACUUM`, `CLUSTER` 등의 유지보수 작업에 사용되는 메모리입니다.
- **권장값**: 전체 시스템 RAM의 **5% ~ 10%** (예: 512MB ~ 2GB)

### 4. `effective_cache_size`
PostgreSQL 쿼리 플래너가 운영체제 파일시스템 캐시를 포함해 사용 가능한 디스크 캐시 크기를 가늠하는 추정치입니다.
- **권장값**: 전체 시스템 RAM의 **50% ~ 75%** (예: 16GB RAM 환경 -> `8GB ~ 12GB`)

### 5. `random_page_cost`
랜덤 디스크 I/O 비용에 대한 쿼리 플래너의 가중치입니다. 기본값은 HDD 기준인 4.0입니다.
- **SSD / NVMe 환경 권장값**: `1.1` ~ `2.0` (공간 인덱스 GiST 탐색을 플래너가 적극적으로 선택하도록 유도)

---

## 설정 적용 방법

`postgresql.conf` 수정 후 PostgreSQL 서비스를 재시작하거나 설정을 리로드합니다:

```sql
SELECT pg_reload_conf();
```

---

| [⬅️ 34. 트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers)](34_history_tracking.md) | [🏠 워크숍 목차](README.md) | [36. PostgreSQL 보안 (PostgreSQL Security) ➡️](36_security.md) |
| :--- | :---: | ---: |
