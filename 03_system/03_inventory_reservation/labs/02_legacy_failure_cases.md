# 실습 2: 레거시 실패 시나리오

## 목표

Redis와 MySQL이 같은 트랜잭션 안에서 변경되지 않을 때 어떤 불일치가 생기는지 확인한다.

## 준비

MySQL과 Redis를 초기화한다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
docker compose exec redis redis-cli FLUSHALL
```

Redis에 가용 수량을 저장한다.

```powershell
docker compose exec redis redis-cli SET available:1:100:1 100
```

## Case 1: Redis 예약 성공 후 MySQL 원장 차감 실패

Redis에서 예약을 먼저 성공시킨다.

```powershell
docker compose exec redis redis-cli DECRBY available:1:100:1 1
docker compose exec redis redis-cli SET reservation:r-fail-mysql 1 EX 300
```

이후 MySQL 원장 차감이 실패했다고 가정하고 아무 작업도 하지 않는다.

상태를 확인한다.

```powershell
docker compose exec redis redis-cli GET available:1:100:1
docker compose exec redis redis-cli GET reservation:r-fail-mysql
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

기록할 점:

- Redis 가용 수량은 줄어든다.
- MySQL 원장은 차감되지 않는다.
- 이 상태가 오래 유지되면 판매 가능한 재고가 Redis에서만 줄어든다.

## Case 2: MySQL 원장 차감 성공 후 Redis 예약 정리 실패

초기화한다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
docker compose exec redis redis-cli FLUSHALL
docker compose exec redis redis-cli SET available:1:100:1 100
```

Redis 예약과 MySQL 차감을 모두 수행한다.

```powershell
docker compose exec redis redis-cli DECRBY available:1:100:1 1
docker compose exec redis redis-cli SET reservation:r-fail-redis 1 EX 300
```

MySQL 원장을 차감한다.

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study -e "UPDATE inventory_ledger SET claimed_quantity = claimed_quantity + 1 WHERE shop_id = 1 AND inventory_item_id = 100 AND location_id = 1;"
```

Redis 예약 정리에 실패했다고 가정하고 예약 키를 삭제하지 않는다.

상태를 확인한다.

```powershell
docker compose exec redis redis-cli GET available:1:100:1
docker compose exec redis redis-cli GET reservation:r-fail-redis
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

기록할 점:

- MySQL 원장은 차감된다.
- Redis 예약 키는 남는다.
- 만료 처리에서 Redis 가용 수량을 다시 복구하면 실제 판매된 재고가 다시 예약 가능해질 수 있다.

## Case 3: Claim과 Release 경합

동일한 예약에 대해 결제 성공 처리와 만료 처리가 동시에 실행된다고 가정한다.

확인할 흐름:

- `Claim`은 MySQL 원장을 차감하려고 한다.
- `Release`는 Redis 가용 수량을 복구하려고 한다.
- 둘 중 어떤 처리가 먼저 끝났는지에 따라 최종 상태가 달라질 수 있다.

기록할 표:

| 항목 | Redis | MySQL |
|---|---|---|
| 초기 상태 |  |  |
| Reserve 이후 |  |  |
| Claim 이후 |  |  |
| Release 이후 |  |  |
| 최종 상태 |  |  |

## 정리 질문

- 어떤 실패가 언더셀을 만든다고 볼 수 있는가?
- 어떤 실패가 오버셀 가능성을 만든다고 볼 수 있는가?
- 복구 기준을 Redis로 둘 때와 MySQL로 둘 때 결과가 어떻게 달라지는가?
