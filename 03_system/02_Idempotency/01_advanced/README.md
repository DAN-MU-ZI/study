# 01_advanced

`01_advanced`는 Redis를 사용하는 멱등성 실습 버전입니다.

구성 요소:

- `backend`: 주문 단위 멱등성, Redis 기반 락/결과 저장, 실패 결과 재전달, 그리고 데이터베이스 제약 조건 역할을 하는 저장소 단위 유니크 가드를 포함한 Spring Boot API
- `frontend`: 활성 주문마다 하나의 idempotency key를 유지하고, 새 주문이 시작될 때만 키를 교체하는 React 페이지
- `tests`: Docker 환경에서 실행할 수 있는 백엔드 통합 테스트와 Playwright QA 검증
- `qa`: 공용 QA 계획, 테스트 데이터, 버그 리포트 템플릿

## 서비스

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4173`
- Redis: `localhost:6379`

## 빠른 시작

앱 스택 실행:

```bash
docker compose up --build backend frontend
```

`http://localhost:4173`를 열고 다음을 확인합니다:

1. 현재 주문이 `PENDING` 상태로 로드된다.
2. `Pay order` 버튼을 더블 클릭해도 승인 건수는 1건만 생성된다.
3. 주문 결제가 끝난 뒤 `Start next order` 버튼이 나타나고, 다음 요청에서는 새 키가 사용된다.
4. 같은 주문에 대해서는 재시도 시 서로 다른 idempotency key를 사용하더라도 중복 승인이 발생하지 않는다.

## 검증

Redis를 포함한 Docker 환경에서 백엔드 테스트 실행:

```bash
docker compose run --rm backend-test
```

공용 QA 폴더에서 Playwright 스위트 실행:

```bash
docker compose -f qa/tests/docker-compose.yml run --rm playwright-advanced
```

## API 요약

- `GET /api/health`
- `POST /api/payments`
- `GET /api/payments`
- `GET /api/orders/current`
- `GET /api/orders/{orderId}`
- `POST /api/orders/next`

## 기본 시드값

- 최초 주문 ID: `1001`
- 고객 ID: `cust-001`
- 금액: `15000`

`POST /api/orders/next`를 호출할 때마다 다음 주문 번호를 가진 새로운 `PENDING` 주문이 생성됩니다.

## 멱등성 규칙

- 비어 있는 `Idempotency-Key`는 멱등성 처리 없이 그대로 통과한다.
- 비어 있지 않은 키는 같은 고객에 속해야 한다: `{customerId}:{suffix}`
- 백엔드는 키만이 아니라 논리적인 주문 단위로 중복 요청을 제거한다.
- 결제 저장소는 마지막 안전장치로 같은 주문에 대한 중복 insert를 거부하며, 이는 `orderId` 기준 데이터베이스 유니크 제약과 비슷한 역할을 한다.
- 같은 주문에 다른 payload를 재사용하면 `400 Bad Request`를 반환한다.
- 선행 요청이 만든 성공 또는 실패 결과는, 같은 주문에 대해 다른 idempotency key를 사용한 후행 요청에도 동일하게 전달된다.
