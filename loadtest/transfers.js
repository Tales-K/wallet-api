import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '200', 10);
const ITER_PER_VU = parseInt(__ENV.ITERATIONS_PER_VU || '100', 10);
const AMOUNT = __ENV.AMOUNT || '1.00';

export const options = { vus: VUS, iterations: VUS * ITER_PER_VU, discardResponseBodies: true };

export function setup() {
  const w1 = createWallet();
  const w2 = createWallet();
  deposit(w1, '10000.00');
  deposit(w2, '10000.00');
  return { w1, w2 };
}

export default function (data) {
  const body = JSON.stringify({ from_wallet_id: data.w1, to_wallet_id: data.w2, amount: AMOUNT });
  const res = http.post(`${BASE_URL}/api/v1/transfers`, body, {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'transfer 2xx/409': (r) => r.status === 200 || r.status === 409 });
}

export function teardown(data) {
  const b1 = parseFloat(http.get(`${BASE_URL}/api/v1/wallets/${data.w1}/balance`).json().balance);
  const b2 = parseFloat(http.get(`${BASE_URL}/api/v1/wallets/${data.w2}/balance`).json().balance);
  if (Math.abs(b1 + b2 - 20000.0) > 0.0001) {
    console.error(`Conservation failed: ${b1} + ${b2} != 20000`);
  }
}

function createWallet() {
  const res = http.post(`${BASE_URL}/api/v1/wallets`, JSON.stringify({}), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'wallet created': (r) => r.status === 201 });
  const json = res.json();
  return json.wallet_id || json.walletId;
}

function deposit(walletId, amount) {
  http.post(`${BASE_URL}/api/v1/wallets/${walletId}/deposit`, JSON.stringify({ amount }), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
}
