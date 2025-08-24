# Requirements

- Given the importance of the project, I decided to skip authentication
  implementation in order to focus on consistency and performance.
- I found that idempotency is crucial for this project, so I implemented it
  using a combination of a unique request identifier and a database transaction.
- I designed the system as stateless, which allows for easy scaling and load
  balancing.
- Future cleanup schedules should be implemented to clean pending in_progress
  idempotency keys.
- There are two ways of fetching balance history: by informing desired date and
  time, and checking ledgers by time range filter.

# Pending implementations

- add current_balance at ledger records
- retrieve balance by time
- retrieve ledger by time range
- create unit tests
- create integration tests
- add load balancer
- create load tests

# Utils

```shell
docker exec -it wallet-postgres psql -U wallet_user -d postgres -c "DROP DATABASE IF EXISTS wallet;"
docker exec -it wallet-postgres psql -U wallet_user -d postgres -c "CREATE DATABASE wallet OWNER wallet_user;"
```
