CREATE TABLE income_streams (
    id                            UUID PRIMARY KEY,
    household_id                  UUID NOT NULL REFERENCES households (id),
    name                          VARCHAR(255) NOT NULL,
    income_type                   VARCHAR(32) NOT NULL,
    amount                        NUMERIC(19,2) NOT NULL,
    frequency                     VARCHAR(16) NOT NULL,
    currency                      VARCHAR(3) NOT NULL,
    compensation_classification   VARCHAR(16) NOT NULL,
    certainty                     VARCHAR(16) NOT NULL,
    start_date                    DATE NOT NULL,
    end_date                      DATE,
    source_type                   VARCHAR(16) NOT NULL,
    created_at                    TIMESTAMPTZ NOT NULL,
    updated_at                    TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_income_streams_amount_nonnegative CHECK (amount >= 0)
);

CREATE INDEX idx_income_streams_household_id ON income_streams (household_id);

CREATE TABLE obligations (
    id                UUID PRIMARY KEY,
    household_id      UUID NOT NULL REFERENCES households (id),
    name              VARCHAR(255) NOT NULL,
    obligation_type   VARCHAR(32) NOT NULL,
    amount            NUMERIC(19,2) NOT NULL,
    frequency         VARCHAR(16) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE,
    source_type       VARCHAR(16) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_obligations_amount_nonnegative CHECK (amount >= 0)
);

CREATE INDEX idx_obligations_household_id ON obligations (household_id);
