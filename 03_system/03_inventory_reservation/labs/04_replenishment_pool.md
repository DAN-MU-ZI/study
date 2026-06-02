# 실습 4: 예약 가능 Pool 보충 시스템

## 목표

`reservation_units`가 줄어들었을 때 `inventory_ledger`를 기준으로 예약 가능한 row pool을 보충하는 흐름을 확인한다.

## 준비

MySQL 데이터를 초기화한다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

초기 상태를 확인한다.

```powershell
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

## Step 1: Pool 줄이기

예약이 여러 번 발생했다고 가정하고 `reservation_units` 일부를 제거한다.

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study -e "DELETE FROM reservation_units WHERE shop_id = 1 AND inventory_item_id = 100 AND location_id = 1 AND unit_id <= 8;"
```

상태를 확인한다.

```powershell
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

확인할 점:

- `reservation_units` 개수가 줄어든다.
- `replenishment_state`의 `target_pool_size`와 현재 pool 크기를 비교한다.

## Step 2: 보충 1회 실행

보충 SQL을 1회 실행한다.

```powershell
Get-Content .\labs\sql\02_replenish_once.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

상태를 다시 확인한다.

```powershell
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

확인할 점:

- `reservation_units`에 새 `unit_id`가 추가된다.
- `replenishment_state.next_unit_id`가 증가한다.
- 보충은 원장 재고 범위 안에서만 수행되어야 한다.

## Step 3: 동시 보충 경합 확인

두 개의 MySQL 세션을 연다.

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study
```

`Session A`에서 보충 상태 row를 잠근다.

```sql
START TRANSACTION;

SELECT *
FROM replenishment_state
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
FOR UPDATE;
```

`Session A`는 커밋하지 않고 유지한다.

`Session B`에서 같은 row를 잠그려고 시도한다.

```sql
START TRANSACTION;

SELECT *
FROM replenishment_state
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
FOR UPDATE;
```

확인할 점:

- `Session B`는 `Session A`가 커밋 또는 롤백할 때까지 기다린다.
- 같은 상품과 위치에 대한 보충 작업은 직렬화된다.
- 중복 `unit_id` 생성을 막을 수 있다.

두 세션을 정리한다.

```sql
ROLLBACK;
```

## 정리 질문

- 보충 작업은 왜 예약 작업보다 뒤에서 다루는가?
- pool 크기가 너무 작으면 어떤 문제가 생기는가?
- pool 크기가 너무 크면 어떤 비용이 생기는가?
- 보충 작업과 예약 작업이 동시에 실행될 때 어떤 row를 잠가야 하는가?
