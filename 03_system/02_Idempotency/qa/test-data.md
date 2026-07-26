# 테스트 데이터

## 공통 시드
- 고객 ID: `cust-001`
- 최초 비즈니스 ID: `1001`
- 기본 결제 금액: `15000`

## 동적으로 생성되는 값
- baseline 새 pending 주문: `POST /api/orders/next`
- advanced 새 pending 장바구니: `POST /api/carts/next`
- advanced 멱등성 키 형식: `{customerId}:{suffix}`
- 예시 키: `cust-001:01HXYZ...`

## 주요 API
- `POST /api/payments`
- baseline
  - `GET /api/orders/current`
  - `GET /api/orders/{orderId}`
  - `POST /api/orders/next`
  - `GET /api/payments?orderId={orderId}`
- advanced
  - `GET /api/carts/current`
  - `GET /api/carts/{cartId}`
  - `POST /api/carts/next`
  - `GET /api/payments?cartId={cartId}`

## baseline 정상 결제 payload

```json
{
  "orderId": "<currentOrder>",
  "customerId": "cust-001",
  "amount": 15000
}
```

## advanced 정상 결제 payload

```json
{
  "cartId": "<currentCart>",
  "customerId": "cust-001",
  "amount": 15000
}
```

## advanced 충돌 payload 조합

허용 payload:

```json
{
  "cartId": "<currentCart>",
  "customerId": "cust-001",
  "amount": 15000
}
```

거부 payload:

```json
{
  "cartId": "<currentCart>",
  "customerId": "cust-001",
  "amount": 9900
}
```

## advanced 검증 실패 payload

```json
{
  "cartId": "<currentCart>",
  "customerId": "cust-001",
  "amount": 0
}
```

## 비교 기대 결과
- 같은 결제 대상을 빠르게 두 번 보냈을 때 baseline은 중복 승인 흔적이 보여야 하고, advanced는 1건 승인만 남아야 한다.
- 처리 중 버튼 상태는 baseline과 advanced가 달라야 한다.
- 다음 결제 대상을 시작했을 때 두 버전 모두 새 pending 상태로 전환되어야 하고, advanced는 새 `Idempotency-Key`까지 확인되어야 한다.

## 백엔드 통합 테스트 기대 결과
- advanced 같은 키 + 같은 payload:
  - 첫 요청이 성공한다.
  - 후행 요청은 같은 성공 결과를 받는다.
  - 같은 장바구니의 결제 이력은 1건만 남는다.
- advanced 다른 키 + 같은 장바구니 + 같은 payload:
  - 첫 요청이 성공한다.
  - 후행 요청은 같은 성공 결과를 받는다.
  - 같은 장바구니의 결제 이력은 1건만 남는다.
- advanced 같은 키 + 다른 payload:
  - 한 요청은 성공한다.
  - 다른 요청은 `400 Bad Request`를 받는다.
  - 같은 장바구니의 결제 이력은 1건만 남는다.
- advanced 다른 키 + 같은 장바구니 + 다른 payload:
  - 한 요청은 성공한다.
  - 다른 요청은 `400 Bad Request`를 받는다.
  - 같은 장바구니의 결제 이력은 1건만 남는다.
- advanced 같은 키 + 잘못된 payload:
  - 두 요청 모두 같은 실패 결과를 받는다.
  - 해당 장바구니에는 결제 이력이 남지 않는다.
- baseline 동일 주문 동시 요청:
  - 두 요청이 모두 승인될 수 있다.
  - 같은 주문의 결제 이력이 2건 남을 수 있다.

## 관찰 체크포인트
- baseline UI 주문 번호와 `GET /api/orders/current`의 주문 번호가 같은지
- advanced UI 장바구니 번호와 `GET /api/carts/current`의 장바구니 번호가 같은지
- baseline UI 결제 이력과 `GET /api/payments?orderId={activeOrderId}` 결과가 같은지
- advanced UI 결제 이력과 `GET /api/payments?cartId={activeCartId}` 결과가 같은지
- advanced에서 다음 장바구니 시작 후 새 `Idempotency-Key`가 발급되는지
- baseline 산출물에는 중복 승인 흔적이 남는지
- API 직접 호출 기반 동시성 검증은 Playwright가 아니라 백엔드 통합 테스트 결과로 확인하는지
