# Product Brief: Financial Data Freshness Review

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Request: Generate five new tasks now.
- Repository evidence: Problem 5 warns that plans become stale; existing assets and liabilities already retain valuedAt and balanceAsOf, so a useful read-only review needs no new assumption schema.
- Constraints: Follow AGENTS.md and the existing isolated, maximum-three-worker pipeline.
- No new household amounts, priorities or financial rules were supplied.

## Product framing

- Underlying problem and evidence: Problem 5 warns that plans become stale; existing assets and liabilities already retain valuedAt and balanceAsOf, so a useful read-only review needs no new assumption schema.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Identify asset valuations and liability balances needing review using an explicit caller-selected age threshold.
- Success measure: The documented operation answers this question from explicit inputs with reproducible results and no canonical writes.
- Priority: Batch order 5 of five; bounded extension of accepted capabilities before broader planning/UI integration.

## Knowledge classification

### Confirmed inputs

- The user requested five tasks. Product context above comes from checked-in vision, problems, user-zero and accepted calculator contracts.
- Tasks 012–014 are marked IN_PROGRESS in this checkout and are not assumed available.

### Product assumptions to validate

- A backend-only operation is a useful next increment; this batch does not yet deliver a household-facing dashboard.
- Modeling inputs are temporary assumptions, never confirmed household facts. Example values below are synthetic test fixtures.

### Open questions

- Household-specific thresholds and financial choices remain caller inputs; none block implementation.
- Broader scenario persistence, UI and automatic use of canonical planning inputs remain future framing work.

## Scope

### In scope

- Endpoint: `GET /api/households/{householdId}/financial-data-freshness?reviewDate=YYYY-MM-DD&maxAgeDays=N`.
- Require explicit reviewDate and whole maxAgeDays in 0..36500; do not use the server date or invent a household review cadence. Read only current assets and liabilities owned by the requested household through established services.
- Return each source record identity, record kind, name, currency, sourceType, source date, signed ageDays = reviewDate - sourceDate and classification. STALE means ageDays > maxAgeDays; CURRENT means 0 <= ageDays <= maxAgeDays; FUTURE_DATED means ageDays < 0.
- Include counts by kind/classification, explicit reviewDate and threshold, and deterministic ordering by record kind then UUID. Return all records, not only stale ones; empty existing households yield an empty list and zero counts.
- Explain that freshness is age relative to the supplied date, not proof that a value is correct or current today. This is a review of present source rows, not reconstruction of historical state at reviewDate.
- Do not copy financial amounts into this metadata-only response. Do not write reminders, change source dates, refresh valuations, capture snapshots or inspect unfinished planning assumptions.

### Out of scope

No notifications, scheduling, freshness scoring, inferred review intervals, assumption supersession, historical reconstruction, source edits, UI, external quotes or financial advice.

## User flow or behavior

1. A trusted caller submits the explicit inputs documented above.
2. The application validates them and performs the bounded calculation or read-only review.
3. The caller receives source/input context, results, and model limitations.
4. No result becomes an approved household decision or changes canonical state.

## Acceptance criteria

- [ ] With reviewDate 2026-09-06 and threshold 30, sources dated 2026-08-07 are CURRENT at 30 days, 2026-08-06 are STALE at 31 days, and 2026-09-07 are FUTURE_DATED at -1 day.
- [ ] Threshold zero marks same-day data CURRENT and earlier dates STALE; leap-year and month/year boundaries use calendar-day arithmetic.
- [ ] Unknown households follow existing 404 semantics. Integration tests prove no other household records leak, correct empty responses, stable ordering/counts and no mutation.
- [ ] Missing/malformed dates, fractional/negative/overflow threshold values and thresholds above 36500 yield established structured 400 errors. Source valuedAt/balanceAsOf is used, not createdAt or updatedAt.
- [ ] Keep calendar classification in deterministic domain logic separate from HTTP and persistence concerns. Validate at domain and HTTP boundaries. Reuse established household ownership and structured error conventions without editing shared handlers.
- [ ] Automated tests cover domain rules and HTTP representation boundaries; identical valid inputs and unchanged source records produce identical results without clock-dependent fields. Use synthetic fixtures and never log submitted financial values.
- [ ] No migrations, persistence or canonical financial mutations. The diff stays inside exclusive ownership paths and reads only the already-merged dependencies explicitly listed below.
- [ ] Run ./verify.sh, exercise documented valid and invalid API flows on isolated test infrastructure, and record commands/results in feature-local api.md and implementation-log.md. No application evidence exists yet; acceptance remains pending.

