CREATE TABLE IF NOT EXISTS ledger_entries (
  ledger_id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tx_id            uuid NOT NULL,
  wallet_id        uuid NOT NULL REFERENCES wallets(wallet_id) ON DELETE RESTRICT,
  amount           numeric(19,4) NOT NULL,
  current_balance  numeric(19,4) NOT NULL,
  posting_type     posting_type  NOT NULL,
  created_at       timestamptz NOT NULL DEFAULT now(),

  CONSTRAINT uq_ledger_tx_per_wallet UNIQUE (tx_id, wallet_id),
  CONSTRAINT chk_ledger_amount_nonzero CHECK (amount <> 0),
  CONSTRAINT chk_ledger_amount_sign CHECK (
    (posting_type IN ('deposit','transfer_credit') AND amount > 0)
    OR
    (posting_type IN ('withdraw','transfer_debit') AND amount < 0)
  )
);

CREATE INDEX IF NOT EXISTS idx_ledger_wallet_created_at ON ledger_entries (wallet_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_id             ON ledger_entries (tx_id);
