CREATE TABLE planning_assumptions (
    id                UUID PRIMARY KEY,
    household_id      UUID NOT NULL REFERENCES households (id),
    name              VARCHAR(255) NOT NULL,
    value             VARCHAR(1000) NOT NULL,
    value_type        VARCHAR(100) NOT NULL,
    notes             VARCHAR(2000),
    effective_from    DATE NOT NULL,
    effective_until   DATE,
    review_date       DATE NOT NULL,
    source_type       VARCHAR(16) NOT NULL,
    superseded_by_id  UUID REFERENCES planning_assumptions (id),
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_planning_assumptions_effective_window
        CHECK (effective_until IS NULL OR effective_until >= effective_from)
);

CREATE INDEX idx_planning_assumptions_household_id ON planning_assumptions (household_id);
CREATE INDEX idx_planning_assumptions_superseded_by_id ON planning_assumptions (superseded_by_id);
