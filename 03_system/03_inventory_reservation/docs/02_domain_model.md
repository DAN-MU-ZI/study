# 2장: 도메인 모델 정리

## 핵심 질문

재고 예약 시스템의 동작을 `Reserve`, `Claim`, `Release`로 나누면 각 단계의 책임이 명확해진다.

## 주요 용어

| 용어 | 의미 |
|---|---|
| `Inventory Ledger` | 실제 재고 원장 |
| `Available Quantity` | 현재 예약 가능한 수량 |
| `Reservation` | 결제 대기 중인 임시 재고 점유 상태 |
| `Reserve` | 결제 전에 재고를 임시 확보하는 동작 |
| `Claim` | 결제 성공 후 예약을 실제 재고 차감으로 확정하는 동작 |
| `Release` | 결제 실패 또는 만료 시 예약을 해제하는 동작 |
| `Reservation Unit` | 개선 구조에서 예약 가능한 재고 1개를 표현하는 row |
| `Replenishment` | 예약 가능한 row pool을 다시 채우는 보충 처리 |

## 상태 전이

예약은 결제 전 임시 점유 상태에서 시작해, 결제 결과에 따라 확정 또는 해제로 끝난다.

![Reservation State Transition](../diagrams/reservation-state.svg)

Mermaid 원본은 [reservation-state.mmd](../diagrams/reservation-state.mmd)에 둔다.

### 상태 설명

| 상태 | 의미 | 다음 상태 |
|---|---|---|
| `Reserving` | 예약 가능 여부를 확인하는 중 | `Reserved`, `Rejected` |
| `Reserved` | 결제 대기 중인 임시 재고 점유 상태 | `Claimed`, `Released`, `Expired` |
| `Claimed` | 결제 성공 후 실제 재고 차감이 확정된 상태 | 종료 |
| `Released` | 결제 실패 또는 만료로 예약이 해제된 상태 | 종료 |
| `Expired` | 예약 TTL이 지나 만료 처리가 필요한 상태 | `Released` |
| `Rejected` | 예약 가능한 재고가 없어 예약이 거절된 상태 | 종료 |

### 단계별 상태 변화

| 단계 | 동작 | 예약 상태 | Redis 레거시 구조 | MySQL 개선 구조 |
|---|---|---|---|---|
| 1 | 사용자가 결제 화면에서 예약 요청 | `Reserving` | Redis 가용 수량 확인 | `reservation_units` row 조회 |
| 2 | 가용 재고 확보 성공 | `Reserved` | Redis 가용 수량 차감, 예약 TTL 저장 | `reservation_units`에서 `reserved_quantities`로 이동 |
| 3 | 결제 성공 | `Claimed` | MySQL 원장 차감 후 Redis 예약 정리 | MySQL 트랜잭션으로 원장 차감 |
| 4 | 결제 실패 | `Released` | Redis 가용 수량 복구 | 예약 row 해제 후 보충 대상이 됨 |
| 5 | TTL 만료 | `Expired` -> `Released` | 만료 처리에서 Redis 가용 수량 복구 | 만료 예약을 해제하고 pool 보충 대상에 반영 |
| 6 | 가용 재고 부족 | `Rejected` | Redis 차감 실패 또는 보정 필요 | 예약 가능한 row 부족으로 실패 |

### 상태 전이에서 확인할 경합

- `Reserved` 상태에서 `Claimed`와 `Released`가 동시에 시도될 수 있다.
- `Expired` 처리와 결제 성공 처리가 같은 예약을 대상으로 실행될 수 있다.
- 레거시 구조에서는 Redis 상태와 MySQL 원장 상태가 서로 다른 시점에 변경된다.
- 개선 구조에서는 예약 row 이동과 상태 변경을 DB 트랜잭션 안에서 묶는 것을 목표로 한다.

## 레거시 구조에서의 상태 위치

Redis는 빠른 예약 판단을 담당한다. MySQL은 최종 재고 원장을 담당한다. 두 저장소가 같은 트랜잭션 안에서 묶이지 않기 때문에 실패 시나리오가 생긴다.

## 개선 구조에서의 상태 위치

MySQL 기반 구조는 예약 가능한 재고 단위를 row로 표현한다. 예약 판단과 예약 상태 변경을 같은 DB 트랜잭션 안에서 처리할 수 있다.

## 실습 연결

- Redis 실습에서는 `Available Quantity`가 Redis에 있고 `Inventory Ledger`가 MySQL에 있는 구조를 확인한다.
- MySQL 실습에서는 `reservation_units`가 예약 가능한 재고 pool 역할을 하는지 확인한다.
