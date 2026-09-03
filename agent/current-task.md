# Current Task

## Task 002 — Record Household Assets and Liabilities

### Goal

Add the first canonical balance-sheet records so a household can explicitly
record and retrieve what it owns and owes.

Detailed requirements and acceptance criteria:

- `agent/product/assets-liabilities/product-brief.md`

### Outcome

A trusted local caller can create, retrieve, and list household assets and
liabilities; preserve exact dated values in original currencies; distinguish
estimated from conservative planning value; and classify liquidity explicitly.

### Required domain fields

Asset:

- UUID id and household_id
- name
- asset_type: `CASH`, `BANK_ACCOUNT`, `PROPERTY`, `INVESTMENT`,
  `BUSINESS_OWNERSHIP`, or `OTHER`
- estimated_value and planning_value as non-negative fixed-precision decimals;
  planning value must not exceed estimated value
- normalized three-letter currency
- non-future valued_at date
- liquidity: `LIQUID`, `RESTRICTED`, or `ILLIQUID`
- source_type: server-assigned `MANUAL_ENTRY`
- created_at and updated_at

Liability:

- UUID id and household_id
- name
- liability_type: `CREDIT_CARD`, `MORTGAGE`, `PERSONAL_LOAN`, `BUSINESS_LOAN`,
  or `OTHER`
- outstanding_balance as a non-negative fixed-precision decimal
- normalized three-letter currency
- non-future balance_as_of date
- source_type: server-assigned `MANUAL_ENTRY`
- created_at and updated_at

Use `BigDecimal` in Java and `NUMERIC(19,2)` in PostgreSQL.

### Required API

- `POST /api/households/{householdId}/assets`
- `GET /api/households/{householdId}/assets`
- `GET /api/households/{householdId}/assets/{assetId}`
- `POST /api/households/{householdId}/liabilities`
- `GET /api/households/{householdId}/liabilities`
- `GET /api/households/{householdId}/liabilities/{liabilityId}`

All lookups are household-scoped. Unknown households and records belonging to a
different household return structured not-found responses. Lists sort by
creation time ascending, then UUID ascending. Duplicate names are permitted.

### Implementation requirements

1. Add Flyway migration(s) with foreign keys, constraints, and scoped indexes.
2. Add JPA models, repositories, services, validated requests, and explicit
   responses within the modular monolith.
3. Keep validation and business rules outside controllers.
4. Extend structured errors for invalid enums and missing records.
5. Add unit and PostgreSQL/Testcontainers integration tests for the brief.
6. Verify migration from Task 001 and startup on an empty database.
7. Verify through the existing Docker Compose environment.
8. Document representative requests in `README.md`.
9. Update `agent/implementation-log.md` with evidence.

### Constraints

Do not implement seeded household financial data; update/delete/archive/transfer
endpoints; value history; aggregation or net-worth math; currency conversion;
loan interest, schedules, amortization, payoff, or projections; person-level
ownership; authentication; UI; AI; external integrations; or new services.

Liquidity and planning value are explicit caller inputs and must not be inferred
from asset type. The API must assign `MANUAL_ENTRY`; clients cannot claim an
unsupported import or calculation source. Do not log request bodies containing
financial values.

### Definition of Done

- every acceptance criterion in the linked product brief is satisfied
- all tests pass under Java 21/Docker
- upgraded and clean-start Flyway paths are verified
- `docker compose up --build` produces healthy app and PostgreSQL services
- live smoke tests cover create, retrieve, list, validation, not-found, and
  household isolation
- no real household financial records are seeded
- README and `agent/implementation-log.md` are updated
- concrete evidence is ready for independent Product Owner acceptance
