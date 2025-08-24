CREATE TABLE IF NOT EXISTS transfers (
  transfer_id     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  from_wallet_id  uuid NOT NULL,
  to_wallet_id    uuid NOT NULL,
  amount          numeric(19,4) NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_transfers_from FOREIGN KEY (from_wallet_id) REFERENCES wallets(wallet_id) ON DELETE RESTRICT,
  CONSTRAINT fk_transfers_to   FOREIGN KEY (to_wallet_id)   REFERENCES wallets(wallet_id) ON DELETE RESTRICT,
  CONSTRAINT chk_transfer_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_transfer_not_self CHECK (from_wallet_id <> to_wallet_id)
);

CREATE INDEX IF NOT EXISTS idx_transfers_from_wallet ON transfers (from_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transfers_to_wallet   ON transfers (to_wallet_id);
