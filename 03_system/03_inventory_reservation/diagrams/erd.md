# Inventory Reservation ERD

Docker 실습 환경의 MySQL 스키마 기준 ERD다. 렌더러 입력 원본은 [erd.mmd](./erd.mmd)에 둔다.

![Inventory Reservation ERD](./erd.svg)

## 읽는 법

- `inventory_ledger`는 실제 재고 원장이다.
- `legacy_reservation_log`는 Redis 기반 레거시 흐름에서 MySQL에 남기는 예약 기록이다.
- `reservation_units`는 개선 구조에서 예약 가능한 재고 1개를 표현하는 pool이다.
- `reserved_quantities`는 예약되어 결제 대기 중인 재고 단위다.
- `replenishment_state`는 같은 상품과 위치에 대해 보충 작업을 직렬화하기 위한 상태다.

## 주의할 점

`reservation_units`와 `reserved_quantities`의 관계는 실제 DB foreign key로 강제하지 않는다. 예약 시 `reservation_units`의 row가 `reserved_quantities`로 이동하는 개념 관계다.

## beautiful-mermaid 렌더링

`beautiful-mermaid`는 [erd.mmd](./erd.mmd)를 입력으로 사용해 [erd.svg](./erd.svg)를 생성한다.

```powershell
cd D:\_Workspace\02_프로젝트\진행중\study\03_system\03_inventory_reservation
npm install
npm run render:diagrams
```

렌더링 스크립트는 [render-erd.mjs](./render-erd.mjs)에 둔다.
