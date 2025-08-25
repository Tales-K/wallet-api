import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

var BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
var VUS = parseInt(__ENV.VUS || '200', 10);
var ITER_PER_VU = parseInt(__ENV.ITERATIONS_PER_VU || '100', 10);
var TOTAL_DEPOSITS = parseInt(__ENV.TOTAL_DEPOSITS || `${VUS * ITER_PER_VU}`, 10);
var AMOUNT = __ENV.AMOUNT || '1.00';

if (VUS * ITER_PER_VU !== TOTAL_DEPOSITS) {
  console.error(`VUS * ITERATIONS_PER_VU must equal TOTAL_DEPOSITS. Got ${VUS * ITER_PER_VU}, expected ${TOTAL_DEPOSITS}`);
  exec.test.abort();
}

export var options = {
  vus: VUS,
  iterations: VUS * ITER_PER_VU,
  thresholds: {
    http_req_failed: ['rate<0.005'],
    http_req_duration: ['p(95)<800'],
  },
  noConnectionReuse: false,
  discardResponseBodies: true,
};

export function setup() {
  var res = http.post(`${BASE_URL}/api/v1/wallets`, JSON.stringify({ metadata: { test: 'k6' } }), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'wallet created': (r) => r.status === 201 });
  var body = res.json();
  var walletId = body.wallet_id || body.walletId;
  if (!walletId) {
    console.error(`Unexpected create wallet response: ${res.body}`);
    exec.test.abort();
  }
  return { walletId };
}

export default function (data) {
  var idem = uuidv4();
  var res = http.post(`${BASE_URL}/api/v1/wallets/${data.walletId}/deposit`, JSON.stringify({ amount: AMOUNT }), {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idem },
  });
  check(res, { 'deposit ok or duplicate': (r) => r.status === 200 || r.status === 409 });
}

export function teardown(data) {
  var res = http.get(`${BASE_URL}/api/v1/wallets/${data.walletId}`, { headers: { Accept: 'application/json' } });
  check(res, { 'balance 200': (r) => r.status === 200 });
  var json = res.json();
  var balance = parseFloat(json.currentBalance || json.current_balance || json.balance || json.new_balance);
  var expected = TOTAL_DEPOSITS * parseFloat(AMOUNT);
  var ok = Math.abs(balance - expected) < 0.0001;
  check(null, { 'final balance matches expected': () => ok });
  if (!ok) console.error(`Final balance ${balance} != expected ${expected}`);
}r
