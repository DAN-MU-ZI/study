import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '2s',
  thresholds: __ENV.FORCE_THRESHOLD_FAILURE === '1'
    ? { checks: ['rate>1.1'] }
    : {},
};

export default function () {
  const response = http.get('http://localhost:8080/actuator/health');
  check(response, {
    'health endpoint returns 200': result => result.status === 200,
  });
  sleep(0.1);
}
