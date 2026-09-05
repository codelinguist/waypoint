# Product Brief: Future-Value Calculator

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Problem as presented: Frame three tasks for another parallel batch.
- Examples or evidence supplied: Phase 7 calls for simple future-value calculations; current calculators cover runway, debt amortization, and goal contributions but not compound growth.
- Corrections and constraints supplied: Use isolated worktrees and concurrent execution.
- Explicit preferences: Three implementation-ready tasks.

## Product framing

- Underlying problem: The household cannot deterministically inspect how an explicit starting amount and equal monthly contributions grow under a stated return assumption.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Calculate an auditable monthly compound-growth projection from explicit temporary inputs.
- Success measure: Identical inputs produce identical reconciled results without persistence, household lookup, or an LLM.
- Priority and rationale: Completes another named Phase 7 primitive and is independent enough to ship safely in parallel.

## Knowledge classification

### Confirmed inputs

- Financial arithmetic must be deterministic and testable.
- Expected return is an assumption, not a fact or guaranteed outcome.

### Product assumptions to validate

- Nominal annual rate divided by 12, monthly compounding, and end-of-month contributions are a useful first convention when stated explicitly.

### Open questions

- Inflation, taxes, fees, irregular contributions, and probabilistic returns are future models.

## Scope

### In scope

- Required currency, non-negative starting principal and monthly contribution, explicit nominal annual percentage rate, and whole projection months from 1 through 1200.
- Convert the annual percentage rate to a monthly decimal rate by dividing by 12 and 100; apply growth monthly, round monetary balances and growth to currency scale using `HALF_UP`, then add the end-of-month contribution.
- Return ending value, total contributed principal, total growth, model conventions, and a deterministic ordered monthly schedule.
- Support zero rate, zero starting principal, and zero contribution; reject a case where both starting principal and contribution are zero only if the brief's result remains well-defined—prefer returning a valid zero projection.
- Enforce bounded decimal inputs without silently rounding malformed inputs; normalize a three-letter currency using `Locale.ROOT`.
- Expose `POST /api/planning/future-value`, pure domain logic, HTTP mapping, dedicated tests, and feature-local API documentation.

### Out of scope

- Persistence, household/entity lookup, inflation, taxes, fees, withdrawals, contribution timing choices, variable returns, Monte Carlo analysis, recommendations, UI, or scenario integration.

## User flow or behavior

1. A caller supplies explicit temporary inputs and a currency.
2. The API validates and runs the pure calculation.
3. The response states its compounding/contribution conventions and returns reconciled totals plus monthly rows.
4. Nothing is saved or represented as a promised return.

## Acceptance criteria

- [ ] Starting 1000.00, contribution 100.00, annual rate 12.00%, and 2 months follows monthly sequencing exactly: month 1 interest 10.00 and close 1110.00; month 2 interest 11.10 and close 1221.10; total principal 1200.00 and growth 21.10.
- [ ] Zero rate produces ending value equal to starting principal plus all contributions; all-zero money inputs produce a valid zero schedule.
- [ ] Each row reconciles opening balance + rounded growth + contribution = closing balance, and final totals reconcile with the last row without hidden precision.
- [ ] Months reject missing, fractional, zero, negative, overflow, and values above 1200 before narrowing; decimal inputs reject negatives and excessive scale/precision at both domain and HTTP boundaries.
- [ ] Currency validation and normalization are locale-independent; modeled financial inputs are not logged.
- [ ] Identical requests produce identical responses with no clock-dependent fields, persistence, migration, household lookup, or sibling dependency.
- [ ] All code, tests, docs, and evidence stay within Task 013's exclusive paths.
- [ ] `./verify.sh` passes and the documented synthetic primary flow is exercised.

## Risks and safeguards

- Financial-data or household-approval boundary: The rate and contributions are temporary assumptions; results are not forecasts, guarantees, recommendations, or approved allocations.
- Privacy or sensitive-data considerations: Stateless private endpoint; use synthetic evidence and do not log request values.
- Accessibility considerations: Backend-only; conventions and results must be understandable as text.
- Failure or misuse risks: Clearly name nominal rate, monthly compounding, end-of-month contribution, rounding, and omitted real-world factors.

## Product decisions

### PD-001 — Fix one explicit compounding convention

- Decision: Nominal annual percentage divided by 12, monthly compounding, end-of-month contribution, monetary `HALF_UP` rounding per month.
- Evidence: A deterministic calculator needs one reproducible convention; exposing multiple timing models would expand scope.
- Alternatives considered: Effective annual rate; beginning-of-month contribution; unrounded internal accrual.
- Rationale: Easy to audit row by row and clearly document.
- User input required: `NO` — this is a reversible calculator convention, not a household return assumption.

### PD-002 — Isolate the parallel implementation

- Decision: Exclusive ownership of `com.waypoint.planning.futurevalue/**`, matching tests, `agent/product/future-value-calculator/**`, and its lifecycle file. No migration or shared-file edits.
- Evidence: The capability is stateless and additive.
- Alternatives considered: Shared calculator framework.
- Rationale: Avoids premature abstraction and parallel merge conflicts.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/013-future-value-calculator.md`
- Design brief, if applicable: Not applicable; backend-only.
- Implementation owner: Claude Code in `task/013-future-value-calculator`.
- Review evidence: Pending. Use `agent/product/future-value-calculator/implementation-log.md` and `api.md`; shared docs are deferred until after the batch.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence:
- Unmet criteria: Pending implementation.
- Returned work:
- Follow-up opportunities: Inflation-adjusted returns, fees, taxes, variable contributions, and stochastic projections.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:

