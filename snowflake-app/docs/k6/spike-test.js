import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '10s', target: 100 }, // 10초 만에 0 -> 100 VU 급증
    { duration: '1m', target: 100 },  // 1분간 유지
    { duration: '10s', target: 1000 }, // 10초 만에 100 -> 1000 VU 폭증 (Spike)
    { duration: '3m', target: 1000 },  // 3분간 고부하 유지
    { duration: '10s', target: 100 },  // 10초 만에 1000 -> 100 VU 급감
    { duration: '3m', target: 100 },   // 3분간 유지 (Recovery 확인)
    { duration: '10s', target: 0 },    // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 스파이크 시에는 좀 더 여유롭게 2초 이내
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
