# Wallet Service (Java) - Mission-Critical, Auditable, Horizontally Scalable

# API Documentation

Endpoints, schema and errors are all mapped out in the OpenAPI spec, available at:

- http://localhost:8081/wallet-api/swagger-ui/index.html

# Postgres container

```
docker run -d \
  --name wallet-postgres \
  -e POSTGRES_DB=wallet \
  -e POSTGRES_USER=wallet_user \
  -e POSTGRES_PASSWORD=wallet_password \
  -p 5432:5432 \
  -v wallet_pg_data:/var/lib/postgresql/data \
  postgres:17
```

# Utils

```shell
docker exec -it wallet-postgres psql -U wallet_user -d postgres -c "DROP DATABASE IF EXISTS wallet;"
docker exec -it wallet-postgres psql -U wallet_user -d postgres -c "CREATE DATABASE wallet OWNER wallet_user;"
```

This service manages user wallets with operations to create wallets, retrieve balances (current and historical), and post deposits, withdrawals, and transfers.
It is designed to run multiple stateless instances concurrently without distributed locks, while preventing duplicates and preserving a complete audit trail.

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
