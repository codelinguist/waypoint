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
- Unmet criteria: Criterion 3 and PD-001's monetary rounding convention are not satisfied; see R1 below.
- Returned work: Correct premature monthly-rate rounding and add domain/HTTP regression evidence for R1.
- Follow-up opportunities: Inflation-adjusted returns, fees, taxes, variable contributions, and stochastic projections.
- Accepted or returned by Product Owner Agent: Codex (returned for correction).
- Accepted or returned at: 2026-09-06.

## Review findings — 2026-09-06

Reviewed PR #20 using `gh pr diff 20`, head
`1a74d1bd1deae4057cc67662a93b751a415ce457`, against this brief and the
collaboration workflow. Context was limited to the four requested files and
the actual PR diff; no implementation conversation was used. Backend-only;
no feature visual-review file exists.

### R1 — BLOCKING — ACCEPTED — Premature rate rounding changes monthly growth

- Evidence: `backend/src/main/java/com/waypoint/planning/futurevalue/FutureValueCalculator.java:66`
  rounds `annualRatePercentage / 1200` to 12 places before line 73 multiplies
  it by the opening balance. With USD, principal `6.00`, contribution `0`,
  annual percentage `1.00`, and one month, the submitted calculator returns
  growth `0.00` and ending value `6.00`. Exact growth before monetary rounding
  is `6.00 * 1.00 / 1200 = 0.005`; the specified HALF_UP convention requires
  growth `0.01` and ending value `6.01`.
- Verification: Extracted the calculator and its three domain types directly
  from the fetched diff into a disposable temporary directory, removed only
  the package declarations and Spring service annotation/import, and compiled
  and ran a Java harness. Output: `Actual growth=0.00, ending=6.00`;
  independent BigDecimal multiply-then-divide at scale 2 with HALF_UP:
  `Expected growth=0.01`. No application source was edited.
- Impact: Valid small inputs produce incorrect financial results. Rows still
  add up internally, but the growth term does not follow PD-001 or criterion
  3's rounded-growth/no-hidden-precision requirement. The implementation log's
  claim that 12-place rate rounding avoids drift is contradicted by this case.
- Acceptance condition: Compute each month's growth from the exact product
  of opening balance and annual percentage divided by 1200, rounding only
  the monetary result HALF_UP to two decimals (or an equivalent exact method).
  Add domain and HTTP regression coverage for the above half-cent case and
  a non-terminating rate with a large accepted balance; assert independently
  derived growth as well as row/totals reconciliation. Correct the feature-local
  precision explanation and rerun `./verify.sh` with a green required check.
- Disposition: ACCEPTED as a required fix; unresolved. No change to the
  approved financial model or household preference is needed.

### Acceptance assessment

Feature acceptance remains `PENDING`; returned to Claude Code for R1.
No other BLOCKING, RECOMMENDED, or OPTIONAL findings were identified.

- Criteria 1–2: Worked-example, zero-rate, and zero-money domain/MVC assertions
  match the brief; the diff's API and implementation log record manual flows.
- Criterion 3: Unmet for the rounded-growth convention, as independently
  reproduced in R1; summation identities alone pass but do not prove correct growth.
- Criterion 4: Domain sign/digit/range checks, DTO constraints, and whole-number
  deserialization reject the specified invalid inputs; focused tests cover
  fractional and overflowing months before narrowing.
- Criterion 5: ASCII currency validation and `Locale.ROOT` normalization are
  present, with a Turkish-locale regression; no financial-value logging is
  introduced in the diff.
- Criterion 6: Pure deterministic loop, explicit inputs and ordered immutable
  schedule; no clock, persistence, household lookup, or sibling dependency.
- Criterion 7: All 14 changed files are in the exclusive code/test/prose paths.
- Criterion 8: Implementation evidence reports local `./verify.sh` success
  (389 tests) and manually exercised synthetic flows. Independently checked
  required [verify run](https://github.com/codelinguist/waypoint/actions/runs/33983493676/job/101352773582)
  is SUCCESS for the reviewed head. The full suite was not rerun during this
  review; its current coverage misses R1.

System evolution: No shared-rule change is required for this fix; PD-001
already defines the correct convention. The feature regression tests should
check exact rounding boundaries, not only reconciliation, to prevent recurrence.
