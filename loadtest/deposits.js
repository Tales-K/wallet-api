import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

var BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export var options = {
  vus: 50,
  iterations: 1000,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export function setup() {
  var res = http.post(`${BASE_URL}/wallet-api/api/v1/wallets`, '{}', {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });

  check(res, { 'wallet created': (r) => r.status === 201 });

  var walletId = res.json().walletId;
  return { walletId };
}

export default function (data) {
  var res = http.post(`${BASE_URL}/wallet-api/api/v1/wallets/${data.walletId}/deposit`,
    JSON.stringify({ amount: '1.00' }), {
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
    });

  check(res, { 'deposit successful': (r) => r.status === 200 });
}

export function teardown(data) {
  var res = http.get(`${BASE_URL}/wallet-api/api/v1/wallets/${data.walletId}`, {
    headers: { 'Accept': 'application/json' }
  });

  check(res, { 'balance retrieved': (r) => r.status === 200 });

  var balance = parseFloat(res.json().currentBalance);
  var expected = 1000.00;

  check(null, { 'balance matches expected': () => Math.abs(balance - expected) < 0.01 });
}
