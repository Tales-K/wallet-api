CREATE TABLE IF NOT EXISTS idempotency_keys (
  idempotency_key  uuid PRIMARY KEY,
  method           text NOT NULL,
  path             text NOT NULL,
  request_hash     text NOT NULL,
  status           idempotency_status NOT NULL,
  response_status  int,
  response_body    jsonb,
  first_seen_at    timestamptz NOT NULL DEFAULT now(),
  last_seen_at     timestamptz NOT NULL DEFAULT now()
);
