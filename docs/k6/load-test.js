import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 50 }, // 30초 동안 사용자 50명까지 증가 (Warm-up)
    { duration: '1m', target: 200 }, // 1분 동안 사용자 200명 유지 (Load)
    { duration: '30s', target: 0 },  // 30초 동안 사용자 0명으로 감소 (Cooldown)
  ],
};

export default function () {
  const url = 'http://localhost:8080/shorten';
  // 랜덤 문자열을 사용하여 매번 새로운 URL 생성
  const uniqueUrl = `https://example.com/${randomString(20)}-${Date.now()}`;
  
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
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(0.01); // 10ms 대기
}