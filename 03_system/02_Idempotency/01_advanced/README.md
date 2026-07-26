# 01_advanced

`01_advanced`는 Redis를 사용하는 멱등성 실습 버전입니다.

구성 요소:

- `backend`: 장바구니 단위 멱등성, Redis 기반 락/결과 저장, 실패 결과 재전달, 그리고 결제 저장소 유니크 가드를 포함한 Spring Boot API
- `frontend`: 활성 장바구니마다 하나의 idempotency key를 유지하고, 새 장바구니가 시작될 때만 키를 교체하는 React 페이지
- `tests`: Docker 환경에서 실행하는 백엔드 통합 테스트와 Playwright QA 검증
- `qa`: 공용 QA 계획, 테스트 데이터, 버그 리포트 템플릿

## 서비스

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4173`
- Redis: `localhost:6379`

## 빠른 시작

```bash
docker compose up --build backend frontend
```

`http://localhost:4173`에 접속해 다음을 확인합니다.

1. 현재 장바구니가 `PENDING` 상태로 로드된다.
2. `Pay cart` 버튼을 더블 클릭해도 승인 건수는 1건만 생성된다.
3. 장바구니 결제가 끝난 뒤 `Start next cart` 버튼이 활성화되고, 다음 요청에서는 새 키가 사용된다.
4. 같은 장바구니에 대해 다른 idempotency key를 사용하더라도 중복 승인이 발생하지 않는다.

## 검증

Redis를 포함한 Docker 환경에서 백엔드 테스트를 실행합니다.

```bash
docker compose run --rm backend-test
```

공용 QA 테스트에서 Playwright 시나리오를 실행합니다.

```bash
docker compose -f qa/tests/docker-compose.yml run --rm playwright-advanced
```

## API 요약

- `GET /api/health`
- `POST /api/payments`
- `GET /api/payments?cartId={cartId}`
- `GET /api/carts/current`
- `GET /api/carts/{cartId}`
- `POST /api/carts/next`

호환성 유지를 위해 기존 `orderId`, `/api/orders/*` 경로도 당분간 함께 허용합니다.

## 기본 시드값

- 최초 장바구니 ID: `1001`
- 고객 ID: `cust-001`
- 금액: `15000`

`POST /api/carts/next`를 호출할 때마다 다음 번호를 가진 새로운 `PENDING` 장바구니가 생성됩니다.

## 멱등성 규칙

- 비어 있는 `Idempotency-Key`는 멱등성 처리 없이 그대로 통과한다.
- 비어 있지 않은 키는 같은 고객에 속해야 한다: `{customerId}:{suffix}`
- 백엔드는 멱등성 저장소를 `cartId` 기준으로 관리한다.
- 결제 저장소는 마지막 안전장치로 같은 장바구니에 대한 중복 insert를 거부하며, 이는 `cartId` 기준 데이터베이스 유니크 제약과 비슷한 역할을 한다.
- 같은 장바구니에 다른 payload를 사용하면 `400 Bad Request`를 반환한다.
- 선행 요청이 만든 성공 또는 실패 결과는 같은 장바구니에 대한 후행 요청에도 동일하게 전달된다.
