# 개선 DB 구조 시각화

Shopify Engineering의 `Scaling Inventory Reservations` 글과 이 저장소의 MySQL 스키마를 바탕으로 만든 교육용 시각화다.

## 실행

```powershell
npm install
npm run dev
```

빌드 확인:

```powershell
npm run build
```

## 보여주는 흐름

- `Reserve`: `reservation_units` row를 `FOR UPDATE SKIP LOCKED`로 잠근 뒤 `reserved_quantities`로 이동한다.
- `동시 결제 요청`: 첫 트랜잭션이 잠근 row를 두 번째 트랜잭션이 건너뛰는 모습을 보여준다.
- `Claim`: 결제 성공 후 `inventory_ledger.claimed_quantity`가 증가한다.
- `Release`: 결제 실패 또는 만료 시 예약 row를 제거하고 보충 필요 상태로 표시한다.
- `Replenish`: `replenishment_state.next_unit_id` 기준으로 `reservation_units` pool을 다시 채운다.

실제 DB에 연결하지 않는다. 모든 상태는 브라우저 메모리에서만 바뀐다.
