# 3장: 레거시 Redis 구조 분석

## 핵심 질문

Redis 기반 예약 구조는 빠른 예약 판단에 유리하다. 이 장에서는 Redis와 MySQL이 각각 어떤 책임을 가지는지 정리한다.

## 구조 개요

레거시 구조는 Redis와 MySQL을 함께 사용한다.

- Redis: 예약 가능한 수량과 임시 예약 상태 저장
- MySQL: 실제 재고 원장과 최종 차감 결과 저장

## 레거시 예약 흐름

![Legacy Redis Reservation Flow](../diagrams/legacy-redis-flow.svg)

Mermaid 원본은 [legacy-redis-flow.mmd](../diagrams/legacy-redis-flow.mmd)에 둔다.

## Reserve 흐름

예약 요청이 들어오면 Redis의 가용 수량을 확인하고 차감한다. 예약 정보에는 만료 시간이 붙는다. 이 단계에서는 MySQL 원장이 아직 차감되지 않는다.

## Claim 흐름

결제 성공 후 MySQL 원장을 차감한다. 이후 Redis에 남아 있는 예약 상태를 정리한다.

## Release 흐름

결제 실패 또는 예약 만료가 발생하면 Redis의 가용 수량을 복구한다. 예약 상태는 삭제되거나 만료 처리된다.

## 장점

- Redis 명령은 빠르게 실행된다.
- 단순 수량 차감은 원자적으로 처리할 수 있다.
- 높은 요청량을 받아내기 쉽다.

## 한계

- Redis 예약 상태와 MySQL 원장이 같은 트랜잭션 안에서 변경되지 않는다.
- 중간 단계 실패가 발생하면 두 저장소의 상태가 어긋난다.
- 복구 시점에는 어떤 상태가 기준인지 판단해야 한다.

## 실습 연결

- `labs/01_legacy_redis_reservation.md`에서 Redis 예약 흐름을 직접 확인한다.
- `labs/02_legacy_failure_cases.md`에서 Redis와 MySQL 원장의 불일치 상황을 만든다.
