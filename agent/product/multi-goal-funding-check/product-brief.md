# Product Brief: Multiple Goal Funding Check

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Request: Generate five new tasks now.
- Repository evidence: The household has education, retirement, travel and business-capital goals; evaluating one contribution in isolation cannot reveal overcommitted monthly savings.
- Constraints: Follow AGENTS.md and the existing isolated, maximum-three-worker pipeline.
- No new household amounts, priorities or financial rules were supplied.

## Product framing

- Underlying problem and evidence: The household has education, retirement, travel and business-capital goals; evaluating one contribution in isolation cannot reveal overcommitted monthly savings.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: See whether several explicitly modeled saving goals fit one monthly saving budget without choosing household priorities.
- Success measure: The documented operation answers this question from explicit inputs with reproducible results and no canonical writes.
- Priority: Batch order 4 of five; bounded extension of accepted capabilities before broader planning/UI integration.

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

- Endpoint: `POST /api/planning/multi-goal-funding-check`.
- Require one currency, a non-negative availableMonthlyBudget, and 1..50 goal inputs. Each has a unique caller reference (1..64 nonblank characters), positive targetAmount, non-negative currentAmount and contributionMonths in 1..1200.
- Reuse the accepted goal-contribution calculator independently for every goal, returning each result in caller order with its reference. All goals start contributing in the first modeled month; individual contributionMonths may differ.
- Sum the rounded-up per-goal monthly contributions to totalRequiredMonthlyContribution. Return budgetMinusRequired, shortfall = max(0, -budgetMinusRequired), unallocatedBudget = max(0, budgetMinusRequired), and FITS or SHORTFALL.
- This is the initial simultaneous monthly funding requirement under zero growth, not an optimizer or complete future allocation schedule. Callers must supply separately earmarked current amounts: return that assumption explicitly; no claimed verification of asset backing.
- No goal priority defaults, automatic reallocations, asset links, automatic target-date conversion, or canonical goal updates. Already-funded goals contribute zero requirement while retaining their calculator status.

### Out of scope

No optimization, household priority choices, future-value growth, persisted allocations, budget edits, FX, UI, goal-deadline inference or canonical writes.

## User flow or behavior

1. A trusted caller submits the explicit inputs documented above.
2. The application validates them and performs the bounded calculation or read-only review.
3. The caller receives source/input context, results, and model limitations.
4. No result becomes an approved household decision or changes canonical state.

## Acceptance criteria

- [ ] Goals with gaps 100 over 3 contributions and 200 over 2 contributions require 33.34 and 100 respectively. Budget 120 yields total requirement 133.34, signed difference -13.34, shortfall 13.34 and unallocated budget 0.
- [ ] A budget exactly equal to the total FITS; all goals already funded yield zero requirement; zero budget is valid and produces an appropriate status.
- [ ] Duplicate references, empty/oversized lists, null entries, missing values, fractional or overflowing month counts, invalid currency and malformed amounts are rejected.
- [ ] Sum rounded per-goal contributions rather than rounding an aggregate quotient; preserve deterministic input order and allow aggregate results to exceed individual input precision without truncation.
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
- Evidence: The household has education, retirement, travel and business-capital goals; evaluating one contribution in isolation cannot reveal overcommitted monthly savings.
- Alternatives: A general persisted scenario/plan framework or a broad dashboard.
- Rationale: Produces a testable planning answer without inventing household decisions or adding infrastructure.
- User input required: `NO`; calculator conventions and a metadata review are reversible product choices.

### PD-002 — Remain independent of concurrent work

- Decision: Exclusive ownership of `backend/src/main/java/com/waypoint/planning/multigoalfunding/**`, `backend/src/test/java/com/waypoint/planning/multigoalfunding/**`, and `agent/product/multi-goal-funding-check/**`.
- Dependencies: Consume merged com.waypoint.planning.goalcontribution classes read-only; no canonical Goals lookup or dependency on Tasks 012–017/019.
- No shared README, roadmap, central log, decision log, workflow, template, build or configuration edits. Consistent with the previous batch, record implementation findings in the feature-local log; consolidate shared prose after the batch.
- Rationale: Each queued task can start from current merged code regardless of sibling completion. Reuse existing deterministic code through direct Java calls, not internal HTTP.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/018-multi-goal-funding-check.md`
- Design brief: Not applicable; backend-only.
- Implementation owner: Claude Code, fresh conversation in isolated `task/018-multi-goal-funding-check` branch/worktree.
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
