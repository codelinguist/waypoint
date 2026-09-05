CREATE TABLE financial_goals (
    id              UUID PRIMARY KEY,
    household_id    UUID NOT NULL REFERENCES households (id),
    name            VARCHAR(255) NOT NULL,
    target_amount   NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    target_date     DATE NOT NULL,
    priority        INTEGER NOT NULL,
    current_amount  NUMERIC(19,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_financial_goals_target_amount_positive CHECK (target_amount > 0),
    CONSTRAINT chk_financial_goals_current_amount_nonnegative CHECK (current_amount >= 0),
    CONSTRAINT chk_financial_goals_priority_positive CHECK (priority > 0)
);

CREATE INDEX idx_financial_goals_household_id ON financial_goals (household_id);
