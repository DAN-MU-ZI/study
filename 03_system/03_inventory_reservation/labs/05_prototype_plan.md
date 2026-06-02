# 실습 5: 작은 프로토타입 구현 계획

## 목표

문서와 SQL 실습으로 확인한 내용을 작은 API 또는 스크립트로 구현하기 전, 구현 범위를 정한다.

## 구현 단위

초기 프로토타입은 두 구현을 분리한다.

- 레거시 구현: Redis 기반 예약
- 개선 구현: MySQL `SKIP LOCKED` 기반 예약

두 구현은 같은 입력 조건을 받는다.

- `shop_id`
- `inventory_item_id`
- `location_id`
- `reservation_id`
- `quantity`

초기에는 `quantity = 1`만 지원한다.

## 레거시 구현 흐름

1. Redis에서 가용 수량을 확인한다.
2. 가용 수량이 있으면 차감한다.
3. Redis에 예약 키와 TTL을 저장한다.
4. 결제 성공 시 MySQL 원장을 차감한다.
5. 결제 실패 또는 만료 시 Redis 수량을 복구한다.

확인할 실패:

- Redis 예약 성공 후 MySQL 차감 실패
- MySQL 차감 성공 후 Redis 정리 실패
- 만료 처리와 결제 성공 처리 경합

## 개선 구현 흐름

1. MySQL 트랜잭션을 시작한다.
2. `reservation_units`에서 예약 가능한 row를 찾는다.
3. `FOR UPDATE SKIP LOCKED`로 잠긴 row를 건너뛴다.
4. 확보한 row를 `reserved_quantities`로 옮긴다.
5. 결제 성공 시 원장을 차감한다.
6. pool이 줄어들면 보충 시스템을 실행한다.

확인할 실패:

- 예약 가능한 row 부족
- 동시 요청에서 같은 `unit_id` 중복 예약 여부
- 보충 작업 중복 실행 여부

## 비교 실험

동일한 초기 조건에서 두 구현을 따로 실행한다.

- 초기 재고: 100개
- 동시 요청: 500개
- 요청당 수량: 1개
- 예약 TTL: 5분

기록할 지표:

- 성공 예약 수
- 실패 예약 수
- 최종 Redis 가용 수량
- 최종 MySQL 원장 수량
- `reservation_units` 잔여 수
- `reserved_quantities` 수
- 상태 불일치 수

## 구현 전 확인

- Docker 환경에서 MySQL과 Redis가 실행되는가?
- SQL 실습으로 `SKIP LOCKED` 동작을 확인했는가?
- 보충 시스템에서 중복 `unit_id`가 생기지 않는 조건을 확인했는가?
- 레거시 실패 시나리오를 표로 기록했는가?
