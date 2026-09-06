# Product Brief: Constant Monthly Cash-Flow Projection

## Status

`ACCEPTED`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Problem as presented: Frame three tasks for parallel execution.
- Examples or evidence supplied: Phase 7 names cash-flow projection; existing runway handles one ratio but does not show a dated balance path or first shortfall month.
- Corrections and constraints supplied: Use the worktree-based concurrent workflow.
- Explicit preferences: Three implementation-ready tasks.

## Product framing

- Underlying problem: A household needs to see how an explicit starting cash balance changes month by month under explicit constant inflow and outflow assumptions.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Produce a deterministic dated projection, lowest balance, and first negative month without reading or changing stored household state.
- Success measure: A caller can inspect and reconcile every projected month and clearly see whether and when cash becomes negative.
- Priority and rationale: This completes a named Phase 7 primitive while staying independent from the new persisted-assumption model and future Scenario Engine.

## Knowledge classification

### Confirmed inputs

- The roadmap calls for cash-flow projection and scenario analysis must remain non-destructive.
- Current income and obligation records exist, but this parallel task must not couple to persistence or infer which records apply.

### Product assumptions to validate

- A constant monthly aggregate model is useful as the smallest projection before irregular dated events and stored-state integration.

### Open questions

- Variable schedules, inflation, multiple currencies, stored-state selection, and scenario overlays remain follow-ups.

## Scope

### In scope

- Required currency, `startMonth` (`YYYY-MM`), non-negative starting cash, non-negative constant monthly inflow, non-negative constant monthly outflow, and whole projection months from 1 through 1200.
- For each month return opening cash, inflow, outflow, signed net cash flow, and closing cash, with `closing = opening + inflow - outflow`; negative balances are valid modeled results.
- Return ending cash, lowest closing balance and its first month, and nullable first negative closing-balance month.
- Return statuses `REMAINS_NONNEGATIVE`, `BECOMES_NEGATIVE`, or `STARTS_NEGATIVE`; because starting cash is otherwise non-negative, omit `STARTS_NEGATIVE` unless negative starting balances are intentionally allowed. This brief chooses non-negative starting cash and therefore uses only the first two statuses.
- Enforce bounded two-decimal money inputs and overflow-safe derived values; normalize currency independently of locale.
- Expose `POST /api/planning/cash-flow-projection`, pure domain logic, HTTP mapping, dedicated tests, and feature-local API documentation.

### Out of scope

- Persistence, household/entity access, automatically aggregating income/obligations, multiple currencies, exchange rates, irregular events, interest, inflation, taxes, recommendations, UI, Plan/Scenario persistence, or AI behavior.

## User flow or behavior

1. A caller supplies an explicit start month and temporary constant cash-flow inputs.
2. The API validates and produces ordered monthly rows.
3. The caller sees the lowest and first negative month and can reconcile every balance.
4. Repeating with changed inputs creates no saved state.

## Acceptance criteria

- [x] Starting January 2027 with cash 1000.00, inflow 300.00, outflow 500.00, and 6 months yields closes 800, 600, 400, 200, 0, -200; status `BECOMES_NEGATIVE`, first negative June 2027, and lowest -200 in June.
- [x] Equal inflow/outflow preserves the starting balance; positive net flow increases it; zero starting cash with zero flows remains nonnegative.
- [x] Each row reconciles exactly and the next opening equals the previous closing; month labels advance correctly across year boundaries.
- [x] If the same lowest balance occurs more than once, the first month is returned; first negative means strictly below zero, not zero.
- [x] Months reject missing, fractional, zero, negative, overflow, and above 1200; monetary inputs reject negatives and excessive precision/scale without silent rounding at domain and HTTP boundaries.
- [x] Derived balances are not truncated by input precision bounds; currency normalization is locale-independent and inputs are not logged.
- [x] Identical requests return identical clock-free responses with no persistence, migration, household lookup, or sibling dependency.
- [x] All changes remain in Task 014's exclusive paths; `./verify.sh` passes and synthetic primary/edge flows are exercised.

## Risks and safeguards

- Financial-data or household-approval boundary: Inputs are temporary assumptions; the projection does not approve spending, change canonical data, or claim the household's actual future cash path.
- Privacy or sensitive-data considerations: Stateless private endpoint, synthetic evidence, no request-value logging.
- Accessibility considerations: Backend-only; dates, signs, status, and conventions must be explicit text/data rather than color.
- Failure or misuse risks: Documentation must state that constant aggregate flows exclude timing within a month and irregular events.

## Product decisions

### PD-001 — Use explicit aggregate inputs rather than stored-state integration

- Decision: Project one currency from caller-supplied constant monthly totals and a stated start month.
- Evidence: This is the smallest Phase 7 projection and avoids depending on Task 012 while both run concurrently.
- Alternatives considered: Aggregate persisted incomes/obligations; full scenario engine; daily projection.
- Rationale: Delivers an auditable primitive without guessing applicability or creating cross-task dependencies.
- User input required: `NO` — no household values or policies are chosen.