## Risks and safeguards

- Financial authority: No household facts, reserve targets, allocations or recommendations are approved by this brief.
- Privacy: Synthetic evidence only; do not log financial inputs or touch the shared household database for testing.
- Accessibility: Backend-only; machine-readable statuses and textual conventions must support later accessible presentation.
- Failure/misuse: Unsupported results remain explicitly unavailable, and validation uses established structured 400/404 conventions. Feature-scoped handlers are allowed within ownership; shared handlers are not edited.

## Product decisions

### PD-001 — Deliver a bounded read-only increment

- Decision: Implement only the defined operation with explicit conventions and non-goals.
- Evidence: Problem 5 warns that plans become stale; existing assets and liabilities already retain valuedAt and balanceAsOf, so a useful read-only review needs no new assumption schema.
- Alternatives: A general persisted scenario/plan framework or a broad dashboard.
- Rationale: Produces a testable planning answer without inventing household decisions or adding infrastructure.
- User input required: `NO`; calculator conventions and a metadata review are reversible product choices.

### PD-002 — Remain independent of concurrent work

- Decision: Exclusive ownership of `backend/src/main/java/com/waypoint/review/freshness/**`, `backend/src/test/java/com/waypoint/review/freshness/**`, and `agent/product/financial-data-freshness/**`.
- Dependencies: Read existing AssetService and LiabilityService (and existing household validation) without edits. Do not depend on Task 012 assumptions or any new migration.
- No shared README, roadmap, central log, decision log, workflow, template, build or configuration edits. Consistent with the previous batch, record implementation findings in the feature-local log; consolidate shared prose after the batch.
- Rationale: Each queued task can start from current merged code regardless of sibling completion. Reuse existing deterministic code through direct Java calls, not internal HTTP.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/019-financial-data-freshness.md`
- Design brief: Not applicable; backend-only.
- Implementation owner: Claude Code, fresh conversation in isolated `task/019-financial-data-freshness` branch/worktree.
- Review evidence: Implemented in new `FinancialDataFreshnessCalculator`/
  `FinancialDataFreshnessService`/`FreshnessSourceRecord`/`FreshnessRecord`/
  `FreshnessRecordKind`/`FreshnessClassification`/`FinancialDataFreshnessResult`/
  `InvalidFreshnessReviewInputException` (domain, package
  `com.waypoint.review.freshness`), `FinancialDataFreshnessController`
  (`GET /api/households/{householdId}/financial-data-freshness`, package
  `com.waypoint.review.freshness.web`), and matching response DTOs — entirely
  additive files inside the exclusive `review/freshness` ownership boundary;
  no existing file was modified. `FinancialDataFreshnessService` reads
  household state only through the existing, unmodified `AssetService` and
  `LiabilityService`. The controller's `InvalidFreshnessReviewInputException`
  handler is declared directly on the controller (not added to the shared
  `ApiExceptionHandler`), so it cannot catch a sibling controller's errors,
  per PD-002. See `agent/product/financial-data-freshness/implementation-log.md`
  for full detail and `agent/product/financial-data-freshness/api.md` for the
  request/response reference.
- Local `./verify.sh`: 427 tests, 0 failures (35 new: 18 domain +
  17 HTTP, in `FinancialDataFreshnessCalculatorTest` and
  `FinancialDataFreshnessApiIntegrationTest`).
- Manual verification: packaged app run locally against a disposable,
  throwaway Postgres container; curl-exercised the exact acceptance-criteria
  boundary example (2026-09-06/30 → CURRENT at 30, STALE at 31),
  `FUTURE_DATED`, unknown household (404), missing/malformed `reviewDate`,
  fractional/negative/overflow/above-bound `maxAgeDays`, and an empty
  household — all nine matched documented behavior exactly. See
  implementation-log.md for full output.
- Delivery gates: Task PR, local ./verify.sh, green required CI verify, and independent Product Owner acceptance under agent/collaboration-workflow.md.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Implementation complete; awaiting independent Product Owner Agent review of the PR diff and evidence above.
- Unmet criteria: None known; pending independent verification.
- Returned work: None.
- Follow-up opportunities: Household-facing integration after these bounded APIs are accepted; do not expand this task during implementation.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:
