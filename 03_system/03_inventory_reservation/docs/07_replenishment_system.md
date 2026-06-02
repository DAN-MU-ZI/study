# 7장: 보충 시스템 분석

## 핵심 질문

예약 가능한 row pool은 계속 줄어든다. 보충 시스템은 원장 재고를 기준으로 pool을 일정 수준으로 유지한다.

## 보충 시스템이 필요한 이유

재고 1개를 row 1개로 표현하면 예약 경합을 명확히 제어할 수 있다. 다만 전체 재고 수량만큼 row를 항상 만들어 두면 테이블 크기와 관리 비용이 커진다.

따라서 일정 크기의 예약 가능한 row pool을 유지한다. pool이 줄어들면 원장 재고와 예약 상태를 기준으로 새 row를 보충한다.

## 보충 흐름

![Replenishment Flow](../diagrams/replenishment-flow.svg)

Mermaid 원본은 [replenishment-flow.mmd](../diagrams/replenishment-flow.mmd)에 둔다.

## 확인할 기준

- 현재 `reservation_units`에 남은 row 수
- 이미 예약된 `reserved_quantities` 수
- `inventory_ledger`의 실제 재고와 확정 차감 수량
- 보충 후 pool 크기
- 중복 unit id 생성 여부

## 보충 경합

동일한 상품과 위치에 대해 두 보충 작업이 동시에 실행될 수 있다. 이 경우 같은 unit id를 중복 생성하거나, 원장 기준 가용 수량보다 많은 row를 만들 수 있다.

보충 작업은 상품과 위치 단위로 직렬화되어야 한다. 실습에서는 `replenishment_state` row를 잠그는 방식으로 중복 보충을 막는 흐름을 확인한다.

## 실습 연결

- `labs/04_replenishment_pool.md`에서 pool을 줄인 뒤 보충 작업을 실행한다.
- 두 개의 MySQL 세션에서 동시에 보충을 시도해 중복 생성 여부를 확인한다.
