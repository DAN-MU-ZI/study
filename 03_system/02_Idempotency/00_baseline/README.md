# 00_baseline

`00_baseline` is the intentionally unsafe comparison version of the idempotency lab.

It contains:

- `backend`: Spring Boot API without Redis or idempotency protection.
- `frontend`: React page that can submit the same logical order more than once while the request is still in flight.
- `backend-test`: Docker-friendly tests that prove duplicate approvals can happen under concurrency.

## Services

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4173`

## Quick Start

Start the app stack:

```bash
docker compose up --build backend frontend
```

Open `http://localhost:4173` and verify:

1. The current order loads as `PENDING`.
2. Double-clicking `Pay order` can create two approvals for the same order.
3. The warning banner appears once duplicate approvals exist.
4. `Start next order` creates a fresh pending order so you can reproduce the issue again.

## Verification

Run backend tests:

```bash
docker compose run --rm backend-test
```

Run the Playwright suite from the shared QA folder:

```bash
docker compose -f ../qa/tests/docker-compose.yml run --rm playwright-baseline
```

The baseline suite may pass at the automation layer when the unsafe behavior is reproduced.
However, the final QA verdict must still be judged against the advanced stability requirements, so the baseline remains a failing comparison target for those safety cases.

## API Summary

- `GET /api/health`
- `POST /api/payments`
- `GET /api/payments`
- `GET /api/orders/current`
- `GET /api/orders/{orderId}`
- `POST /api/orders/next`

## Default Seed

- First seeded order id: `1001`
- Customer id: `cust-001`
- Amount: `15000`

Every call to `POST /api/orders/next` creates a fresh pending order with the next order id.

## Expected Failure

- Two concurrent `POST /api/payments` calls for the same order can both pass the `PENDING` check.
- The mock PG layer approves both requests independently.
- The order ends as `PAID`, but the payment history contains multiple approvals for one logical order.
