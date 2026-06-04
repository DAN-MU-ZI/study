# 재고 예약 시스템 스터디

[Shopify 원본 링크](https://shopify.engineering/scaling-inventory-reservations)

Shopify Engineering의 Scaling Inventory Reservations 글을 바탕으로 재고 예약 시스템을 레거시 구조와 개선 구조로 나누어 재현한다.

## 주제

- 레거시: Redis 기반 재고 예약과 MySQL 재고 원장을 함께 사용할 때 생기는 정합성 문제
- 개선: MySQL의 `SKIP LOCKED`와 재고 단위별 로우 설계(`one row per unit`)를 사용한 예약 구조

## 목표

- Redis 기반 예약 구조와 RDBMS를 동시에 사용할 때의 트랜잭션 경계 문제를 이해한다.
- 예약(`Reserve`), 확정(`Claim`), 해제(`Release`)의 책임과 상태 변화를 구분한다.
- Redis 예약 상태와 MySQL 원장 사이에서 오버셀 또는 언더셀이 발생할 수 있는 조건을 정리한다.
- MySQL `SELECT ... FOR UPDATE SKIP LOCKED`가 동시 예약 요청을 어떻게 처리하는지 확인한다.
- 재고 단위별 로우 설계가 락 경합과 예약 처리에 주는 영향을 정리한다.
- 예약 가능한 재고 풀을 보충하는 시스템이 왜 필요한지와 어떤 경합을 제어해야 하는지 정리한다.

## 스터디 범위

이 스터디는 Shopify의 재고 예약 시스템 전환 사례를 바탕으로, 기존 Redis 기반 구조의 한계와 MySQL 기반 개선 구조의 동작 원리를 이해하는 것을 목표로 한다.

### 포함 범위

- 재고 예약 시스템에서 `Reserve`, `Claim`, `Release`가 각각 어떤 역할을 하는지 정리한다.
- Redis 기반 예약 구조가 어떤 방식으로 빠른 예약 처리를 제공하는지 확인한다.
- Redis 예약 상태와 MySQL 재고 원장을 함께 사용할 때 트랜잭션 경계가 어떻게 나뉘는지 분석한다.
- 오버셀, 언더셀, 예약 만료와 결제 성공 경합 같은 실패 시나리오를 정리한다.
- MySQL `SELECT ... FOR UPDATE SKIP LOCKED` 기반 예약 구조를 분석한다.
- `one row per unit` 설계가 재고 예약 문제를 어떻게 모델링하는지 정리한다.
- 예약 가능한 재고 풀을 보충하는 `replenishment` 시스템을 정리한다.
- 최종적으로 레거시 구조와 개선 구조를 비교해 어떤 문제가 어떤 설계로 해결되는지 정리한다.

### 제외 범위

- Shopify의 실제 운영 규모를 그대로 재현하지 않는다.
- 다중 리전, 샤딩, 전체 주문 시스템, 결제 시스템 전체 구현은 다루지 않는다.
- 처음부터 완성된 애플리케이션을 만들지 않는다.
- 초기 구현은 상품 1개, 재고 위치 1개, 요청당 수량 1개 예약으로 제한한다.
- 성능 최적화는 기본 동작과 실패 시나리오를 확인한 뒤 진행한다.

## 학습 자료

Docker 기반 실습 환경과 챕터별 문서를 함께 구성한다.

- [Docker 환경 준비](./labs/00_environment.md)
- [레거시 Redis 재고 예약](./00_legacy/README.md)
- [개선 MySQL 재고 예약](./01_advanced/README.md)
- [문제 정의](./docs/01_problem_definition.md)
- [도메인 모델 정리](./docs/02_domain_model.md)
- [레거시 Redis 구조 분석](./docs/03_legacy_redis_architecture.md)
- [레거시 실패 시나리오 분석](./docs/04_legacy_failure_modes.md)
- [개선 구조 요구사항 정리](./docs/05_improvement_requirements.md)
- [개선 MySQL 구조 분석](./docs/06_mysql_skip_locked_architecture.md)
- [보충 시스템 분석](./docs/07_replenishment_system.md)
- [실험 계획 수립](./docs/08_experiment_plan.md)
- [Inventory Reservation ERD](./diagrams/erd.md)

다이어그램 산출물:

- [예약 상태 전이](./diagrams/reservation-state.svg)
- [오버셀·언더셀 판정 기준](./diagrams/oversell-undersell-classification.svg)
- [레거시 Redis 예약 흐름](./diagrams/legacy-redis-flow.svg)
- [레거시 실패 Case 1(오버셀 위험)](./diagrams/legacy-failure-case1-redis-claim-fail.svg)
- [레거시 실패 Case 2(언더셀 위험)](./diagrams/legacy-failure-case2-redis-cleanup-fail.svg)
- [레거시 실패 Case 3](./diagrams/legacy-failure-case3-claim-release-race.svg)
- [MySQL Lock Wait vs SKIP LOCKED](./diagrams/mysql-lock-wait-vs-skip-locked.svg)
- [보충 시스템 흐름](./diagrams/replenishment-flow.svg)
- [Inventory Reservation ERD](./diagrams/erd.svg)

다이어그램 SVG는 `beautiful-mermaid`로 생성한다.

```powershell
npm install
npm run render:diagrams
```

## 학습 방식

이 스터디는 레거시 구조를 먼저 이해한 뒤 개선 구조를 다룬다. 먼저 Redis 기반 예약 흐름을 정리하고, 그 구조에서 정합성 문제가 생기는 지점을 재현한 다음, MySQL 기반 개선 구조가 어떤 문제를 해결하는지 확인한다.

### 1. 문서 기반 구조 분석

먼저 Shopify 글에서 설명하는 구조를 도메인 동작 단위로 나눈다.

- `Reserve`, `Claim`, `Release`의 책임을 분리한다.
- Redis 기반 구조에서 Redis와 MySQL이 각각 어떤 상태를 가지는지 정리한다.
- MySQL 기반 구조에서 `reservation_units`와 `reserved_quantities`가 어떤 역할을 하는지 정리한다.
- 보충 시스템이 `inventory_ledger`와 `reservation_units` 사이에서 어떤 역할을 하는지 정리한다.
- 레거시 구조와 개선 구조의 차이를 표로 비교한다.

### 2. Redis 명령 기반 레거시 구조 재현

Redis 기반 예약 구조는 Redis CLI 명령으로 먼저 확인한다.

- Redis에 재고별 가용 수량을 저장한다.
- 예약 요청이 들어오면 Redis에서 가용 수량을 차감하는 방식으로 처리한다.
- 예약마다 만료 시간을 둔다.
- 결제 실패 또는 예약 만료 시 Redis의 가용 수량을 복구한다.
- 결제 성공 시 MySQL 원장을 차감하고 Redis 예약 상태를 정리한다.
- Redis 수량과 MySQL 원장 수량이 달라지는 상황을 의도적으로 만든다.
- 실험 결과를 통해 Redis 기반 구조에서 트랜잭션 경계가 어디에서 끊기는지 정리한다.

### 3. 레거시 실패 시나리오 주입

레거시 구조의 문제는 정상 흐름보다 실패 흐름에서 더 잘 드러난다. 따라서 중간 단계 실패를 의도적으로 만든다.

- 결제 성공 후 MySQL 원장 차감 실패(오버셀 위험, 결제 전 예약 잔존 구간은 언더셀처럼 보일 수 있음)
- MySQL 원장 차감 성공 후 Redis 예약 정리 실패(언더셀 위험, 잘못된 TTL Release 후 오버셀 전이 가능)
- 예약 만료 처리와 결제 성공 처리의 동시 실행
- 같은 reservation id에 대한 중복 `Claim`
- 같은 reservation id에 대한 `Claim`과 `Release` 경합

각 케이스는 다음 항목으로 기록한다.

- 초기 상태
- 실행 순서
- Redis 최종 상태
- MySQL 최종 상태
- 오버셀 또는 언더셀 발생 여부
- 복구 가능 여부

### 4. SQL 기반 개선 구조 경합 재현

MySQL 개선 구조의 핵심 경합은 SQL만으로 재현할 수 있다. MySQL 세션 2개를 열고 같은 재고 row를 동시에 예약하는 방식으로 확인한다.

실험은 다음 방식으로 진행한다.

- 예약 가능한 재고 row를 여러 개 준비한다.
- 첫 번째 세션에서 특정 재고 row를 잠근 뒤 트랜잭션을 유지한다.
- 두 번째 세션에서 같은 조건으로 예약 가능한 row를 조회한다.
- `FOR UPDATE`만 사용했을 때 두 번째 세션이 대기하는지 확인한다.
- `FOR UPDATE SKIP LOCKED`를 사용했을 때 잠긴 row를 건너뛰고 다른 row를 가져오는지 확인한다.
- 예약 가능한 row 수보다 많은 트랜잭션을 실행했을 때 일부 요청이 실패하는지 확인한다.
- `READ COMMITTED`와 `REPEATABLE READ`에서 조회 결과와 락 대기 양상이 어떻게 달라지는지 비교한다.
- 실험 결과를 통해 `SKIP LOCKED`가 재고 예약 경합을 어떻게 완화하는지 정리한다.

### 5. SQL 기반 보충 시스템 재현

보충 시스템은 예약 가능한 row pool을 유지하기 위한 풀링 처리로 본다. 기본 예약 경합과 레거시 실패 시나리오를 먼저 확인한 뒤, 보충 시스템을 별도 단계로 다룬다.

실험은 다음 방식으로 진행한다.

- `inventory_ledger`에 실제 재고 수량을 둔다.
- `reservation_units`에는 예약 가능한 일부 수량만 row로 만들어 둔다.
- 예약 요청이 반복되어 `reservation_units`가 줄어드는 상황을 만든다.
- pool 크기가 임계값 아래로 내려가면 원장 기준으로 예약 가능한 row를 보충하는 흐름을 확인한다.
- 같은 상품과 위치에 대해 보충 작업이 동시에 실행될 때 중복 row가 생기지 않는지 확인한다.
- 보충 작업과 예약 작업이 동시에 실행될 때 예약 가능 수량이 원장 수량을 초과하지 않는지 확인한다.
- 실험 결과를 통해 보충 시스템이 예약 처리량과 데이터 정합성에 어떤 영향을 주는지 정리한다.

### 6. 작은 프로토타입 구현

문서와 SQL 실험으로 구조를 확인한 뒤 작은 구현을 진행한다.

- Redis 기반 레거시 예약 API
- MySQL `SKIP LOCKED` 기반 개선 예약 API
- 예약 가능한 row pool 보충 기능
- 동일한 조건에서 두 구조를 따로 실행하는 비교 테스트
- 성공 예약 수, 실패 예약 수, 상태 불일치 수 측정

초기 구현 조건은 다음과 같다.

- 상품 1개
- 재고 위치 1개
- 초기 재고 100개
- 요청당 예약 수량 1개
- 동시 요청 500개

## 챕터 구성

### 1장: 문제 정의

재고 예약 시스템이 왜 필요한지 정리한다.

- 사용자가 결제 전에 재고를 임시로 확보해야 하는 이유
- 실제 재고 차감과 임시 예약을 분리해야 하는 이유
- 동시 구매 상황에서 오버셀이 발생하는 조건
- 예약을 오래 잡아둘 때 언더셀이 발생하는 조건

### 2장: 도메인 모델 정리

공통 용어와 상태 전이를 먼저 고정한다.

- `Reserve`: 결제 전에 재고를 임시 확보하는 동작
- `Claim`: 결제 성공 후 재고를 확정 차감하는 동작
- `Release`: 결제 실패 또는 만료 시 예약을 해제하는 동작
- `Inventory Ledger`: 실제 재고 원장
- `Available Quantity`: 예약 가능한 수량
- `Reservation`: 결제 대기 중인 임시 재고 점유 상태

### 3장: 레거시 Redis 구조 분석

기존 Redis 기반 예약 구조를 먼저 이해한다.

- Redis counter 기반 예약 흐름
- 예약 TTL 처리
- 결제 성공 후 MySQL 원장 차감 흐름
- 결제 실패 또는 만료 시 Redis 수량 복구 흐름
- Redis와 MySQL이 각각 어떤 책임을 가지는지 정리

### 4장: 레거시 실패 시나리오 분석

Redis 구조에서 정합성 문제가 생기는 지점을 구체적으로 정리한다.

- 결제 성공 후 MySQL 원장 차감 실패(오버셀 위험, 결제 전 예약 잔존 구간은 언더셀처럼 보일 수 있음)
- MySQL 원장 차감 성공 후 Redis 예약 정리 실패(언더셀 위험, 잘못된 TTL Release 후 오버셀 전이 가능)
- 예약 만료 처리와 결제 성공 처리의 경합
- 중복 요청 또는 재시도 요청으로 인한 상태 불일치
- 장애 복구 시 Redis 상태와 MySQL 상태를 맞추기 어려운 상황

### 5장: 개선 구조 요구사항 정리

레거시 실패 시나리오를 바탕으로 개선 구조가 만족해야 할 조건을 정리한다.

- 예약 판단과 예약 상태 변경을 같은 트랜잭션 안에서 처리할 수 있어야 한다.
- 동시 요청이 같은 재고를 중복으로 잡지 않아야 한다.
- 잠긴 재고 때문에 전체 예약 처리가 불필요하게 대기하지 않아야 한다.
- 예약 가능한 재고 단위를 명확히 추적할 수 있어야 한다.
- 예약 가능한 row pool이 부족해지는 상황을 처리할 수 있어야 한다.

### 6장: 개선 MySQL 구조 분석

MySQL 기반 예약 구조가 어떤 방식으로 문제를 풀려고 하는지 정리한다.

- 예약 가능한 재고를 row 단위로 표현하는 방식
- `SELECT ... FOR UPDATE SKIP LOCKED`의 동작
- 동시에 여러 요청이 들어왔을 때 잠긴 row를 건너뛰는 방식
- `READ COMMITTED` 격리 수준을 사용하는 이유
- 복합 primary key와 인덱스 설계가 필요한 이유

### 7장: 보충 시스템 분석

예약 가능한 재고 풀을 어떻게 유지하고 보충하는지 정리한다.

- 전체 재고 수량만큼 row를 만들지 않는 이유
- 예약 가능한 row pool의 크기를 제한하는 이유
- pool이 부족할 때 보충하는 방식
- 같은 상품과 위치에 대해 중복 보충을 막는 방식
- 보충 작업과 예약 작업이 동시에 실행될 때 필요한 락 범위
- 보충된 row 수량이 원장 기준 가용 수량을 초과하지 않도록 검증하는 방식

### 8장: 실험 계획 수립

문서화 이후 구현 전에 검증할 실험을 정의한다.

- Redis 기반 예약 구조에서 동시 예약 요청을 처리한다.
- MySQL `SKIP LOCKED` 기반 예약 구조에서 같은 조건을 처리한다.
- MySQL 보충 시스템이 필요한 상황을 만들고 pool 보충 결과를 확인한다.
- 두 구조에서 성공 예약 수, 실패 예약 수, 지연 시간, 상태 불일치 여부를 비교한다.
- 초기 조건은 재고 100개, 동시 요청 500개, 요청당 예약 수량 1개로 둔다.
- 실험 결과를 바탕으로 레거시 구조와 개선 구조의 차이를 정리한다.
