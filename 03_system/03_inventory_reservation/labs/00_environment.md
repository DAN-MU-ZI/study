# 실습 0: Docker 환경 준비

## 목표

MySQL 8과 Redis를 Docker Compose로 실행한다. 이후 모든 실습은 이 환경에서 진행한다.

## 구성

| 서비스 | 컨테이너 | 호스트 포트 | 용도 |
|---|---|---:|---|
| MySQL | `inventory-reservation-mysql` | `13306` | 원장, 예약 row, 보충 상태 |
| Redis | `inventory-reservation-redis` | `16379` | 레거시 예약 수량 |

## 실행

프로젝트 디렉터리로 이동한다.

```powershell
cd D:\_Workspace\02_프로젝트\진행중\study\03_system\03_inventory_reservation
```

컨테이너를 실행한다.

```powershell
docker compose up -d
```

상태를 확인한다.

```powershell
docker compose ps
```

## MySQL 접속

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study
```

상태 조회 SQL 파일을 실행한다.

```powershell
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

## Redis 접속

```powershell
docker compose exec redis redis-cli
```

Redis CLI에서 연결을 확인한다.

```text
PING
```

## 실습 데이터 초기화

MySQL 데이터를 초기 상태로 되돌린다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

Redis 데이터를 초기 상태로 되돌린다.

```powershell
docker compose exec redis redis-cli FLUSHALL
```

## 종료

컨테이너를 중지한다.

```powershell
docker compose down
```

볼륨까지 삭제하려면 다음 명령을 사용한다.

```powershell
docker compose down -v
```