### PD-002 — Isolate the parallel implementation

- Decision: Exclusive ownership of `com.waypoint.planning.cashflow/**`, matching tests, `agent/product/cash-flow-projection/**`, and its lifecycle file. No migration or shared-file edits.
- Evidence: Stateless additive code can be implemented independently.
- Alternatives considered: Shared planning abstractions and existing household-package edits.
- Rationale: Avoids merge conflicts and premature infrastructure.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/014-cash-flow-projection.md`
- Design brief, if applicable: Not applicable; backend-only.
- Implementation owner: Claude Code in `task/014-cash-flow-projection`.
- Review evidence: PR #21 diff, feature-local `implementation-log.md` and `api.md` as included in that diff, and successful required CI `verify`; see the dated review below. Shared docs remain deferred until after the batch.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: All eight criteria satisfied by the reviewed implementation, domain/MVC tests, recorded synthetic manual flows, and successful required `verify` on `f0c5bc69f8260a52f9bb4e42ae87c15e2cdcf89e`; detailed mapping below.
- Unmet criteria: None.
- Returned work: None.
- Follow-up opportunities: Irregular events, persisted-state aggregation, multiple currencies, dated scenarios, and plan integration.
- Accepted or returned by Product Owner Agent: Codex
- Accepted or returned at: 2026-09-06



## Review findings — 2026-09-06

- Reviewer: Codex, independent Product Owner review of PR #21.
- Reviewed implementation commit: `f0c5bc69f8260a52f9bb4e42ae87c15e2cdcf89e` on `task/014-cash-flow-projection` against `main`, obtained with `gh pr diff 21`; local HEAD matched the PR head.
- Context: Only the four requested governing/task/brief files and the actual PR diff were read; no implementation conversation was used. Git/PR metadata established branch identity and check results.
- Findings: No `BLOCKING`, `RECOMMENDED`, or `OPTIONAL` findings warranted by the reviewed evidence. No finding dispositions are needed; no unresolved blocking findings remain.
- UI review: Not applicable (explicitly backend-only); `agent/ui/cash-flow-projection/visual-review.md` does not exist.

### Acceptance evidence against the complete brief

| Criterion | Evidence and assessment |
| --- | --- |
| 1. Six-month January 2027 example | Calculator test asserts all six exact closes, June first negative, June minimum, ending cash and status; MVC test asserts the endpoint result. The diff's API/manual record reports the same real-application curl result. Satisfied. |
| 2. Equal, positive and zero flows | Separate domain tests cover unchanged balance, rising balances and all-zero nonnegative results. Satisfied. |
| 3. Reconciliation and calendar progression | Domain tests check opening + inflow - outflow = closing, signed net flow and continuity for every row; November-to-February labels are asserted and recorded in the manual API flow. Satisfied. |
| 4. First minimum and strictly negative | Calculator updates the minimum only on a strict decrease and records only the first negative sign; dedicated tie, exact-zero and repeated-negative tests corroborate this. Satisfied. |
| 5. Input rejection at both boundaries | Domain validation enforces nonnegative, bounded money without rounding and months 1–1200. Typed domain months exclude fractional/overflow representations. HTTP required-field and digit/range annotations plus the field-local whole-number deserializer reject missing, fractional, overflow and out-of-range months. Domain/MVC tests exercise invalid values; manual evidence additionally covers scientific-notation integer-bound bypass rejection. Satisfied. |
| 6. Derived precision, locale and privacy | Addition/subtraction use unrestricted BigDecimal results, with input-only bounds and UNNECESSARY scale normalization; no result clamping or request logging appears in the diff. Locale.ROOT normalization is tested under Turkish locale. Satisfied by implementation inspection and tests. |
| 7. Determinism and isolation | Calculator has no clock, persistence or household dependency; controller maps explicit inputs directly to the calculator. Both domain and HTTP tests assert repeatability. No migration or sibling import appears in the diff. Satisfied. |
| 8. Exclusive paths and verification | Every changed path is in the task's exclusive code, test or feature-local prose directories. The implementation log records 49 focused tests and 392 full-suite tests passing with no failures/errors/skips, plus synthetic real-application primary/edge flows. Independently checked GitHub's required verify result: SUCCESS on the reviewed commit. Satisfied. |

Required-check evidence: [verify run](https://github.com/codelinguist/waypoint/actions/runs/33983633950/job/101353142295), completed 2026-09-05 18:19:32 UTC. This review inspected test code and recorded manual evidence and independently checked CI; it did not rerun the application or local suite.

The API documentation makes temporary assumptions, constant monthly aggregation, excluded intramonth timing/irregular events, signed balances and statuses explicit. The diff preserves financial-data approval boundaries because it neither reads nor persists household state. No shared-rule/template change is justified by this review.

Decision: `ACCEPTED`. All acceptance criteria are satisfied. This records product acceptance for automatic pipeline merge; the orchestrator must still require a green `verify` check on the updated PR head after this documentation commit. No separate human merge step is required.
