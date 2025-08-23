# Wallet Service (Java) - Mission-Critical, Auditable, Horizontally Scalable

This service manages user wallets with operations to create wallets, retrieve balances (current and historical), and
post deposits, withdrawals, and transfers. It is designed to run multiple stateless instances concurrently without
distributed locks, while preventing duplicates and preserving a complete audit trail.

Contents

- Goals
- Assumptions and Decisions
- Architecture Overview
- Data Model and Invariants
- API Specification
- Concurrency and Idempotency
- Historical Balance
- Running Locally
- Testing Strategy
- Trade-offs and Future Work
- Time Tracking

Goals

- Safety under concurrent load, no double-spend.
- Strong auditability with an append-only ledger.
- Idempotent operations across retries and instances.
- Simple, explicit SQL; predictable performance.

Assumptions and Decisions

- Single currency; amounts stored as DECIMAL(19,4). Minimum > 0; maximum ≤ 999,999,999,999,999.9999.
- No overdrafts: balances must never go negative.
- Only “now” postings (no backdated/future-dated effective_at).
- All IDs server-generated ULIDs (wallet_id, tx_id, transfer_id). Transfer POST does not accept a transfer_id.
- Idempotency-Key header is required for all POSTs; responses cached in idempotency_keys for safe retries.
- Pagination uses page and size.
- No authentication for this assignment.
- Minimal metadata; we do not store user_agent or IP.
- PostgreSQL + Spring Boot + Spring Data JDBC. Liquibase for DB migrations.
- Isolation: READ COMMITTED with atomic conditional updates. No version column on balances.
- No outbox/events; the ledger is the audit source of truth.
- Hourly snapshots to accelerate historical balance queries.

Architecture Overview

- Stateless HTTP API behind a load balancer; multiple instances can process requests concurrently.
- Each request that mutates state runs in a single ACID transaction:
    - Validate input and Idempotency-Key.
    - Check idempotency cache; if present, return cached result.
    - Execute atomic SQL (conditional updates + inserts).
    - Insert append-only ledger entries.
    - Store final response in idempotency_keys.
- Transfers update two wallets within one transaction, with deterministic row update ordering to avoid deadlocks.

Data Model (logical)

- wallets
    - wallet_id ULID (PK), status, created_at TIMESTAMPTZ, metadata JSONB
- balances
    - wallet_id (PK, FK wallets), current_balance DECIMAL(19,4) NOT NULL DEFAULT 0, updated_at TIMESTAMPTZ
- ledger_entries (append-only)
    - ledger_id ULID (PK), tx_id ULID, wallet_id ULID, amount DECIMAL(19,4) NOT NULL
    - posting_type ENUM(deposit, withdraw, transfer_debit, transfer_credit)
    - transfer_id ULID NULL
    - created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    - metadata JSONB
    - UNIQUE (tx_id, wallet_id)
    - INDEX (wallet_id, created_at), (tx_id)
- transfers
    - transfer_id ULID (PK), from_wallet_id, to_wallet_id, amount DECIMAL(19,4), status ENUM(pending, completed,
      failed), created_at TIMESTAMPTZ
- idempotency_keys
    - idempotency_key TEXT (PK), method, path, request_hash TEXT
    - status ENUM(in_progress, succeeded, failed)
    - response_status INT, response_body JSONB
    - first_seen_at TIMESTAMPTZ, last_seen_at TIMESTAMPTZ
- snapshots
    - wallet_id ULID, snapshot_at TIMESTAMPTZ, balance_at_snapshot DECIMAL(19,4)
    - PK (wallet_id, snapshot_at)

Invariants

- No negative balances: enforced by SQL conditional updates.
- Idempotency: UNIQUE(tx_id, wallet_id) for postings and UNIQUE(transfer_id) for transfers; Idempotency-Key maps to the
  first processed result.
- Double-entry: Transfers create two ledger rows with the same transfer_id in the same transaction.
- Ledger is append-only; balances reflect the sum of ledger entries.

API Specification (v1)
Headers

- Idempotency-Key: required on POST endpoints.
- X-Correlation-Id: optional, echoed for tracing.

Endpoints

- POST /api/v1/wallets
    - Body: { "metadata": { ... } }
    - 201: { "wallet_id": "01J..." , "created_at": "..." }
- GET /api/v1/wallets/{wallet_id}/balance
    - 200: { "wallet_id": "01J...", "balance": "123.45", "as_of": "timestamp" }
