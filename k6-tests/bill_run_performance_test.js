import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<300000'], // 95% of requests must complete under 5 minutes
  },
};

export default function () {
  const url = 'http://billing-service:9007/api/v1/billing/runs';
  
  console.log('Triggering daily bill run for 1000 users...');
  const res = http.post(url);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'body says triggered': (r) => r.body.includes('Bill run triggered successfully'),
  });
}
