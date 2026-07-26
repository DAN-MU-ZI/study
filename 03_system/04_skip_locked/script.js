import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const successfulPurchases = new Counter('successful_purchases');
const conflictResponses = new Counter('conflict_responses');
const otherFailures = new Counter('other_failures');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '60s', target: 50 },
    { duration: '60s', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

export default function () {
  const res = http.post(
    'http://localhost:8080/buy',
    JSON.stringify({ id: 1, quantity: 1 }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status === 200) {
    successfulPurchases.add(1);
  } else if (res.status === 409) {
    conflictResponses.add(1);
  } else {
    otherFailures.add(1);
  }

  check(res, {
    'status is 200': r => r.status === 200,
  });

  sleep(0.1);
}
