ALTER TABLE liabilities
    ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE liability_balance_history (
    id                     UUID PRIMARY KEY,
    liability_id           UUID NOT NULL REFERENCES liabilities (id),
    currency               VARCHAR(3) NOT NULL,
    previous_balance       NUMERIC(19,2) NOT NULL,
    previous_balance_as_of DATE NOT NULL,
    previous_source_type   VARCHAR(16) NOT NULL,
    new_balance            NUMERIC(19,2) NOT NULL,
    new_balance_as_of      DATE NOT NULL,
    new_source_type        VARCHAR(16) NOT NULL,
    reason                 VARCHAR(500) NOT NULL,
    revision               BIGINT NOT NULL,
    recorded_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_liability_balance_history_previous_balance_nonnegative CHECK (previous_balance >= 0),
    CONSTRAINT chk_liability_balance_history_new_balance_nonnegative CHECK (new_balance >= 0),
    CONSTRAINT uq_liability_balance_history_liability_revision UNIQUE (liability_id, revision)
);

CREATE INDEX idx_liability_balance_history_liability_id ON liability_balance_history (liability_id);
