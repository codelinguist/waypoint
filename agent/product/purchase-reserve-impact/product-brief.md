# Product Brief: Purchase Impact on Cash Reserves

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Request: Generate five new tasks now.
- Repository evidence: User-zero documents major purchases and travel; Problems 3 and 4 ask for consequences and lifestyle guardrails instead of payment-only affordability.
- Constraints: Follow AGENTS.md and the existing isolated, maximum-three-worker pipeline.
- No new household amounts, priorities or financial rules were supplied.

## Product framing

- Underlying problem and evidence: User-zero documents major purchases and travel; Problems 3 and 4 ask for consequences and lifestyle guardrails instead of payment-only affordability.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: See how a proposed cash purchase changes reserve coverage against an explicitly chosen reserve floor.
- Success measure: The documented operation answers this question from explicit inputs with reproducible results and no canonical writes.
- Priority: Batch order 2 of five; bounded extension of accepted capabilities before broader planning/UI integration.

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

- Endpoint: `POST /api/scenarios/purchase-reserve-impact`.
- Require currency, availableReserve, purchaseAmount, monthlyExpenses, monthlyNetIncome and minimumReserve. All amounts are explicit non-negative temporary inputs, including minimumReserve; never supply a household policy default.
- Calculate reserveAfterPurchase = availableReserve - purchaseAmount, purchaseFundingGap = max(0, -reserveAfterPurchase), and reserveFloorGap = max(0, minimumReserve - reserveAfterPurchase). Return baseline floor gap too, so an existing gap is distinguishable from the purchase impact.
- For affordable-from-cash inputs, use the accepted emergency-fund runway calculator for before/after coverage with unchanged income and expenses. Return its FINITE and NO_SHORTFALL semantics without converting null runway into zero or infinity.
- When purchaseAmount exceeds availableReserve, retain signed reserveAfterPurchase and funding gap; after-purchase runway is unavailable with an explicit INSUFFICIENT_CASH reason, never computed from a silently clamped reserve.
- Return neutral facts: whether purchase fits supplied cash and whether remaining cash meets supplied floor. Never return an approved/denied purchase decision or recommend the floor. Zero purchase is valid.

### Out of scope

No asset sale assumptions, financing, borrowing, recurring purchase costs, goal delays, persisted rules, purchase approval, household entity lookup, UI or canonical writes.

## User flow or behavior

1. A trusted caller submits the explicit inputs documented above.
2. The application validates them and performs the bounded calculation or read-only review.
3. The caller receives source/input context, results, and model limitations.
4. No result becomes an approved household decision or changes canonical state.

## Acceptance criteria

- [ ] Reserve 1000, purchase 400, expenses 300, income 100 and floor 800 returns remaining cash 600, funding gap 0, baseline floor gap 0, after floor gap 200, and finite runway changing from 5 to 3 months.
- [ ] A purchase above reserve returns a negative cash balance, exact funding gap, and explicit unavailable after-runway; purchase equal to reserve returns a valid zero reserve calculation.
- [ ] Zero purchase preserves baseline; income covering expenses preserves NO_SHORTFALL/null semantics; zero floor and an already-breached floor are handled without normative labels.
- [ ] Read-only reuse of the merged runway calculator preserves its rounding and validation conventions; invalid input cannot become a 500 response.
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
- Evidence: User-zero documents major purchases and travel; Problems 3 and 4 ask for consequences and lifestyle guardrails instead of payment-only affordability.
- Alternatives: A general persisted scenario/plan framework or a broad dashboard.
- Rationale: Produces a testable planning answer without inventing household decisions or adding infrastructure.
- User input required: `NO`; calculator conventions and a metadata review are reversible product choices.

### PD-002 — Remain independent of concurrent work

- Decision: Exclusive ownership of `backend/src/main/java/com/waypoint/scenarios/purchasereserve/**`, `backend/src/test/java/com/waypoint/scenarios/purchasereserve/**`, and `agent/product/purchase-reserve-impact/**`.
- Dependencies: Consume merged com.waypoint.planning.runway classes read-only. Do not change their APIs or duplicate their runway arithmetic. No dependency on Tasks 012–014 or 015/017–019.
- No shared README, roadmap, central log, decision log, workflow, template, build or configuration edits. Consistent with the previous batch, record implementation findings in the feature-local log; consolidate shared prose after the batch.
- Rationale: Each queued task can start from current merged code regardless of sibling completion. Reuse existing deterministic code through direct Java calls, not internal HTTP.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/016-purchase-reserve-impact.md`
- Design brief: Not applicable; backend-only.
- Implementation owner: Claude Code, fresh conversation in isolated `task/016-purchase-reserve-impact` branch/worktree.
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
