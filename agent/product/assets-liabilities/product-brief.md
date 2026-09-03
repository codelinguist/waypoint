# Product Brief: Household Assets and Liabilities

## Status

`ACCEPTED`

## Ownership

- Product Owner Agent: Waypoint Product Owner Agent
- User(s): Ralph and his wife
- Created at: 2026-09-03
- Last updated at: 2026-09-03

## User input

- Problem as presented: With the household foundation accepted, Waypoint needs
  canonical records of what the household owns and owes before it can explain
  financial position or calculate net worth.
- Examples or evidence supplied: User-zero assets include cash, properties, and
  startup equity; liabilities include a credit card, mortgage, personal loan,
  and business loan. Values may be approximate and assets vary in liquidity.
- Corrections and constraints supplied: Use Docker locally; preserve the Java
  modular monolith and PostgreSQL boundary; never seed documented household data
  or treat an estimate as a confirmed fact.
- Explicit preferences: Use clear, measurable rules and distinguish usable
  financial resources from illiquid wealth without adding bookkeeping burden.

## Product framing

- Underlying problem: Waypoint cannot yet represent the household balance sheet
  in canonical state. Later snapshots, goals, and scenarios need dated asset
  values and liability balances rather than conversation memory.
- Primary user: Ralph and his wife in one trusted, private household.
- Desired outcome: A trusted caller can record and retrieve household assets and
  liabilities with explicit values, dates, currencies, categories, and
  liquidity treatment.
- Success measure: Records persist in PostgreSQL, remain household-scoped,
  reject invalid values, and are retrieved predictably through tested APIs.
- Priority and rationale: This is the next roadmap increment and supplies the
  minimum balance-sheet state needed by future snapshots and calculations.

## Knowledge classification

### Confirmed inputs

- Asset and liability records belong to a household.
- Assets require values, valuation dates, and liquidity classification.
- Liabilities require outstanding balances and balance dates.
- Monetary values must use deterministic decimal representation.
- Illiquid assets must not become emergency liquidity merely because they have
  an estimated value.
- The household may hold values in more than one currency.
- Research context does not authorize canonical financial-data writes.
- Material values require persisted provenance; Task 002 accepts only explicit
  manual API entry.

### Product assumptions to validate

- Initial asset types: `CASH`, `BANK_ACCOUNT`, `PROPERTY`, `INVESTMENT`,
  `BUSINESS_OWNERSHIP`, and `OTHER`.
- Initial liability types: `CREDIT_CARD`, `MORTGAGE`, `PERSONAL_LOAN`,
  `BUSINESS_LOAN`, and `OTHER`.
- Initial liquidity classifications: `LIQUID`, `RESTRICTED`, and `ILLIQUID`.
  The caller supplies classification; it is not inferred from asset type.
- Keeping both estimated and conservative planning value on an asset lets an
  uncertain owned asset retain context while contributing zero to conservative
  planning.
- Create-and-read behavior is sufficient now. Value changes must not silently
  overwrite history before history semantics are deliberately defined.
- A server-assigned `MANUAL_ENTRY` source type is sufficient provenance for this
  API-only increment; the later facts/assumptions phase can extend the model.

### Open questions

- Whether value changes become immutable valuation records, financial events,
  or part of snapshots remains to be decided before update endpoints.
- Person-level ownership is not yet required.
- Exchange-rate sources and conversion rules are undefined.
- Interest, payments, payoff, and amortization belong to a later increment.

## Scope

### In scope

- Household-owned `Asset` and `Liability` records.
- Asset name, type, estimated value, conservative planning value, currency,
  valuation date, and liquidity classification.
- Liability name, type, outstanding balance, currency, and balance-as-of date.
- Server-assigned `MANUAL_ENTRY` provenance for asset values and liability
  balances.
- UUIDs, creation/update timestamps, Flyway migrations, JPA persistence,
  validated create/retrieve/list APIs, structured errors, automated tests,
  documentation, and Docker Compose verification.
- Household-scoped lookup and deterministic creation ordering.

### Out of scope

