## Step 1: Redis에 가용 수량 저장

이 단계에서는 Redis를 재고별 가용 수량 counter로 사용하는 방식을 확인한다. 목적은 예약 판단이 MySQL 원장 변경 전에 Redis 값만 보고 시작되는 구조를 보는 것이다.

```redis
SET available:1:100:1 100
OK
GET available:1:100:1
"100"
```

확인할 점:

- Redis가 재고별 가용 수량을 단일 counter처럼 다룬다.  
-> available:1:100:1 하나의 키에 값 100을 저장하여 재고/위치 조합별로 키를 만들고 counter처럼 증감한다.  
- 아직 MySQL 원장은 변경되지 않는다.  
-> 이 단계에서는 Redis의 available counter만 설정되고, MySQL 원장은 변경되지 않는다.  
-> MySQL 원장의 확정 차감 수량(claimed_quantity)은 0으로 남는다.


## Step 2: 임시 예약(Reserve) 수행

이 단계에서는 임시 예약이 Redis 가용 수량 차감과 TTL 예약 키 생성으로 표현되는지 확인한다. 목적은 예약 요청이 성공한 직후에도 MySQL 원장은 아직 확정 차감되지 않는 구간을 보는 것이다.

```redis
127.0.0.1:6379> DECRBY available:1:100:1 1
(integer) 99
127.0.0.1:6379> SET reservation:r-001 1 EX 300
OK
127.0.0.1:6379> GET available:1:100:1
"99"
127.0.0.1:6379> GET reservation:r-001
"1"
127.0.0.1:6379> TTL reservation:r-001
(integer) 272
```

확인할 점:

- Redis 가용 수량은 99가 된다.  
-> 임시 예약 과정에서 `DECRBY`가 수행되어 Redis 가용 수량이 1 차감된다.  
- 예약 키에는 TTL이 붙는다.  
-> 정책으로 정한 시간만큼 TTL이 적용되며, `TTL reservation:r-001` 조회 시 남은 시간이 감소하는 것을 확인할 수 있다.  
- MySQL 원장의 `claimed_quantity`는 아직 0이다.  
-> 임시 예약 단계에서는 원장 확정 차감이 수행되지 않으므로 `claimed_quantity`는 0으로 남는다.  

## Step 3: 확정 반영(Claim) 수행

이 단계에서는 결제 성공 이후 MySQL 원장에 확정 차감을 반영하는 흐름을 확인한다. 목적은 Redis 임시 예약과 MySQL 원장 반영이 서로 다른 시점에 실행된다는 점을 보는 것이다.

```mysql
mysql> UPDATE inventory_ledger
    -> SET claimed_quantity = claimed_quantity + 1
    -> WHERE shop_id = 1
    ->   AND inventory_item_id = 100
    ->   AND location_id = 1;
Query OK, 1 row affected (0.70 sec)
Rows matched: 1  Changed: 1  Warnings: 0

mysql> INSERT INTO legacy_reservation_log (
    ->   reservation_id,
    ->   shop_id,
    ->   inventory_item_id,
    ->   location_id,
    ->   quantity,
    ->   status,
    ->   expires_at
    -> ) VALUES (
    ->   'r-001',
    ->   1,
    ->   100,
    ->   1,
    ->   1,
    ->   'CLAIMED',
    ->   DATE_ADD(NOW(), INTERVAL 5 MINUTE)
    -> );
Query OK, 1 row affected (0.83 sec)
```

```redis
127.0.0.1:6379> DEL reservation:r-001
(integer) 1
127.0.0.1:6379> GET available:1:100:1
"99"
127.0.0.1:6379> GET reservation:r-001
(nil)
```

## Step 4: 상태 기록

이 단계에서는 Redis와 MySQL의 최종 상태를 나란히 확인한다. 목적은 Redis 예약 정보, Redis 가용 수량, MySQL 원장 수량이 같은 의미로 맞춰져 있는지 판단하는 것이다.

```mysql
mysql> USE inventory_study;
Database changed
mysql>
mysql> SELECT
    ->   shop_id,
    ->   inventory_item_id,
    ->   location_id,
    ->   total_quantity,
    ->   claimed_quantity,
    ->   total_quantity - claimed_quantity AS ledger_available
    -> FROM inventory_ledger;
+---------+-------------------+-------------+----------------+------------------+------------------+
| shop_id | inventory_item_id | location_id | total_quantity | claimed_quantity | ledger_available |
+---------+-------------------+-------------+----------------+------------------+------------------+
|       1 |               100 |           1 |            100 |                1 |               99 |
+---------+-------------------+-------------+----------------+------------------+------------------+
1 row in set (0.00 sec)

mysql> SELECT
    ->   shop_id,
    ->   inventory_item_id,
    ->   location_id,
    ->   COUNT(*) AS reservation_unit_count,
    ->   MIN(unit_id) AS min_unit_id,
    ->   MAX(unit_id) AS max_unit_id
    -> FROM reservation_units
    -> GROUP BY shop_id, inventory_item_id, location_id;
+---------+-------------------+-------------+------------------------+-------------+-------------+
| shop_id | inventory_item_id | location_id | reservation_unit_count | min_unit_id | max_unit_id |
+---------+-------------------+-------------+------------------------+-------------+-------------+
|       1 |               100 |           1 |                     10 |           1 |          10 |
+---------+-------------------+-------------+------------------------+-------------+-------------+
1 row in set (0.00 sec)

mysql> SELECT
    ->   status,
    ->   COUNT(*) AS reserved_count
    -> FROM reserved_quantities
    -> GROUP BY status;
Empty set (0.32 sec)

mysql> SELECT
    ->   shop_id,
    ->   inventory_item_id,
    ->   location_id,
    ->   next_unit_id,
    ->   target_pool_size,
    ->   low_watermark
    -> FROM replenishment_state;
+---------+-------------------+-------------+--------------+------------------+---------------+
| shop_id | inventory_item_id | location_id | next_unit_id | target_pool_size | low_watermark |
+---------+-------------------+-------------+--------------+------------------+---------------+
|       1 |               100 |           1 |           11 |               10 |             3 |
+---------+-------------------+-------------+--------------+------------------+---------------+
1 row in set (0.00 sec)
```


## 정리 질문

- Redis 가용 수량과 MySQL 원장은 같은 시점에 변경되는가?  
-> 각기 다른 시점에 발생한다.  
-> 우선적으로 Redis에서 재고 차감이 처리되고, 결제 성공 후 MySQL의 실제 원장에 반영된다.  
- Redis 예약 성공 후 MySQL 원장 차감 전 장애가 발생하면 어떤 상태가 남는가?  
-> Redis의 available counter는 99로 차감된 상태로 남는다.  
-> reservation TTL 키는 만료될 수 있지만, available counter는 자동으로 복구되지 않는다.  
- Redis 예약 키가 TTL로 만료되면 MySQL 원장은 자동으로 복구되는가?  
-> 복구되지 않는다.  
-> 그러므로 별도의 복구 프로세스가 필요하다.
