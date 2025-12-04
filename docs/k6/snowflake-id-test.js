import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '10s', target: 50 }, // Warm up
    { duration: '30s', target: 200 }, // Load
    { duration: '10s', target: 0 }, // Cooldown
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests should be below 200ms
  },
};

export default function () {
  const url = 'http://localhost:8081/api/v1/global-transaction-id';
  
  // 1. Generate new ID
  let payload = JSON.stringify({});
  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  let res = http.post(url, payload, params);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'has globalTransactionId': (r) => JSON.parse(r.body).globalTransactionId !== undefined,
  });

  const body = JSON.parse(res.body);
  const originalId = body.globalTransactionId;

  // 2. Generate with origin ID (Simulate Cancellation)
  if (originalId) {
      payload = JSON.stringify({
          originGlobalTransactionId: originalId
      });
      res = http.post(url, payload, params);
      check(res, {
        'status is 200 (cancel)': (r) => r.status === 200,
      });
  }

  sleep(1);
}
