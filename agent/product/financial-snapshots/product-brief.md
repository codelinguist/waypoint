# Product Brief: Historical Financial Position Snapshots

## Status

`READY`

## Ownership

- Product Owner Agent: Waypoint Product Owner Agent
- User(s): Ralph and his wife
- Created at: 2026-09-04
- Last updated at: 2026-09-04

## User input

- Problem as presented: The household now has canonical balance-sheet records,
  but Waypoint cannot preserve what the financial position looked like at a
  particular point in time.
- Examples or evidence supplied: The product must eventually answer what net
  worth was then and what changed since a prior review.
- Corrections and constraints supplied: Do not seed household data; preserve
  exact decimals, household isolation, and the modular-monolith architecture;
  do not invent FX, cash-flow, or valuation-history rules.
- Explicit preferences: Favor auditable point-in-time records and measurable
  tradeoffs over broad financial dashboards.

## Product framing

- Underlying problem: Current Asset and Liability rows are not a durable
  historical record. A later value change or data correction would otherwise
  make it impossible to compare household financial position over time.
- Primary user: Ralph and his wife in one trusted, private household.
- Desired outcome: A trusted caller can create an immutable snapshot of the
  household's known balance-sheet position and retrieve it later, including
  deterministic net-worth totals within each original currency.
- Success measure: Snapshots persist in PostgreSQL, contain auditable captured
  line items and calculation metadata, remain household-scoped, and return
  stable exact-decimal results through tested APIs.
- Priority and rationale: This is the smallest useful Phase 4 increment and
  establishes historical state before goals, scenarios, or dashboards.

## Knowledge classification

### Confirmed inputs

- Assets and liabilities are already canonical household-scoped records.
- Asset planning values and liability balances use exact decimal storage and
  retain their original currencies.
- A snapshot must not mutate current assets or liabilities.
- Cross-currency totals are not meaningful without an accepted FX policy.

### Product assumptions to validate

- Snapshot creation is explicit and captures the records known at request time.
- The caller supplies a calendar `asOfDate`; the server also records the
  actual capture timestamp so the two concepts are not conflated.
- Assets with `valuedAt` on or before `asOfDate` and liabilities with
  `balanceAsOf` on or before `asOfDate` are eligible for capture.
- The snapshot stores copied line-item values and source dates rather than
  live references, so later source edits cannot rewrite history.
- Net worth is calculated per currency as summed asset planning value minus
  summed liability balance; no cross-currency total is returned.

### Open questions

- Whether future snapshots should include income and obligation schedules is
  deferred until frequency normalization and schedule-history semantics exist.
- Whether asset/liability value changes become immutable valuation records or
  financial events is deferred to a later provenance/history task.
- Whether snapshots should support labels, notes, deletion, or comparison
  endpoints is deferred.

## Scope

### In scope

- Household-owned immutable `FinancialSnapshot` records with UUID,
  `asOfDate`, capture timestamp, and server-assigned manual provenance.
- Copied asset and liability snapshot line items containing source identity,
  name, type, currency, source date, and the exact value used in the snapshot.
- Deterministic per-currency asset totals, liability totals, and net-worth
  totals using `BigDecimal`/`NUMERIC(19,2)`-compatible values.
- Validated create, retrieve, and list APIs under household-scoped routes.
- Flyway migration, JPA entities/services/controllers, structured errors,
  unit tests, PostgreSQL/Testcontainers tests, README, and implementation log.

### Out of scope

- Income streams, recurring obligations, cash-flow totals, annualization,
  frequency conversion, taxes, runway, forecasting, or goals.
- FX conversion or a combined net worth across currencies.
- Updating, deleting, archiving, or recomputing a snapshot.
- Updating source assets/liabilities, valuation history, events, or imports.
- Seeding Ralph's documented financial data, authentication, UI, AI, or
  external integrations.

## User flow or behavior

1. A trusted caller selects an existing household and supplies an `asOfDate`.
2. Waypoint verifies the household, reads eligible current assets and
   liabilities, calculates exact per-currency totals, and persists copied
   snapshot data in one transaction.
3. The response returns the immutable snapshot, line items, totals, capture
   timestamp, and provenance.
4. The caller retrieves one snapshot or lists snapshots in ascending
   `asOfDate`, then capture-time/UUID order.
5. Unknown households and cross-household snapshot IDs return not found
   without disclosing existence; a household with no eligible records gets an
   empty, zero-total snapshot rather than an error.

