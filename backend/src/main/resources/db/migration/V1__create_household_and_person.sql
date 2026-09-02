CREATE TABLE households (
    id             UUID PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    base_currency  VARCHAR(3) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE people (
    id             UUID PRIMARY KEY,
    household_id   UUID NOT NULL REFERENCES households (id),
    name           VARCHAR(255) NOT NULL,
    role           VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_people_household_id ON people (household_id);
