ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS ref_id uuid;
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_ref_id ON idempotency_keys (ref_id);

