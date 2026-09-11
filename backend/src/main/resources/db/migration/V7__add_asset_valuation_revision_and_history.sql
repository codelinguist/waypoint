ALTER TABLE assets
    ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE asset_valuations (
    id                        UUID PRIMARY KEY,
    asset_id                  UUID NOT NULL REFERENCES assets (id),
    household_id              UUID NOT NULL REFERENCES households (id),
    revision                  BIGINT NOT NULL,
    previous_estimated_value  NUMERIC(19,2) NOT NULL,
    previous_planning_value   NUMERIC(19,2) NOT NULL,
    previous_valued_at        DATE NOT NULL,
    previous_source_type      VARCHAR(16) NOT NULL,
    new_estimated_value       NUMERIC(19,2) NOT NULL,
    new_planning_value        NUMERIC(19,2) NOT NULL,
    new_valued_at             DATE NOT NULL,
    new_source_type           VARCHAR(16) NOT NULL,
    reason                    VARCHAR(500) NOT NULL,
    recorded_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_asset_valuations_asset_revision UNIQUE (asset_id, revision),
    CONSTRAINT chk_asset_valuations_previous_estimated_nonnegative CHECK (previous_estimated_value >= 0),
    CONSTRAINT chk_asset_valuations_previous_planning_nonnegative CHECK (previous_planning_value >= 0),
    CONSTRAINT chk_asset_valuations_previous_planning_not_exceeding_estimated
        CHECK (previous_planning_value <= previous_estimated_value),
    CONSTRAINT chk_asset_valuations_new_estimated_nonnegative CHECK (new_estimated_value >= 0),
    CONSTRAINT chk_asset_valuations_new_planning_nonnegative CHECK (new_planning_value >= 0),
    CONSTRAINT chk_asset_valuations_new_planning_not_exceeding_estimated
        CHECK (new_planning_value <= new_estimated_value)
);

CREATE INDEX idx_asset_valuations_asset_id ON asset_valuations (asset_id);
