import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://api:8080';
const tag = __ENV.TAG || 'java';

export const options = {
  vus: Number(__ENV.VUS || 20),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export function setup() {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const response = http.get(`${baseUrl}/actuator/health`);
    if (response.status === 200) {
      return;
    }
    sleep(1);
  }
  throw new Error('API가 30초 안에 준비되지 않았습니다.');
}

export default function () {
  const page = Math.floor(Math.random() * 20);
  const response = http.get(
    `${baseUrl}/api/posts?tag=${encodeURIComponent(tag)}&page=${page}&pageSize=20`,
  );

  check(response, {
    'status is 200': (result) => result.status === 200,
    'response contains data': (result) => Array.isArray(result.json('data')),
  });
  sleep(0.1);
}

