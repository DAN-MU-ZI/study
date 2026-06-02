# 실습 3: MySQL Locking과 SKIP LOCKED

## 목표

MySQL 세션 2개를 사용해 `FOR UPDATE`와 `FOR UPDATE SKIP LOCKED`의 차이를 확인한다.

![MySQL Lock Wait vs SKIP LOCKED](../diagrams/mysql-lock-wait-vs-skip-locked.svg)

## 준비

MySQL 데이터를 초기화한다.

```powershell
Get-Content .\labs\sql\00_reset_mysql.sql | docker compose exec -T mysql mysql -ustudy -pstudy inventory_study
```

두 개의 터미널을 열고 각각 MySQL에 접속한다.

```powershell
docker compose exec mysql mysql -ustudy -pstudy inventory_study
```

이 문서에서는 두 터미널을 `Session A`, `Session B`로 부른다.

## Part 1: FOR UPDATE 대기 확인

`Session A`에서 트랜잭션을 시작하고 첫 번째 예약 가능 row를 잠근다.

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

SELECT unit_id
FROM reservation_units
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
ORDER BY unit_id
LIMIT 1
FOR UPDATE;
```

`Session A`는 커밋하지 않고 유지한다.

`Session B`에서 같은 조건으로 조회한다.

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

SELECT unit_id
FROM reservation_units
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
ORDER BY unit_id
LIMIT 1
FOR UPDATE;
```

확인할 점:

- `Session B`는 `Session A`가 잡은 row를 기다린다.
- 락 대기 시간이 길어지면 예약 처리 지연이 생긴다.

두 세션을 정리한다.

```sql
ROLLBACK;
```

## Part 2: SKIP LOCKED 확인

다시 `Session A`에서 첫 번째 row를 잠근다.

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

SELECT unit_id
FROM reservation_units
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
ORDER BY unit_id
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

`Session A`는 커밋하지 않고 유지한다.

`Session B`에서 같은 조건으로 조회한다.

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

SELECT unit_id
FROM reservation_units
WHERE shop_id = 1
  AND inventory_item_id = 100
  AND location_id = 1
ORDER BY unit_id
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

확인할 점:

- `Session B`는 `Session A`가 잠근 row를 기다리지 않는다.
- `Session B`는 다음 예약 가능 row를 가져온다.
- 두 세션이 서로 다른 `unit_id`를 확보한다.

두 세션을 정리한다.

```sql
ROLLBACK;
```

## Part 3: 예약 처리 흐름으로 확장

확보한 row를 실제 예약 상태로 옮기는 흐름을 확인한다.

진행 방식:

1. 트랜잭션을 시작한다.
2. `reservation_units`에서 row를 `FOR UPDATE SKIP LOCKED`로 가져온다.
3. 가져온 `unit_id`를 `reserved_quantities`에 기록한다.
4. 같은 `unit_id`를 `reservation_units`에서 삭제한다.
5. 커밋한다.

확인할 점:

- 예약 가능한 row 수가 줄어든다.
- 예약된 row 수가 늘어난다.
- 동시에 실행해도 같은 `unit_id`가 중복 예약되지 않는다.

## 정리 질문

- `FOR UPDATE`와 `FOR UPDATE SKIP LOCKED`의 대기 방식은 어떻게 다른가?
- `SKIP LOCKED`는 예약 성공률과 지연 시간에 어떤 영향을 줄 수 있는가?
- 예약 가능한 row가 부족하면 어떤 결과가 나와야 하는가?
