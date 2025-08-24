CREATE TABLE IF NOT EXISTS wallets (
  wallet_id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  current_balance  numeric(19,4) NOT NULL DEFAULT 0,
  updated_at       timestamptz NOT NULL DEFAULT now(),
  created_at       timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_wallet_balance_nonnegative CHECK (current_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wallets_updated_at ON wallets (updated_at);
