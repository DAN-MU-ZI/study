import { check } from 'k6';
import http from 'k6/http';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    load_test: {
      executor: 'constant-arrival-rate',
      duration: '30s',
      preAllocatedVUs: 1000,
      rate: 1000,      // 초당 1000회 요청
      timeUnit: '1s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8000';

export default function () {
  const url = `${BASE_URL}/api/v1/data/shorten`;

  // 1. Create Short URL
  const longUrl = `https://example.com/${randomString(10)}`;
  const payload = JSON.stringify({ longUrl: longUrl });
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const resPost = http.post(url, payload, params);
  check(resPost, {
    'Create status must be 200': (r) => r.status === 200,
  });

  if (resPost.status !== 200) {
    return;
  }

  const shortUrl = resPost.json().shortUrl;

  // 2. Get (Redirect) 10 times → 1:10 비율 구현
  for (let i = 0; i < 10; i++) {
    const resGet = http.get(`${BASE_URL}/api/v1/${shortUrl}`, { redirects: 0 });
    check(resGet, {
      'Redirect status must be 301 or 302': (r) => r.status === 301 || r.status === 302,
    });
  }
}
