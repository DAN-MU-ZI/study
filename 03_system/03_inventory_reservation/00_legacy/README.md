# 레거시 Redis 재고 예약

## 목적

Redis 기반 재고 예약 구조를 먼저 이해한다. Redis가 빠른 예약 판단을 맡고 MySQL이 실제 원장을 맡을 때, 두 저장소 사이의 트랜잭션 경계가 어디에서 끊기는지 확인한다.

## 학습 순서

1. [문제 정의](../docs/01_problem_definition.md)
2. [도메인 모델 정리](../docs/02_domain_model.md)
3. [레거시 Redis 구조 분석](../docs/03_legacy_redis_architecture.md)
4. [레거시 실패 시나리오 분석](../docs/04_legacy_failure_modes.md)

## 실습 순서

1. [Docker 환경 준비](../labs/00_environment.md)
2. [Redis 기반 레거시 예약 흐름](../labs/01_legacy_redis_reservation.md)
3. [레거시 실패 시나리오](../labs/02_legacy_failure_cases.md)

## 확인할 결과

- Redis 가용 수량이 예약 시점에 줄어드는지 확인한다.
- MySQL 원장 차감은 결제 성공 이후에 수행되는지 확인한다.
- Redis와 MySQL 사이에서 상태 불일치가 생기는 순서를 기록한다.
- 오버셀과 언더셀 가능성이 생기는 조건을 구분한다.
