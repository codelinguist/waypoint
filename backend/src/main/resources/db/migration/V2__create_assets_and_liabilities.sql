CREATE TABLE assets (
    id               UUID PRIMARY KEY,
    household_id     UUID NOT NULL REFERENCES households (id),
    name             VARCHAR(255) NOT NULL,
    asset_type       VARCHAR(32) NOT NULL,
    estimated_value  NUMERIC(19,2) NOT NULL,
    planning_value   NUMERIC(19,2) NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    valued_at        DATE NOT NULL,
    liquidity        VARCHAR(16) NOT NULL,
    source_type      VARCHAR(16) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_assets_estimated_value_nonnegative CHECK (estimated_value >= 0),
    CONSTRAINT chk_assets_planning_value_nonnegative CHECK (planning_value >= 0),
    CONSTRAINT chk_assets_planning_not_exceeding_estimated CHECK (planning_value <= estimated_value)
);

CREATE INDEX idx_assets_household_id ON assets (household_id);

CREATE TABLE liabilities (
    id                   UUID PRIMARY KEY,
    household_id         UUID NOT NULL REFERENCES households (id),
    name                 VARCHAR(255) NOT NULL,
    liability_type       VARCHAR(32) NOT NULL,
    outstanding_balance  NUMERIC(19,2) NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    balance_as_of        DATE NOT NULL,
    source_type          VARCHAR(16) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_liabilities_outstanding_balance_nonnegative CHECK (outstanding_balance >= 0)
);

CREATE INDEX idx_liabilities_household_id ON liabilities (household_id);
