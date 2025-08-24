-- Enforce append-only semantics on ledger_entries. Any corrections should be made by new entries.
-- This trigger prevents UPDATE and DELETE operations on the ledger_entries table.

CREATE OR REPLACE FUNCTION ledger_entries_append_only_guard()
RETURNS trigger
LANGUAGE plpgsql
AS '
BEGIN
  RAISE EXCEPTION ''ledger_entries is append-only; % not allowed'', TG_OP;
END;
';

DROP TRIGGER IF EXISTS ledger_entries_append_only ON ledger_entries;

CREATE TRIGGER ledger_entries_append_only
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION ledger_entries_append_only_guard();
