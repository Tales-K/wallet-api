# Run and Test

This repo provides compose stacks and k6 scripts so reviewers can run everything with a few commands.

Build and run API locally (optional)

```bash
# Start Postgres if you don't have one running already
docker compose -f infra/docker-compose.yml up -d db

# Run the api (it lives under api/ directory)
./api/mvnw -f api/pom.xml spring-boot:run

# Ready to go! check docs here: http://localhost:8080/wallet-api/swagger-ui/index.html
```

1) Run API + DB (containers only)

```bash
docker compose -f infra/docker-compose.yml up --build
# API: http://localhost:8080/wallet-api
# DB:  localhost:5432  (wallet/wallet)
```

2) Run PostgresContainer tests locally (JUnit + Testcontainers)

```bash
# Runs unit tests
./api/mvnw -f api/pom.xml clean verify

# run integration tests; Testcontainers will start Postgres:17 automatically
./api/mvnw -f api/pom.xml -Dit.test=ConcurrencyIT clean test-compile failsafe:integration-test failsafe:verify
```

3) Run horizontally scaled API behind Nginx and k6 load test (+ Grafana)

- Start base + LB + Grafana/Loki/Promtail (optional observability)

```bash
# Bring up DB, API, LB, Grafana stack
docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.lb.yml \
  -f infra/docker-compose.observability.yml \
  up -d --build

# check metrics on grafana: http://localhost:3000/explore/metrics/trail (user/password: admin/admin)

# Scale API to 3 instances
docker compose -f infra/docker-compose.yml -f infra/docker-compose.lb.yml up -d --scale app=3
```

- Run k6 load test against the load balancer

```bash
# Choose the script via K6_SCRIPT; default is deposits.js
# BASE_URL points to the LB service name "lb" inside the compose network
TOTAL_DEPOSITS=100000 VUS=500 ITERATIONS_PER_VU=200 AMOUNT=1.00 BASE_URL=http://lb K6_SCRIPT=/scripts/deposits.js \
docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.lb.yml \
  -f infra/docker-compose.k6.yml \
  up --build --abort-on-container-exit k6

# Use other scenarios:
K6_SCRIPT=/scripts/withdraws.js ... up --abort-on-container-exit k6
K6_SCRIPT=/scripts/transfers.js ... up --abort-on-container-exit k6
```

Tear down

```bash

docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.lb.yml \
  -f infra/docker-compose.observability.yml \
  -f infra/docker-compose.logs.yml \
  -f infra/docker-compose.k6.yml \
  down -v

```

Notes

- Nginx resolves the "app" service and will distribute requests across replicas when you scale with --scale app=N.
- k6 scripts generate unique Idempotency-Key values and validate final balances in teardown (deposits).
- Grafana at http://localhost:3000 (admin/admin) with a Loki datasource for logs (app, nginx). Add dashboards or Prometheus later if you want latency histograms.
- Prometheus scrape URL: http://localhost:8080/wallet-api/actuator/prometheus
- Metrics browsing: http://localhost:8080/wallet-api/actuator/metrics
