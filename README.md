# Wallet Service (Java) - Mission-Critical, Auditable, Horizontally Scalable

This service manages user wallets with operations to create wallets, retrieve balances (current and historical), and post deposits, withdrawals, and transfers.
It is designed to run multiple stateless instances concurrently without distributed locks, while preventing duplicates and preserving a complete audit trail.

# Contents

1. [How to run the project](#how-to-run-the-project)
2. [Decisions](#decisions)
3. [Assumptions](#assumptions)
4. [Architecture Overview](#architecture-overview)
5. [Infrastructure](#infrastructure)
6. [Running tests](#running-tests)
8. [Running the API locally](#running-the-api-locally)

# How to run the project

Pre-requisites:

- Docker and Docker Compose.

### 1) Run all services (API, db, load-balancer, monitoring and tracing):

```bash
docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.lb.yml \
  -f infra/docker-compose.observability.yml \
  -f infra/docker-compose.zipkin.yml \
  up -d --build --scale app=3
```

### 2) Check API status

Credentials:

```
user: admin
password: admin
```

- [Metrics](http://localhost:3000/explore/metrics/trail)
- [Logs](http://localhost:3000/explore?left=%5B%22now-1h%22,%22now%22,%22Loki%22,%7B%22expr%22%3A%22%7Bjob%3D%5C%22app-logs%5C%22%7D%22%7D%5D)
- [Traces](http://localhost:9411)

### 3) Run requests

#### Option 1

Run manually through [the API documentation](http://localhost:8081/wallet-api/swagger-ui/index.html) (has endpoints, schemas and errors).
Here you can create wallets, do deposits, withdrawals and transfers, and check balances and ledgers.

#### Option 2

Run a load test and do lots of requests:

```bash
K6_SCRIPT=transfers.js docker compose -f infra/docker-compose.k6.yml run --rm k6
```

You can replace `transfers.js` with `withdraws.js` or `deposits.js` to test those operations.

# Decisions

- Given the importance of the project, I decided to skip authentication implementation in order to focus on consistency and performance.
- I found that idempotency is crucial for this project, so I implemented it using a combination of a unique request identifier and database transaction.
- I designed the system as stateless, which allows for easy scaling and load balancing.
- Future cleanup schedules should be implemented to clean old idempotency keys.
- There are two ways of fetching balance history: by informing desired date and time, and checking ledgers by time range filter.

# Assumptions

- Single currency; amounts stored as DECIMAL(19,4).
- No overdrafts: balances must never go negative.
- Only “now” postings (no backdated/future-dated effective_at).
- All IDs server-generated UUID v4 (wallet_id, tx_id), except for idempotency_key.
- Idempotency-Key header is required for all POSTs; responses are cached for safe retries.
- Idempotency state machine is intentionally simple: only in_progress, succeeded and failed.
    - An in_progress entry is considered stale after 5 seconds.
    - If a retry arrives after that window, the stale in_progress is treated as abandoned and the retry proceeds (stale takeover). This ensures transient errors never block progress.
- The ledger is the audit source of truth.

# Architecture Overview

- Each request that mutates state runs in a single ACID transaction:
    - Validate input and Idempotency-Key.
    - Check idempotency cache; if present and completed, return cached result.
    - Execute atomic SQL (conditional updates + inserts).
    - Insert append-only ledger entries.
    - Store final response in idempotency_keys.
- Transfers update two wallets within one transaction.

# Infrastructure

- Docker Compose is used to orchestrate the services.
- Added observability stack (Grafana, Loki, Promtail) for metrics and logs.
- Added tracing stack (Micrometer, Zipkin) for distributed tracing.
- Nginx is used as a load balancer to distribute requests across multiple API instances.
- k6 is used for load testing the API.
- Testcontainers are used for integration tests.

# Running tests

### Unit tests

Classic unit tests made with JUnit and Mockito.

```bash
./api/mvnw -f api/pom.xml test
```

### Integration tests

Made with Testcontainers, they run a real Postgres instance in a container.

```bash
./api/mvnw -f api/pom.xml -Dit.test=ConcurrencyIT failsafe:integration-test failsafe:verify
```

### Load tests

Made with k6, they target the load-balancer so it spreads across 3 instances.

```bash
./api/mvnw -f api/pom.xml -Dit.test=ConcurrencyIT clean test-compile failsafe:integration-test failsafe:verify
```

# Running the API locally

#### Requires JDK 21, Maven and Postgres

1. Start your postgres with a `wallet` database, `wallet_user` and `wallet_password` credentials.
2. Run the API:

```bash
./api/mvnw -f api/pom.xml spring-boot:run
```

# Time tracking

```
    [1h] - research (atomicity and idempotency patterns)
    [2h] - development of transaction entities
    [2h] - implementation of idempotency
    [2h] - testing (unit, integration and load) and bugfix
    [2h] - infra setup
    [1h] - documentation and review
```
