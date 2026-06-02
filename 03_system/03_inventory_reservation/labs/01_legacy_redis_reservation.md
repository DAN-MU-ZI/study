# 실습 1: Redis 기반 레거시 예약 흐름

## 목표

Redis에 가용 수량을 저장하고, 예약 시 수량을 차감하는 레거시 흐름을 확인한다.

## 준비

Docker 환경을 실행한다.

```powershell
docker compose up -d
```

Redis를 초기화한다.

```powershell
docker compose exec redis redis-cli FLUSHALL
```

MySQL도 초기화한다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

## Step 1: Redis에 가용 수량 저장

Redis CLI에 접속한다.

```powershell
docker compose exec redis redis-cli
```

상품 `100`, 위치 `1`의 가용 수량을 100으로 저장한다.

```text
SET available:1:100:1 100
GET available:1:100:1
```

확인할 점:

- Redis가 재고별 가용 수량을 단일 counter처럼 다룬다.
- 아직 MySQL 원장은 변경되지 않는다.

## Step 2: Reserve 수행

예약 1개를 만든다고 가정하고 Redis 가용 수량을 1 차감한다.

```text
DECRBY available:1:100:1 1
SET reservation:r-001 1 EX 300
GET available:1:100:1
GET reservation:r-001
TTL reservation:r-001
```

확인할 점:

- Redis 가용 수량은 99가 된다.
- 예약 키에는 TTL이 붙는다.
- MySQL 원장의 `claimed_quantity`는 아직 0이다.

## Step 3: Claim 수행

결제 성공 후 MySQL 원장을 차감한다고 가정한다.

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study
```

MySQL에서 원장 수량을 차감하고 예약 로그를 남긴다.

```sql
UPDATE inventory_ledger
SET claimed_quantity = claimed_quantity + 1
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1;

INSERT INTO legacy_reservation_log (
  reservation_id,
  shop_id,
  inventory_item_id,
  location_id,
  quantity,
  status,
  expires_at
) VALUES (
  'r-001',
  1,
  100,
  1,
  1,
  'CLAIMED',
  DATE_ADD(NOW(), INTERVAL 5 MINUTE)
);
```

Redis 예약 키를 정리한다.

```text
DEL reservation:r-001
```

## Step 4: 상태 기록

MySQL 상태를 확인한다.

```powershell
Get-Content .\labs\sql\01_show_state.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

Redis 상태를 확인한다.

```text
GET available:1:100:1
GET reservation:r-001
```

## 정리 질문

- Redis 가용 수량과 MySQL 원장은 같은 시점에 변경되는가?
- Redis 예약 성공 후 MySQL 원장 차감 전 장애가 발생하면 어떤 상태가 남는가?
- Redis 예약 키가 TTL로 만료되면 MySQL 원장은 자동으로 복구되는가?