- GET /api/v1/wallets/{wallet_id}/balance/history?at=2025-01-01T00:00:00Z
    - 200: { "wallet_id": "01J...", "balance": "100.00", "as_of": "..." }
- POST /api/v1/wallets/{wallet_id}/deposit
    - Headers: Idempotency-Key
    - Body: { "amount": "50.00", "metadata": { ... } }
    - 200: { "tx_id": "01J...", "wallet_id": "01J...", "new_balance": "..." }
- POST /api/v1/wallets/{wallet_id}/withdraw
    - Headers: Idempotency-Key
    - Body: { "amount": "25.00", "metadata": { ... } }
    - 200: { "tx_id": "01J...", "wallet_id": "01J...", "new_balance": "..." }
    - 409: { "code": "INSUFFICIENT_FUNDS", "message": "..." }
- POST /api/v1/transfers
    - Headers: Idempotency-Key
    - Body: { "from_wallet_id": "01J...", "to_wallet_id": "01J...", "amount": "10.00", "metadata": { ... } }
    - 200: { "transfer_id": "01J...", "status": "completed", "from_wallet_id": "...", "to_wallet_id": "...", "amount": "
      10.00" }
- GET /api/v1/transfers/{transfer_id}
    - 200: { "transfer_id": "01J...", "status": "completed" }
- GET /api/v1/wallets/{wallet_id}/ledger?page=0&size=50&from=...&to=...
    - 200: { "page": 0, "size": 50, "total": 12345, "entries": [ ... ] }

Errors

- 400: validation errors (non-positive amounts, invalid IDs).
- 404: wallet or transfer not found.
- 409: insufficient funds; idempotency-key mismatch (same key, different request).
- 422: semantic errors (e.g., self-transfer).
- 500: unexpected errors.

Concurrency and Idempotency

- Deposits (single wallet):
    - UPDATE balances SET current_balance = current_balance + :amount WHERE wallet_id = :id;
    - Insert ledger_entries with tx_id; commit.
- Withdrawals (single wallet):
    - UPDATE balances SET current_balance = current_balance - :amount WHERE wallet_id = :id AND current_balance >= :
      amount;
    - If 0 rows updated → 409 insufficient funds; else insert ledger entry; commit.
- Transfers (two wallets):
    - Determine deterministic order by wallet_id; debit source with conditional update; credit destination; insert two
      ledger entries; mark transfer completed; commit.
- Idempotency handling:
    - On first-seen key: mark in_progress; process; store response; return.
    - On replay with same key and request_hash: return stored response and status.
    - On replay with same key but different request_hash: 409 conflict.

Historical Balance

- Compute balance at t by summing ledger amounts with created_at ≤ t.
- Hourly snapshots: answer by combining latest snapshot ≤ t with incremental sum up to t.
- Indexes: ledger_entries (wallet_id, created_at), (tx_id).

Running Locally
Prerequisites

- JDK 21, Docker + Docker Compose, Maven 3.9+ (or ./mvnw)

Build

```bash
./mvnw clean package
```

Run (compose)

```bash
docker compose up --build
```

- Starts PostgreSQL and the app. Liquibase migrations run on startup.

Configuration (env)

- DATABASE_URL (jdbc:postgresql://localhost:5432/wallet)
- DATABASE_USER (wallet)
- DATABASE_PASSWORD (wallet)
- HTTP_PORT (8080)

OpenAPI

- /v3/api-docs and /swagger-ui/index.html

Testing Strategy

- Unit tests for validation, invariants (min/max amount, self-transfer rejection).
- Integration tests (Testcontainers Postgres):
    - Concurrent withdrawals exceeding balance → only some succeed; no negatives.
    - Cross-transfers (A↔B) with deterministic ordering → no deadlocks; balances and ledger consistent.
    - Idempotency replays → identical response/status without re-execution.
- Property tests:
    - Sum of all balances equals net deposits; transfers conserve total.
- Historical queries:
    - Verify snapshot + incremental sum equals full sum.

Trade-offs and Future Work

- Isolation: READ COMMITTED chosen for throughput; can elevate to REPEATABLE READ if complex read-then-write patterns
  emerge.
- No outbox/events: OK now; add outbox for external integrations later.
- Partitioning: Add if ledger grows large (time-based or hash-by-wallet).
- Observability, load/soak testing, deployment/K8s: planned later.

Time Tracking

- Will report rough hours by phase at completion:
    - Schema + migrations
    - API endpoints
    - Concurrency/idempotency
    - Tests
    - Historical balance + snapshots
    - Docs
