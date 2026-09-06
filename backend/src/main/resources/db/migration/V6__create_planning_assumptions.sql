CREATE TABLE planning_assumptions (
    id                UUID PRIMARY KEY,
    household_id      UUID NOT NULL REFERENCES households (id),
    name              VARCHAR(255) NOT NULL,
    value             VARCHAR(2000) NOT NULL,
    value_type        VARCHAR(100) NOT NULL,
    notes             VARCHAR(2000),
    effective_from    DATE NOT NULL,
    effective_until   DATE,
    review_date       DATE NOT NULL,
    source_type       VARCHAR(16) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    superseded_by_id  UUID REFERENCES planning_assumptions (id),
    CONSTRAINT chk_planning_assumptions_effective_until_not_before_from
        CHECK (effective_until IS NULL OR effective_until >= effective_from),
    CONSTRAINT chk_planning_assumptions_not_self_superseding
        CHECK (superseded_by_id IS NULL OR superseded_by_id <> id),
    CONSTRAINT uq_planning_assumptions_superseded_by UNIQUE (superseded_by_id)
);

CREATE INDEX idx_planning_assumptions_household_id ON planning_assumptions (household_id);
CREATE INDEX idx_planning_assumptions_household_name ON planning_assumptions (household_id, name);
