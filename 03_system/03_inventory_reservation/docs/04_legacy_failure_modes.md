# 4장: 레거시 실패 시나리오 분석

## 핵심 질문

레거시 구조의 핵심 문제는 Redis와 MySQL의 트랜잭션 경계가 분리된다는 점이다. 이 장에서는 어떤 순서에서 오버셀 또는 언더셀이 생기는지 정리한다.

## 실패 모드 요약

각 케이스는 독립적으로 발생할 수 있다. 순차 단계로 해석하지 않도록 케이스별 다이어그램을 분리한다.

## Case 1(오버셀 위험): Redis 예약 성공 후 MySQL 원장 차감 실패

![Redis Reserve Success and MySQL Claim Failure](../diagrams/legacy-failure-case1-redis-claim-fail.svg)

Mermaid 원본은 [legacy-failure-case1-redis-claim-fail.mmd](../diagrams/legacy-failure-case1-redis-claim-fail.mmd)에 둔다.

Redis에서는 가용 수량이 줄어든다. 결제 성공 후 MySQL 원장 차감이 실패하면 Redis 기준으로는 예약 또는 판매된 것처럼 보이지만 원장은 그대로 남는다.

관찰할 상태:

- Redis 가용 수량
- Redis 예약 키
- MySQL 원장의 `claimed_quantity`
- 예약 로그의 상태

## Case 2(언더셀 위험): MySQL 원장 차감 성공 후 Redis 정리 실패

![MySQL Claim Success and Redis Cleanup Failure](../diagrams/legacy-failure-case2-redis-cleanup-fail.svg)

Mermaid 원본은 [legacy-failure-case2-redis-cleanup-fail.mmd](../diagrams/legacy-failure-case2-redis-cleanup-fail.mmd)에 둔다.

MySQL 원장은 차감된다. 이후 Redis 예약 정리에 실패하면 Redis에는 이미 처리된 예약이 남는다. 만료 처리와 충돌하면 가용 수량이 잘못 복구될 수 있다.

관찰할 상태:

- MySQL 원장의 차감 여부
- Redis 예약 키의 잔존 여부
- Redis 가용 수량 복구 여부

## Case 3(Claim/Release 경합): 예약 만료와 결제 성공의 경합

![Claim and Release Race](../diagrams/legacy-failure-case3-claim-release-race.svg)

Mermaid 원본은 [legacy-failure-case3-claim-release-race.mmd](../diagrams/legacy-failure-case3-claim-release-race.mmd)에 둔다.

예약 만료 처리가 `Release`를 수행하는 동시에 결제 성공 처리가 `Claim`을 수행할 수 있다. 같은 예약에 대해 두 흐름이 동시에 실행되면 최종 상태가 흔들릴 수 있다.

관찰할 상태:

- 같은 reservation id에 대해 `Claim`과 `Release` 중 어떤 처리가 먼저 완료되는지
- Redis 가용 수량이 중복 복구되는지
- MySQL 원장이 중복 차감되는지

## Case 4(멱등성 누락 위험): 중복 요청 또는 재시도 요청

네트워크 지연이나 클라이언트 재시도로 같은 예약에 대한 요청이 반복될 수 있다. 레거시 구조에서는 같은 요청을 한 번만 처리했는지 별도로 확인해야 한다.

## 실습 연결

- `labs/02_legacy_failure_cases.md`에서 중간 실패를 직접 주입한다.
- 실패 후 Redis 상태와 MySQL 상태를 표로 기록한다.
