ALTER TABLE ledger_entries DROP COLUMN IF EXISTS transfer_id;
DROP INDEX IF EXISTS idx_ledger_transfer_id;
-- Ensure indexes
CREATE INDEX IF NOT EXISTS idx_ledger_tx_id ON ledger_entries (tx_id);
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_created_at ON ledger_entries (wallet_id, created_at);

