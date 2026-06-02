# 개선 MySQL 재고 예약

## 목적

MySQL `SKIP LOCKED`와 재고 단위별 row 설계를 사용해 동시 예약 경합을 제어하는 구조를 확인한다. 이후 예약 가능한 row pool을 유지하는 보충 시스템을 다룬다.

## 학습 순서

1. [개선 구조 요구사항 정리](../docs/05_improvement_requirements.md)
2. [개선 MySQL 구조 분석](../docs/06_mysql_skip_locked_architecture.md)
3. [보충 시스템 분석](../docs/07_replenishment_system.md)
4. [실험 계획 수립](../docs/08_experiment_plan.md)

## 실습 순서

1. [Docker 환경 준비](../labs/00_environment.md)
2. [MySQL Locking과 SKIP LOCKED](../labs/03_mysql_locking_skip_locked.md)
3. [예약 가능 Pool 보충 시스템](../labs/04_replenishment_pool.md)
4. [작은 프로토타입 구현 계획](../labs/05_prototype_plan.md)

## 확인할 결과

- `FOR UPDATE` 사용 시 두 번째 트랜잭션이 대기하는지 확인한다.
- `FOR UPDATE SKIP LOCKED` 사용 시 잠긴 row를 건너뛰는지 확인한다.
- 같은 `unit_id`가 중복 예약되지 않는지 확인한다.
- 보충 작업이 같은 상품과 위치 단위로 직렬화되는지 확인한다.
