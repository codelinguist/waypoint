# Product Brief: Household Income and Recurring Obligations

## Status

`READY`

## Ownership

- Product Owner Agent: Waypoint Product Owner Agent
- User(s): Ralph and his wife
- Created at: 2026-09-03
- Last updated at: 2026-09-03

## User input

- Problem as presented: The balance sheet is now recorded, but Waypoint cannot
  represent the household's incoming cash or recurring commitments needed to
  understand runway and future planning.
- Examples or evidence supplied: The household expects two jobs to begin in
  October 2026, with hourly and monthly compensation; known recurring outflow
  includes household expenses, mortgage, and loan payments. The PHP 450,000
  combined monthly receipts figure is a planning assumption, not guaranteed
  net income.
- Corrections and constraints supplied: Preserve the distinction between
  received/confirmed income and expected or variable income. Do not seed
  Ralph's records or calculate a combined cash-flow total before frequency,
  currency, and assumption semantics are deliberately defined.
- Explicit preferences: Favor top-level commitments and clear tradeoffs over
  obsessive transaction tracking.

## Product framing

- Underlying problem: Waypoint has assets and liabilities but no canonical
  representation of recurring cash inflows and outflows. Later snapshots,
  runway calculations, and planning need dated, household-scoped schedules.
- Primary user: Ralph and his wife in one trusted, private household.
- Desired outcome: A trusted caller can record and retrieve income streams and
  recurring obligations with explicit amounts, frequencies, currencies, dates,
  and uncertainty/classification.
- Success measure: Records persist in PostgreSQL, remain household-scoped,
  reject invalid schedule data, and are retrieved predictably through tested
  APIs without silently turning estimates into facts.
- Priority and rationale: This is Phase 3 of the roadmap and supplies the
  minimum cash-flow state needed before snapshots and deterministic planning.

## Knowledge classification

### Confirmed inputs

- Income streams and obligations belong to a household.
- Income may be hourly, monthly, or otherwise frequency-based and may be
  gross, net, or unknown.
- Income and obligations have start/end dates; future starts are valid because
  the household has upcoming employment.
- Monetary values require deterministic decimal representation and a currency.
- Expected or variable income must remain visibly distinct from confirmed
  income; the later facts/assumptions phase owns richer provenance semantics.
- Recurring obligations are meaningful commitments, not a ledger of every
  transaction.

### Product assumptions to validate

- Initial income types: `SALARY`, `HOURLY_CONTRACT`, `BUSINESS_DISTRIBUTION`,
  and `OTHER`.
- Initial obligation types: `HOUSEHOLD_BASELINE`, `MORTGAGE`, `LOAN_PAYMENT`,
  `INSURANCE`, `TUITION`, `TRAVEL_SINKING_FUND`, `DISCRETIONARY`, and `OTHER`.
