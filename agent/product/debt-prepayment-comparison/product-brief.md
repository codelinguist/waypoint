# Product Brief: Debt Prepayment Comparison

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Request: Generate five new tasks now.
- Repository evidence: The vision and architecture name mortgage acceleration analysis; the accepted debt-amortization model provides an auditable constant-rate baseline.
- Constraints: Follow AGENTS.md and the existing isolated, maximum-three-worker pipeline.
- No new household amounts, priorities or financial rules were supplied.

## Product framing

- Underlying problem and evidence: The vision and architecture name mortgage acceleration analysis; the accepted debt-amortization model provides an auditable constant-rate baseline.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Compare an explicit immediate principal prepayment with continuing the same fixed monthly debt payment.
- Success measure: The documented operation answers this question from explicit inputs with reproducible results and no canonical writes.
- Priority: Batch order 3 of five; bounded extension of accepted capabilities before broader planning/UI integration.

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

- Endpoint: `POST /api/scenarios/debt-prepayment`.
- Require the accepted debt-amortization inputs plus immediatePrepayment, a non-negative amount no greater than principal. Do not infer interest-rate basis from user-zero loan notes.
- Reuse the merged debt-amortization calculator twice: baseline principal and principal minus immediatePrepayment, with identical currency, explicit monthly decimal interest rate and monthly payment. Prepayment happens before first interest accrual; no fees or penalties are modeled.
- Return both calculator outcomes including statuses, remaining balances and schedules. Include immediatePrepayment separately and scenarioTotalCashPaid = immediatePrepayment + scenario scheduled totalPaid; clearly mark partial totals for incomplete schedules.
- Only when both paths are PAID_OFF return lifetimeInterestSaved = baseline totalInterest - scenario totalInterest, payoffMonthsSaved = baseline payoffMonths - scenario payoffMonths, and lifetimeCashSaved = baseline totalPaid - scenarioTotalCashPaid.
- If either path is NON_AMORTIZING or HORIZON_LIMIT, lifetime savings and payoff-time delta are null with explicit comparison-unavailable reasons. Never compare a truncated total with a lifetime total as savings.

### Out of scope

No lender payoff quotes, fees, variable rates, recurring extra payments, invest-versus-prepay advice, reserve affordability, liability lookup/update, UI or persistence.

## User flow or behavior

1. A trusted caller submits the explicit inputs documented above.
2. The application validates them and performs the bounded calculation or read-only review.
3. The caller receives source/input context, results, and model limitations.
4. No result becomes an approved household decision or changes canonical state.

## Acceptance criteria

- [ ] Principal 1000, zero monthly rate, payment 300 and prepayment 400 produces baseline payoff in 4 months, scenario payoff in 2, scenario total cash paid 1000, zero lifetime interest/cash savings and 2 months saved.
- [ ] Zero prepayment produces identical paths and zero savings when paid off; prepayment equal to principal produces zero scenario schedule months while retaining the upfront cash in totals.
- [ ] Interest-bearing fixtures reconcile schedule totals and upfront payment using existing rounding; negative prepayments and amounts above principal are rejected.
- [ ] NON_AMORTIZING and HORIZON_LIMIT combinations retain their statuses and suppress unsupported lifetime comparisons, including a prepayment that makes only the scenario repayable.
- [ ] Use deterministic domain logic separated from HTTP transport. For monetary features, reject inputs above 17 integer digits or 2 fractional digits (including equivalent scientific notation); normalize three-letter alphabetic currency with Locale.ROOT. Reused calculators retain their documented rate precision and result conventions. Never silently round invalid inputs.
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
- Evidence: The vision and architecture name mortgage acceleration analysis; the accepted debt-amortization model provides an auditable constant-rate baseline.
- Alternatives: A general persisted scenario/plan framework or a broad dashboard.
- Rationale: Produces a testable planning answer without inventing household decisions or adding infrastructure.
- User input required: `NO`; calculator conventions and a metadata review are reversible product choices.

### PD-002 — Remain independent of concurrent work

- Decision: Exclusive ownership of `backend/src/main/java/com/waypoint/scenarios/debtprepayment/**`, `backend/src/test/java/com/waypoint/scenarios/debtprepayment/**`, and `agent/product/debt-prepayment-comparison/**`.
- Dependencies: Consume merged com.waypoint.planning.debtamortization classes read-only. No edits to existing calculator, no annual-rate conversion and no dependency on current workers or batch siblings.
- No shared README, roadmap, central log, decision log, workflow, template, build or configuration edits. Consistent with the previous batch, record implementation findings in the feature-local log; consolidate shared prose after the batch.
- Rationale: Each queued task can start from current merged code regardless of sibling completion. Reuse existing deterministic code through direct Java calls, not internal HTTP.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/017-debt-prepayment-comparison.md`
- Design brief: Not applicable; backend-only.
- Implementation owner: Claude Code, fresh conversation in isolated `task/017-debt-prepayment-comparison` branch/worktree.
- Review evidence: Pending. Deliver feature-local `api.md` and `implementation-log.md` including changes, tests, assumptions, architectural choices, limitations, next task and system-evolution recommendations.
- Delivery gates: Task PR, local ./verify.sh, green required CI verify, and independent Product Owner acceptance under agent/collaboration-workflow.md.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: None; framing only.
- Unmet criteria: Implementation and verification pending.
- Returned work: None.
- Follow-up opportunities: Household-facing integration after these bounded APIs are accepted; do not expand this task during implementation.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:
