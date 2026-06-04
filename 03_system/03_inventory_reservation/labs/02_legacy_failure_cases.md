# 실습 2: 레거시 실패 시나리오

## 목표

Redis와 MySQL이 같은 트랜잭션 안에서 변경되지 않을 때 어떤 불일치가 생기는지 확인한다.

## Shopify 기준 용어

- 오버셀: 결제 또는 주문은 성공했지만 MySQL 원장 차감이 반영되지 않아, 이미 팔린 재고가 다시 판매 가능해지는 상태.
- 언더셀: 실제로는 판매 가능한 재고가 있는데 Redis 예약 또는 원장 차감 상태가 과하게 남아 품절처럼 보이는 상태.

![오버셀·언더셀 판정 기준](../diagrams/oversell-undersell-classification.svg)

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

## Case 1(오버셀 위험): 결제 성공 후 MySQL 원장 차감 실패

Redis에서 예약을 먼저 성공시킨다.

```powershell
docker compose exec redis redis-cli DECRBY available:1:100:1 1
docker compose exec redis redis-cli SET reservation:r-fail-mysql 1 EX 300
```

결제 또는 주문은 성공했지만, 이후 MySQL 원장 차감이 실패했다고 가정하고 아무 작업도 하지 않는다.

상태를 확인한다.

```powershell
docker compose exec redis redis-cli GET available:1:100:1
docker compose exec redis redis-cli GET reservation:r-fail-mysql
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

기록할 점:

- Redis 가용 수량은 줄어든다.
- 결제 또는 주문은 성공한 상태다.
- MySQL 원장은 차감되지 않는다.
- Redis 예약이 만료되거나 정리되면, 이미 팔린 재고가 다시 예약 가능해진다.
- Shopify 기준으로는 이미 팔린 재고가 원장에 차감되지 않은 상태이므로 오버셀 위험이다.
- 결제 전 Redis 예약만 남아 있는 순간 상태는 일시적으로 언더셀처럼 보일 수 있다. 이 케이스의 핵심은 결제 성공 후 원장 차감 실패다.

## Case 2(언더셀 위험): MySQL 원장 차감 성공 후 Redis 예약 정리 실패

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
- Redis 기준 가용 수량은 이미 줄어든 상태로 유지된다.
- 실제 판매 가능 수량보다 시스템이 더 적게 보게 되어 품절처럼 판단할 수 있다.
- Shopify 기준으로는 차감된 재고가 여전히 예약 상태로 남은 것이므로 언더셀 위험이다.

추가 변형:

- 이미 Claim된 예약을 만료 작업이 Release하면 Redis 가용 수량이 잘못 복구된다.
- 이 경우에는 실제 판매된 재고가 다시 예약 가능해지므로 오버셀 위험으로 전이된다.

## Case 3: Claim과 Release 경합

동일한 예약에 대해 결제 성공 처리와 만료 처리가 동시에 실행된다고 가정한다.

경합 자체는 오버셀 또는 언더셀로 바로 분류하지 않는다. 최종 상태로 분류한다.

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

판정 기준:

| 최종 상태 | 분류 |
|---|---|
| 결제 성공, MySQL 원장 차감 실패 | 오버셀 |
| MySQL 원장 차감 성공, Redis 예약이 계속 남음 | 언더셀 |
| MySQL 원장 차감 성공, Release가 Redis 가용 수량까지 복구 | 오버셀 |
| 결제 실패 또는 만료 후 Redis 예약만 남음 | 언더셀 |

## 정리 질문

- 어떤 실패가 언더셀을 만든다고 볼 수 있는가?
- 어떤 실패가 오버셀 가능성을 만든다고 볼 수 있는가?
- 복구 기준을 Redis로 둘 때와 MySQL로 둘 때 결과가 어떻게 달라지는가?
