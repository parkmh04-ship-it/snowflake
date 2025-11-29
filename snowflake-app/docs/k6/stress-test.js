import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '2m', target: 100 }, // 100 VU까지 2분간 서서히 증가
    { duration: '5m', target: 100 }, // 100 VU 유지
    { duration: '2m', target: 200 }, // 200 VU까지 2분간 증가
    { duration: '5m', target: 200 }, // 200 VU 유지
    { duration: '2m', target: 300 }, // 300 VU까지 2분간 증가
    { duration: '5m', target: 300 }, // 300 VU 유지
    { duration: '2m', target: 400 }, // 400 VU까지 2분간 증가 (한계점 탐색)
    { duration: '5m', target: 400 }, // 400 VU 유지
    { duration: '10m', target: 0 },  // 10분간 서서히 0으로 감소 (Recovery)
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% 요청은 500ms 이내여야 함
  },
};

export default function () {
  const url = 'http://localhost:8080/shorten';
  const uniqueUrl = `https://example.com/${randomString(10)}-${Date.now()}`;
  
  const payload = JSON.stringify({
    url: uniqueUrl,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'is status 201': (r) => r.status === 201,
  });

  sleep(0.01); 
}