- Seeding Ralph's documented financial records.
- Update, delete, archive, transfer, valuation history, or balance history.
- Net-worth totals, conversion, rates, gains/losses, or projections.
- Interest, payment schedules, amortization, or payoff calculations.
- Account numbers, addresses, lenders, documents, or extra sensitive data.
- Person ownership, authentication, UI, AI, or external integrations.

## User flow or behavior

1. A trusted caller selects an existing household.
2. The caller records an asset with its name, controlled type, non-negative
   estimated/planning values, currency, valuation date, and explicit liquidity.
3. Waypoint verifies that planning value does not exceed estimated value,
   persists the asset, and returns it.
4. The caller retrieves one household-scoped asset or lists the household's
   assets in deterministic creation order.
5. The caller similarly records and retrieves a liability with a non-negative
   balance, currency, type, and balance date.
6. Invalid input, unknown households, and cross-household record IDs return
   clear errors without partial writes or data leakage.

## Acceptance criteria

- [x] Flyway upgrades a Task 001 database and builds the complete schema on an
  empty PostgreSQL database without manual SQL.
- [x] Creating a valid asset returns its UUID, household ID, normalized
  enum/currency values, exact estimated/planning decimals, valuation date,
  liquidity, `MANUAL_ENTRY` source type, and timestamps; it remains retrievable.
- [x] Asset values accept zero, reject negatives, and reject planning value
  greater than estimated value.
- [x] Creating a valid liability returns its UUID, household ID, normalized
  enum/currency values, exact outstanding balance, balance date, and timestamps;
  its `MANUAL_ENTRY` source type is persisted and it remains retrievable.
- [x] Liability balance accepts zero and rejects negative values.
- [x] Names are non-blank, currencies contain exactly three letters, enums are
  recognized, and monetary dates are not in the future.
- [x] Unknown households return not found for create/list operations and never
  create orphan records.
- [x] Retrieving a record through another household ID returns not found without
  disclosing the record.
- [x] New households return empty collections; populated collections contain
  only that household's records in creation-time/UUID order.
- [x] Duplicate names are permitted because UUIDs are identities.
- [x] No documented household financial values are seeded.
- [x] Clients cannot submit or claim unsupported provenance; Task 002 records
  every created monetary record as `MANUAL_ENTRY`.
- [x] Tests cover success, exact decimals, validation, unknown households,
  isolation, ordering, duplicates, and Flyway/PostgreSQL behavior.
- [x] README documents representative API requests and retains
  `docker compose up --build` as the primary startup path.
- [x] No aggregation, conversion, update behavior, or other out-of-scope feature
  is introduced.
- [x] `agent/implementation-log.md` records evidence and open questions.

## Risks and safeguards

- Financial-data boundary: Persist values only from explicit API requests.
  Estimated and planning values remain separately named.
- Privacy: Store only approved descriptive and financial fields; do not log
  request bodies or add account numbers, addresses, lenders, or documents.
- Accessibility: No UI is in scope; error payloads must remain clear.
- Misuse: Prevent cross-household disclosure, floating-point rounding, negative
  values, inferred liquidity, and silent overwrites through scoped queries,
  `BigDecimal`/`NUMERIC(19,2)`, explicit classification, validation, and no
  update API.

## Product decisions

### PD-001 — Preserve estimated and planning values separately

- Decision: Each asset records non-negative `estimatedValue` and
  `planningValue`; planning value cannot exceed estimated value.
- Evidence: User-zero includes illiquid property and startup equity that should
  be treated conservatively; product principles require explicit uncertainty.
- Alternatives considered: Store one value; defer planning treatment.
- Rationale: Separate values preserve context without forcing uncertain wealth
  into future affordability calculations.
- User input required: `NO`

### PD-002 — Liquidity is explicit and independent of asset type

- Decision: Use caller-supplied `LIQUID`, `RESTRICTED`, or `ILLIQUID`.
- Evidence: Property must not count as emergency liquidity, and type alone
  cannot reliably determine accessibility.
- Alternatives considered: Infer liquidity; use a boolean; postpone it.
- Rationale: A small explicit enum is auditable and supports future runway math.
- User input required: `NO`

### PD-003 — Keep original currencies and defer aggregation

