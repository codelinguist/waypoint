# Product Brief: Income Interruption Scenario

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Request: Generate five new tasks now.
- Repository evidence: The vision explicitly asks what happens if one income disappears for six months; the existing runway calculator covers only a constant shortfall.
- Constraints: Follow AGENTS.md and the existing isolated, maximum-three-worker pipeline.
- No new household amounts, priorities or financial rules were supplied.

## Product framing

- Underlying problem and evidence: The vision explicitly asks what happens if one income disappears for six months; the existing runway calculator covers only a constant shortfall.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Determine the reserve needed to withstand a caller-defined temporary income interruption.
- Success measure: The documented operation answers this question from explicit inputs with reproducible results and no canonical writes.
- Priority: Batch order 1 of five; bounded extension of accepted capabilities before broader planning/UI integration.

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

- Endpoint: `POST /api/scenarios/income-interruption`.
- Require currency, openingReserve, normalMonthlyNetIncome, interruptedMonthlyNetIncome, monthlyExpenses, horizonMonths, interruptionStartMonth and interruptionMonths. Money is non-negative. Interrupted income must not exceed normal income. Use explicit month indices, not inferred employment dates.
- Require horizonMonths in 1..1200, interruptionStartMonth in 1..horizonMonths, and interruptionMonths in 1..horizonMonths with the entire interval inside the horizon. Reject fractional and oversized integers before narrowing.
- Run baseline at normal income throughout. Run scenario at interrupted income for the inclusive interval beginning at interruptionStartMonth and lasting interruptionMonths, then restore normal income. Opening reserve and expenses match across paths.
- Return ordered monthly opening cash, income, expenses, net flow and closing cash for both paths, scenario-minus-baseline closing deltas, ending cash, minimum cash including opening cash, and first strictly negative closing month (null if absent). Preserve negative balances as modeled funding gaps, not automatic borrowing.
- Return additionalOpeningReserveNeeded = max(0, -minimumScenarioCash), including opening cash in the minimum. Explain that monthly netting cannot establish intramonth solvency and that recovery income is only an explicit assumption.

### Out of scope

No persisted scenarios, automatic household-state cloning, employment lookup, benefits, taxes, loans, interest, goal-delay claims, recommendations, UI or canonical writes.

## User flow or behavior

1. A trusted caller submits the explicit inputs documented above.
2. The application validates them and performs the bounded calculation or read-only review.
3. The caller receives source/input context, results, and model limitations.
4. No result becomes an approved household decision or changes canonical state.

## Acceptance criteria

- [ ] With opening reserve 100, normal income 100, interrupted income 0, expenses 80, horizon 3 and interruption covering months 2–3: baseline closes 120, 140, 160; scenario closes 120, 40, -40; extra opening reserve needed is 40 and first negative month is 3.
- [ ] A no-loss input (interrupted income equals normal income) yields identical paths and zero deltas; interruption at month 1 and ending at the horizon obey interval boundaries.
- [ ] Zero cash is not negative; negative balances remain visible and every row reconciles opening + income - expenses = closing. Recovery after the interruption is tested.
- [ ] Reject missing fields, invalid interval boundaries, malformed currency, negative money, fractional/overflow month counts and excessive monetary precision at domain and HTTP boundaries.
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
- Evidence: The vision explicitly asks what happens if one income disappears for six months; the existing runway calculator covers only a constant shortfall.
- Alternatives: A general persisted scenario/plan framework or a broad dashboard.
- Rationale: Produces a testable planning answer without inventing household decisions or adding infrastructure.
- User input required: `NO`; calculator conventions and a metadata review are reversible product choices.

### PD-002 — Remain independent of concurrent work

- Decision: Exclusive ownership of `backend/src/main/java/com/waypoint/scenarios/incomeinterruption/**`, `backend/src/test/java/com/waypoint/scenarios/incomeinterruption/**`, and `agent/product/income-interruption-scenario/**`.
- Dependencies: Tasks 012–014 and all batch siblings are unnecessary. This is a bounded variable-income scenario, not a second constant-input projection. No imports from unfinished cash-flow work.
- No shared README, roadmap, central log, decision log, workflow, template, build or configuration edits. Consistent with the previous batch, record implementation findings in the feature-local log; consolidate shared prose after the batch.
- Rationale: Each queued task can start from current merged code regardless of sibling completion. Reuse existing deterministic code through direct Java calls, not internal HTTP.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/015-income-interruption-scenario.md`
- Design brief: Not applicable; backend-only.
- Implementation owner: Claude Code, fresh conversation in isolated `task/015-income-interruption-scenario` branch/worktree.
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
