# 38. PostgreSQL 백업 및 복원 (PostgreSQL Backup and Restore)

대용량 공간 데이터베이스의 데이터 손실을 방지하기 위한 정기적인 백업 및 복구 절차입니다.

---

## 1. pg_dump를 사용한 백업

`pg_dump`는 PostgreSQL의 표준 데이터베이스 백업 유틸리티입니다.

### 커스텀 압축 포맷 백업 (권장):
```bash
pg_dump -U postgres -Fc -b -v -f nyc_backup.dump nyc
```
- `-Fc`: pg_restore로 유연하게 복원 가능한 고성능 압축 바이너리 포맷
- `-b`: 대용량 Large Object 포함
- `-v`: 상세 진행 상황 출력(Verbose)

### 특정 공간 테이블만 백업:
```bash
pg_dump -U postgres -Fc -t nyc_streets -f nyc_streets.dump nyc
```

---

## 2. pg_restore를 사용한 복원

```bash
# 새 데이터베이스 생성 후 덤프 파일 복원
createdb -U postgres nyc_restore
pg_restore -U postgres -d nyc_restore -v nyc_backup.dump
```

멀티코어 병렬 복원(`-j` 옵션)을 사용하면 대용량 공간 인덱스 재생성 시간을 대폭 단축할 수 있습니다:
```bash
pg_restore -U postgres -d nyc_restore -j 4 nyc_backup.dump
```

---

| [⬅️ 37. PostgreSQL 스키마 (PostgreSQL Schemas)](37_schemas.md) | [🏠 워크숍 목차](README.md) | [39. 소프트웨어 업그레이드 (Software Upgrades) ➡️](39_upgrades.md) |
| :--- | :---: | ---: |