- Initial frequencies: `HOURLY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, and `ANNUAL`.
- Income certainty classifications: `CONFIRMED`, `EXPECTED`, and `VARIABLE`.
  A caller supplies this classification; it is not inferred from the income
  type or amount.
- Income compensation classification: `GROSS`, `NET`, and `UNKNOWN`.
- A server-assigned `MANUAL_ENTRY` source type is sufficient for this API-only
  increment; the broader provenance and Fact/Assumption model remains Phase 6.
- Create-and-read behavior is sufficient now. Changes to amounts or schedules
  must not silently overwrite history before history semantics are defined.

### Open questions

- Whether hourly rates should support an expected-hours companion field, or
  remain a rate that later planning converts, is deferred to the planning
  engine.
- Whether obligations should distinguish required from discretionary beyond
  their type is deferred until a concrete planning rule needs it.
- Whether dates need day-level versus month-level precision for future
  snapshots is deferred; this task uses ISO calendar dates consistently.
- Aggregation, FX conversion, taxes, and net-cash-flow semantics are undefined
  until a later deterministic calculation task.

## Scope

### In scope

- Household-owned `IncomeStream` records with name, type, amount/rate,
  frequency, currency, gross/net/unknown classification, certainty,
  start date, optional end date, and server-assigned `MANUAL_ENTRY` provenance.
- Household-owned `Obligation` records with name, type, amount, frequency,
  currency, start date, optional end date, and server-assigned
  `MANUAL_ENTRY` provenance.
- UUIDs, creation/update timestamps, Flyway migration, JPA persistence,
  validated create/retrieve/list APIs, structured errors, automated tests,
  documentation, and Docker Compose verification.
- Household-scoped lookup and deterministic creation ordering.

### Out of scope

- Seeding Ralph's documented income or obligations.
- Update, delete, archive, transfer, or schedule-history behavior.
- Cash-flow totals, net income, taxes, FX conversion, annualization, runway,
  forecasting, amortization, or goal calculations.
- Assumption versioning, review dates, supersession, or imported documents.
- Account details, employer details, lenders, addresses, authentication, UI,
  AI, or external integrations.

## User flow or behavior

1. A trusted caller selects an existing household.
2. The caller records an income stream with an explicit amount/rate,
   frequency, currency, compensation classification, certainty, and dates.
3. Waypoint validates the schedule, persists the income stream, and returns it
   with server-assigned provenance and timestamps.
4. The caller retrieves one household-scoped income stream or lists the
   household's income streams in deterministic creation order.
5. The caller similarly records and retrieves a recurring obligation.
6. Invalid input, unknown households, and cross-household record IDs return
   clear errors without partial writes or data leakage.

## Acceptance criteria

- [ ] Flyway upgrades the accepted Task 003 database and builds both new
  tables on an empty PostgreSQL database without manual SQL.
- [ ] Creating a valid income stream returns its UUID, household ID, exact
  decimal amount/rate, normalized enum/currency values, frequency,
  compensation classification, certainty, dates, `MANUAL_ENTRY` provenance,
  and timestamps; it remains retrievable.
- [ ] Creating a valid obligation returns its UUID, household ID, exact
  decimal amount, normalized enum/currency values, frequency, dates,
  `MANUAL_ENTRY` provenance, and timestamps; it remains retrievable.
- [ ] Monetary amounts accept zero, reject negatives, and preserve exact
  `NUMERIC(19,2)`-compatible decimals without silent rounding.
- [ ] Names are non-blank, currencies contain exactly three letters, and all
  controlled enum values are recognized.
- [ ] Future start dates are accepted; an end date cannot precede a start date.
- [ ] Income certainty and gross/net/unknown classification are explicit and
  are returned as stored; no classification is inferred or silently changed.
- [ ] Unknown households return not found for create/list operations and never
  create orphan records.
- [ ] Retrieving a record through another household ID returns not found
  without disclosing the record.
- [ ] New households return empty collections; populated collections contain
  only that household's records in creation-time/UUID order.
- [ ] Duplicate names are permitted because UUIDs are identities.
- [ ] Clients cannot submit or claim unsupported provenance; created records
  use server-assigned `MANUAL_ENTRY`.
- [ ] Tests cover success, exact decimals, validation, future starts,
  date-ordering, unknown households, isolation, ordering, duplicates, and
  Flyway/PostgreSQL behavior.
- [ ] README documents representative API requests and retains
  `./verify.sh` as the canonical verification command.
- [ ] No aggregation, conversion, update behavior, seeded household data, or
  other out-of-scope feature is introduced.
- [ ] `agent/implementation-log.md` records evidence, assumptions, limitations,
  and recommended follow-up work.

## Risks and safeguards

- Financial-data boundary: Persist schedules only from explicit API requests;
  certainty and compensation classifications remain caller-visible.
- Privacy: Store only approved descriptive and monetary fields; do not add
  employer names, account numbers, lender details, or documents.
- Misuse: Prevent cross-household disclosure, floating-point rounding,
  negative amounts, invalid date ranges, and unsupported provenance through
  scoped queries, `BigDecimal`/`NUMERIC(19,2)`, validation, and no update API.
- Planning risk: Do not present an income rate as monthly cash or combine
  currencies until deterministic conversion and frequency semantics exist.

## Product decisions

### PD-001 — Preserve schedule semantics without calculating totals

- Decision: Store amount/rate and an explicit frequency, but do not annualize,
  aggregate, or calculate net cash flow in Task 004.
- Evidence: The household has hourly and monthly income and multiple
  currencies; no accepted conversion, hours, tax, or net-income policy exists.
- Alternatives considered: Convert everything to monthly; accept only monthly
  values; add a planning engine now.
- Rationale: Preserving the caller's stated schedule avoids invented math and
  leaves deterministic normalization to a later planning task.
- User input required: `NO`

### PD-002 — Make income uncertainty explicit

- Decision: Every income stream carries `CONFIRMED`, `EXPECTED`, or `VARIABLE`
  certainty, plus `GROSS`, `NET`, or `UNKNOWN` compensation classification.
- Evidence: The PHP 450,000 combined monthly receipts figure is explicitly a
  planning assumption, while job compensation may differ by stream.
- Alternatives considered: Treat all entries as income facts; infer certainty
  from type; defer uncertainty entirely.
- Rationale: The API can preserve an important distinction now without
  prematurely implementing the complete Phase 6 Fact/Assumption model.
- User input required: `NO`

### PD-003 — Use create/read-only schedules initially

- Decision: Support create and retrieve/list only; do not overwrite an income
  rate or obligation amount before history semantics exist.
- Evidence: The product requires auditable historical state, and snapshots are
  a later roadmap phase.
- Alternatives considered: General update endpoints; immutable versioning now.
- Rationale: A narrow vertical slice avoids silently losing prior financial
  state while delivering useful canonical records.
- User input required: `NO`

## Delivery handoff

- Current task: `agent/current-task.md` — Task 004, Income and Recurring
  Obligations
- Design brief: Not applicable; no UI is in scope.
- Implementation owner: Claude Code
- Review evidence: Pending implementation, canonical verification, CI, and
  Product Owner review.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Not yet implemented.
- Unmet criteria: All acceptance criteria remain pending.
- Returned work: None.
- Follow-up opportunities: Financial snapshots, immutable schedule history,
  and deterministic cash-flow normalization/planning.
- Accepted or returned by Product Owner Agent: Pending
- Accepted or returned at: Pending