## Acceptance criteria

- [ ] Flyway upgrades the accepted Task 004 schema and builds all snapshot
  tables on an empty PostgreSQL database without manual SQL.
- [ ] Creating a snapshot for a known household persists an immutable header
  and copied eligible asset/liability line items in one transaction.
- [ ] Eligibility uses the caller's `asOfDate`; records dated after it are
  excluded, while records dated on it are included.
- [ ] Asset line items use `planningValue`; liability line items use
  `outstandingBalance`; source dates, currencies, names, types, and source
  IDs are retained.
- [ ] Per-currency asset, liability, and net-worth totals are deterministic,
  exact decimals, and never combine different currencies.
- [ ] Zero values and negative-result net worth are represented correctly;
  no floating-point rounding is introduced.
- [ ] Future `asOfDate` values are rejected, and malformed or missing dates
  produce the existing structured validation errors.
- [ ] Snapshots are read-only: there are no update/delete routes, and later
  source-record changes cannot alter an existing snapshot.
- [ ] Unknown households return not found for create/list operations; scoped
  retrieval through another household returns not found without disclosure.
- [ ] New households list no snapshots; duplicate `asOfDate` snapshots are
  permitted because each capture is a distinct historical observation.
- [ ] Provenance is server-assigned `MANUAL_ENTRY`; clients cannot submit a
  different source type.
- [ ] Tests cover empty snapshots, eligibility boundaries, exact decimals,
  multi-currency isolation, ordering, immutability, duplicates, unknown
  households, cross-household access, Flyway, and PostgreSQL persistence.
- [ ] README documents representative snapshot requests and retains
  `./verify.sh` as the canonical verification command.
- [ ] No cash-flow aggregation, FX conversion, source update behavior, seeded
  records, or other out-of-scope feature is introduced.
- [ ] `agent/implementation-log.md` records evidence, assumptions,
  limitations, and recommended follow-up work.

## Risks and safeguards

- Financial-data boundary: A snapshot records the application's known state
  at capture time; it must not imply that an old `asOfDate` reconstructs facts
  that were not historically stored.
- Privacy: Snapshot rows remain household-scoped and copy only existing
  approved fields; no account, lender, address, or document data is added.
- Misuse: Scoped queries, immutable copied rows, exact decimals, explicit
  currency grouping, and server-assigned provenance prevent leakage and
  misleading totals.

## Product decisions

### PD-001 — Capture known balance-sheet state without pretending to reconstruct history

- Decision: `asOfDate` filters eligible source dates, while `capturedAt`
  records when the snapshot was actually generated; copied rows are immutable.
- Evidence: Existing assets/liabilities have current values plus valuation
  dates, but do not yet have value history.
- Alternatives considered: Treat every snapshot as a reconstructed historical
  truth; store only a mutable pointer to current rows; build full valuation
  history first.
- Rationale: This delivers useful historical observations now and makes the
  limitation explicit for later comparison and provenance work.
- User input required: `NO`

### PD-002 — Calculate net worth separately for each original currency

- Decision: Sum planning asset values and liability balances only within the
  same currency; return no cross-currency total.
- Evidence: The household already records PHP and USD and no FX source or
  policy is accepted.
- Alternatives considered: Force base currency; use a live FX provider; omit
  all totals.
- Rationale: Same-currency arithmetic is deterministic and useful without
  inventing conversion assumptions.
- User input required: `NO`

### PD-003 — Keep snapshots immutable and create-only

- Decision: Support create and retrieve/list only; do not update, delete, or
  recompute a persisted snapshot.
- Evidence: Historical state must remain auditable and current source rows do
  not yet have versioned value history.
- Alternatives considered: Recompute on read; general update endpoints;
  delete mistaken captures.
- Rationale: A copied, immutable observation is the safest foundation for
  later comparisons.
- User input required: `NO`

## Delivery handoff

- Current task: `agent/current-task.md` — Task 005, Financial Position
  Snapshots
- Design brief: Not applicable; no UI is in scope.
- Implementation owner: Claude Code
- Review evidence: Pending implementation and PR evidence.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Pending implementation.
- Unmet criteria: All implementation criteria are pending.
- Returned work: None.
- Follow-up opportunities: Immutable source valuation history, income/
  obligation schedule snapshots, deterministic cash-flow normalization, and
  historical comparison endpoints.
- Accepted or returned by Product Owner Agent: Pending
- Accepted or returned at: Pending