- Decision: Store a three-letter currency per record; calculate no cross-currency
  totals in Task 002.
- Evidence: The household uses PHP and USD, with no accepted FX source or policy.
- Alternatives considered: Force base currency; fetch rates; sum currencies.
- Rationale: Original amounts are correct and reversible; unsupported conversion
  would create misleading financial state.
- User input required: `NO`

### PD-004 — Do not overwrite monetary state yet

- Decision: Support create and read only until value-history semantics exist.
- Evidence: The product requires auditable history and has later snapshot and
  provenance phases.
- Alternatives considered: General update endpoints; full history now.
- Rationale: This proves the model without losing history or expanding scope.
- User input required: `NO`

### PD-005 — Scope records through household routes

- Decision: Every endpoint includes household ID; a record requested through
  another household returns not found.
- Evidence: Household is the planning boundary established in Task 001.
- Alternatives considered: Global record routes; forbidden responses before
  authentication exists.
- Rationale: Scoped routes prevent leakage and preserve consistent semantics.
- User input required: `NO`

### PD-006 — Record narrow provenance now

- Decision: Persist a server-assigned `sourceType` of `MANUAL_ENTRY` on every
  Task 002 asset and liability; do not accept source type from the request.
- Evidence: Material financial values require provenance, while this increment
  supports only explicit trusted-local API entry and Phase 6 owns the broader
  provenance model.
- Alternatives considered: Omit provenance; expose future source types now;
  implement the complete Fact/Assumption model early.
- Rationale: This records truthful origin without allowing clients to claim an
  unsupported import/calculation source or expanding Task 002 into Phase 6.
- User input required: `NO`

## Delivery handoff

- Current task: `agent/current-task.md` — Task 002, Record Household Assets and
  Liabilities
- Design brief: Not applicable; no UI is in scope.
- Implementation owner: Claude Code
- Review evidence: PR `task/002-assets-liabilities`. Findings F-001 through
  F-004 from the 2026-09-03 Product Owner review addressed (see
  `agent/implementation-log.md`); corrected test command, run from the
  repository root: `docker run --rm -v "$(pwd)/backend":/workspace -v
  /var/run/docker.sock:/var/run/docker.sock -e
  TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e
  TESTCONTAINERS_RYUK_DISABLED=true -w /workspace
  maven:3.9-eclipse-temurin-21 mvn -B test` — 62 tests passing under Java
  21/Docker (`AssetServiceTest`, `LiabilityServiceTest`,
  `AssetLiabilityApiIntegrationTest`, plus prior Task 001 suites); Flyway V2
  applied cleanly both on an empty schema (Testcontainers) and as a genuine
  upgrade of the persisted Task 001 volume (`docker compose up --build`,
  "Current version of schema public: 1" -> "Migrating ... to version 2"); live
  smoke checks for asset/liability create, get, list, zero-value acceptance,
  negative-value rejection, planning-value-exceeds-estimated rejection,
  future-date rejection, unknown-household 404, and cross-household 404
  without disclosure.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: Product Owner fix-round review of PR #1 on 2026-09-03
  verified commit `1d86533` against all four returned findings and every Task
  002 acceptance criterion. Independent execution of the exact corrected Java
  21 command documented in the PR passed 62 tests with zero failures or errors,
  including 32 PostgreSQL-backed asset/liability integration tests. The logs
  confirm Flyway applies V1 and V2 to an empty PostgreSQL schema. Diff review
  confirmed `NUMERIC(19,2)`-safe request validation, structured rejection of
  unsupported provenance fields, symmetric asset/liability validation and
  ordering coverage, household isolation, server-assigned `MANUAL_ENTRY`, and
  no expansion into aggregation, updates, conversion, UI, or seeded financial
  data.
- Unmet criteria: None.
- Returned work: F-001 through F-004 are resolved and independently verified in
  commit `1d86533`; no further fix round is required.
- Follow-up opportunities: Define immutable value history before updates; use
  accepted state in the later financial-snapshot increment.
- Accepted or returned by Product Owner Agent: Codex
- Accepted or returned at: 2026-09-03
