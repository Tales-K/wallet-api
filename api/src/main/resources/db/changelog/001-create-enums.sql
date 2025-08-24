DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'posting_type') THEN
    CREATE TYPE posting_type AS ENUM ('deposit', 'withdraw', 'transfer_debit', 'transfer_credit');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'idempotency_status') THEN
    CREATE TYPE idempotency_status AS ENUM ('in_progress', 'succeeded', 'failed');
  END IF;
END$$;
