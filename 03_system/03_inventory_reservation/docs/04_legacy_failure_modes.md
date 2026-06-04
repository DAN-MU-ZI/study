# 4장: 레거시 실패 시나리오 분석

## 핵심 질문

레거시 구조의 핵심 문제는 Redis와 MySQL의 트랜잭션 경계가 분리된다는 점이다. 이 장에서는 어떤 순서에서 오버셀 또는 언더셀이 생기는지 정리한다.

## 실패 모드 요약

각 케이스는 독립적으로 발생할 수 있다. 순차 단계로 해석하지 않도록 케이스별 다이어그램을 분리한다.

## Case 1(오버셀 위험): 결제 성공 후 MySQL 원장 차감 실패

![Redis Reserve Success and MySQL Claim Failure](../diagrams/legacy-failure-case1-redis-claim-fail.svg)

Mermaid 원본은 [legacy-failure-case1-redis-claim-fail.mmd](../diagrams/legacy-failure-case1-redis-claim-fail.mmd)에 둔다.

Redis에서는 가용 수량이 줄어들고 결제 또는 주문은 성공한 상태다. 이때 MySQL 원장 차감이 실패하면 이미 팔린 재고가 원장에 반영되지 않는다. 이후 Redis 예약이 만료되거나 정리되면 같은 재고를 다시 판매할 수 있으므로 Shopify 기준으로는 오버셀 위험으로 본다.

결제 전 Redis 예약만 남아 있는 순간 상태는 일시적으로 언더셀처럼 보일 수 있다. 이 케이스에서는 결제 성공 여부와 원장 차감 실패 여부를 함께 기록한다.

관찰할 상태:

- Redis 가용 수량
- Redis 예약 키
- MySQL 원장의 `claimed_quantity`
- 예약 로그의 상태

## Case 2(언더셀 위험): MySQL 원장 차감 성공 후 Redis 정리 실패

![MySQL Claim Success and Redis Cleanup Failure](../diagrams/legacy-failure-case2-redis-cleanup-fail.svg)

Mermaid 원본은 [legacy-failure-case2-redis-cleanup-fail.mmd](../diagrams/legacy-failure-case2-redis-cleanup-fail.mmd)에 둔다.

MySQL 원장은 차감된다. 이후 Redis 예약 정리에 실패하면 Redis에는 이미 처리된 예약이 남는다. Redis 기준 가용 수량은 줄어든 상태로 유지되므로 실제 판매 가능 수량보다 시스템이 더 적게 보게 된다. Shopify 기준으로는 차감된 재고가 여전히 예약 상태로 남은 것이므로 언더셀 위험으로 본다.

단, 만료 처리에서 이미 Claim된 예약을 다시 `Release`하면 실제 판매된 재고가 Redis 가용 수량으로 복구된다. 이 경우에는 오버셀 위험으로 전이된다.

관찰할 상태:

- MySQL 원장의 차감 여부
- Redis 예약 키의 잔존 여부
- Redis 가용 수량 복구 여부

## Case 3(Claim/Release 경합): 예약 만료와 결제 성공의 경합

![Claim and Release Race](../diagrams/legacy-failure-case3-claim-release-race.svg)

Mermaid 원본은 [legacy-failure-case3-claim-release-race.mmd](../diagrams/legacy-failure-case3-claim-release-race.mmd)에 둔다.

예약 만료 처리가 `Release`를 수행하는 동시에 결제 성공 처리가 `Claim`을 수행할 수 있다. 같은 예약에 대해 두 흐름이 동시에 실행되면 최종 상태가 흔들릴 수 있다. 경합 자체는 오버셀 또는 언더셀로 바로 분류하지 않고 최종 상태로 판단한다.

관찰할 상태:

- 같은 reservation id에 대해 `Claim`과 `Release` 중 어떤 처리가 먼저 완료되는지
- Redis 가용 수량이 중복 복구되는지
- MySQL 원장이 중복 차감되는지

## Case 4(멱등성 누락 위험): 중복 요청 또는 재시도 요청

네트워크 지연이나 클라이언트 재시도로 같은 예약에 대한 요청이 반복될 수 있다. 레거시 구조에서는 같은 요청을 한 번만 처리했는지 별도로 확인해야 한다.

## 실습 연결

- `labs/02_legacy_failure_cases.md`에서 중간 실패를 직접 주입한다.
- 실패 후 Redis 상태와 MySQL 상태를 표로 기록한다.
