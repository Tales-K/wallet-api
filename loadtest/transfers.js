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
  var w1 = createWallet();
  var w2 = createWallet();
  deposit(w1, '10000.00');
  deposit(w2, '10000.00');
  return { w1, w2 };
}

export default function (data) {
  var res = http.post(`${BASE_URL}/wallet-api/api/v1/transfers`,
    JSON.stringify({ fromWalletId: data.w1, toWalletId: data.w2, amount: '1.00' }), {
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
    });

  check(res, { 'transfer successful': (r) => r.status === 200 });
}

export function teardown(data) {
  var res1 = http.get(`${BASE_URL}/wallet-api/api/v1/wallets/${data.w1}`);
  var res2 = http.get(`${BASE_URL}/wallet-api/api/v1/wallets/${data.w2}`);

  check(res1, { 'wallet 1 retrieved': (r) => r.status === 200 });
  check(res2, { 'wallet 2 retrieved': (r) => r.status === 200 });

  var wallet1 = res1.json();
  var wallet2 = res2.json();

  var balance1 = parseFloat(wallet1.currentBalance);
  var balance2 = parseFloat(wallet2.currentBalance);
  var totalBalance = balance1 + balance2;

  var expectedBalance1 = 9000.00;
  var expectedBalance2 = 11000.00;
  var expectedTotal = 20000.00;

  check(null, { 'wallet 1 balance matches expected': () => Math.abs(balance1 - expectedBalance1) < 0.01 });
  check(null, { 'wallet 2 balance matches expected': () => Math.abs(balance2 - expectedBalance2) < 0.01 });
  check(null, { 'balance conservation verified': () => Math.abs(totalBalance - expectedTotal) < 0.01 });
}

function createWallet() {
  var res = http.post(`${BASE_URL}/wallet-api/api/v1/wallets`, '{}', {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'wallet created': (r) => r.status === 201 });
  return res.json().walletId;
}

function deposit(walletId, amount) {
  http.post(`${BASE_URL}/wallet-api/api/v1/wallets/${walletId}/deposit`,
    JSON.stringify({ amount }), {
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
    });
}
