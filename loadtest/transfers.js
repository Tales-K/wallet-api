import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
var BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
var VUS = parseInt(__ENV.VUS || '200', 10);
var ITER_PER_VU = parseInt(__ENV.ITERATIONS_PER_VU || '100', 10);
var AMOUNT = __ENV.AMOUNT || '1.00';

export var options = { vus: VUS, iterations: VUS * ITER_PER_VU, discardResponseBodies: true };

export function setup() {
  var w1 = createWallet();
  var w2 = createWallet();
  deposit(w1, '10000.00');
  deposit(w2, '10000.00');
  return { w1, w2 };
}

export default function (data) {
  var body = JSON.stringify({ fromWalletId: data.w1, toWalletId: data.w2, amount: AMOUNT });
  var res = http.post(`${BASE_URL}/api/v1/transfers`, body, {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'transfer 2xx/409': (r) => r.status === 200 || r.status === 409 });
}

export function teardown(data) {
  var res1 = http.get(`${BASE_URL}/api/v1/wallets/${data.w1}`, { headers: { Accept: 'application/json' } });
  var res2 = http.get(`${BASE_URL}/api/v1/wallets/${data.w2}`, { headers: { Accept: 'application/json' } });
  var b1 = parseFloat(res1.json().currentBalance || res1.json().current_balance || res1.json().balance || res1.json().new_balance);
  var b2 = parseFloat(res2.json().currentBalance || res2.json().current_balance || res2.json().balance || res2.json().new_balance);
  if (Math.abs(b1 + b2 - 20000.0) > 0.0001) {
    console.error(`Conservation failed: ${b1} + ${b2} != 20000`);
  }
}

function createWallet() {
  var res = http.post(`${BASE_URL}/api/v1/wallets`, JSON.stringify({}), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'wallet created': (r) => r.status === 201 });
  var json = res.json();
  return json.wallet_id || json.walletId;
}

function deposit(walletId, amount) {
  http.post(`${BASE_URL}/api/v1/wallets/${walletId}/deposit`, JSON.stringify({ amount }), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
}ta
