CREATE TABLE financial_snapshots (
    id            UUID PRIMARY KEY,
    household_id  UUID NOT NULL REFERENCES households (id),
    as_of_date    DATE NOT NULL,
    captured_at   TIMESTAMPTZ NOT NULL,
    source_type   VARCHAR(16) NOT NULL
);

CREATE INDEX idx_financial_snapshots_household_id ON financial_snapshots (household_id);

CREATE TABLE snapshot_asset_line_items (
    id              UUID PRIMARY KEY,
    snapshot_id     UUID NOT NULL REFERENCES financial_snapshots (id),
    source_asset_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    asset_type      VARCHAR(32) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    source_date     DATE NOT NULL,
    value           NUMERIC(19,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_snapshot_asset_line_items_value_nonnegative CHECK (value >= 0)
);

CREATE INDEX idx_snapshot_asset_line_items_snapshot_id ON snapshot_asset_line_items (snapshot_id);

CREATE TABLE snapshot_liability_line_items (
    id                  UUID PRIMARY KEY,
    snapshot_id         UUID NOT NULL REFERENCES financial_snapshots (id),
    source_liability_id UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    liability_type      VARCHAR(32) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    source_date         DATE NOT NULL,
    value               NUMERIC(19,2) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_snapshot_liability_line_items_value_nonnegative CHECK (value >= 0)
);

CREATE INDEX idx_snapshot_liability_line_items_snapshot_id ON snapshot_liability_line_items (snapshot_id);
