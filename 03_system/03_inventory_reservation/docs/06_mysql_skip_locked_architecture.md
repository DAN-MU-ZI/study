# 6장: 개선 MySQL 구조 분석

## 핵심 질문

MySQL 기반 개선 구조는 예약 가능한 재고를 row로 표현하고, row lock을 사용해 동시 예약 경합을 제어한다.

## 주요 테이블

| 테이블 | 역할 |
|---|---|
| `inventory_ledger` | 실제 재고 원장 |
| `reservation_units` | 예약 가능한 재고 1개 단위의 row pool |
| `reserved_quantities` | 예약되어 결제 대기 중인 재고 단위 |
| `replenishment_state` | 보충 시스템이 다음에 만들 unit id와 pool 기준을 관리 |

## ERD

![Inventory Reservation ERD](../diagrams/erd.svg)

Mermaid 원본은 [erd.mmd](../diagrams/erd.mmd)에 둔다.

## Reserve 흐름

예약 요청은 `reservation_units`에서 필요한 수량만큼 row를 가져온다. 이때 row lock을 잡는다. 이미 다른 트랜잭션이 잠근 row는 `SKIP LOCKED`로 건너뛴다.

![MySQL Lock Wait vs SKIP LOCKED](../diagrams/mysql-lock-wait-vs-skip-locked.svg)

Mermaid 원본은 [mysql-lock-wait-vs-skip-locked.mmd](../diagrams/mysql-lock-wait-vs-skip-locked.mmd)에 둔다.

확보한 row는 `reserved_quantities`에 기록하고, `reservation_units`에서는 제거한다. 이 작업은 하나의 트랜잭션 안에서 처리한다.

## Claim 흐름

결제 성공 후 `reserved_quantities`를 기준으로 `inventory_ledger`를 차감한다. 이후 해당 예약 상태를 확정 또는 삭제한다.

## Release 흐름

결제 실패 또는 만료가 발생하면 `reserved_quantities`의 예약을 해제한다. 해제된 수량은 보충 시스템을 통해 다시 예약 가능한 pool에 반영한다.

## 격리 수준

실습 환경은 `READ COMMITTED`를 기본으로 둔다. 예약 요청마다 현재 커밋된 예약 가능 row를 기준으로 판단하는 흐름을 확인하기 위해서다.

## 실습 연결

- `labs/03_mysql_locking_skip_locked.md`에서 두 개의 MySQL 세션으로 경합을 재현한다.
