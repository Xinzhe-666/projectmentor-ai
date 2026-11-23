CREATE TABLE ledger_entry (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    amount_minor BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE
);

CREATE INDEX ledger_entry_account_time ON ledger_entry (account_id, created_at);
