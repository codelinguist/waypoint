# Product Brief: Household Financial Goals

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-05
- Last updated at: 2026-09-05

## User input

- Problem as presented: Task 006 identified the Goals domain as a follow-up opportunity; the roadmap calls for financial goals with target amounts, target dates, priority, and progress.
- Examples or evidence supplied: The product vision asks whether the household is ahead or behind its plan and whether a choice delays a mortgage, education, or retirement goal.
- Corrections and constraints supplied: This is a private household product; financial facts must remain canonical and AI must not silently change them.
- Explicit preferences: None supplied beyond the repository principles and roadmap.

## Product framing

- Underlying problem: The application can record financial position but cannot represent what the household is trying to achieve or show progress toward it.
- Primary user: Ralph and his wife reviewing their household plan.
- Desired outcome: A household-scoped goal can be recorded with a measurable monetary target, target date, priority, and current progress, and retrieved deterministically.
- Success measure: A trusted caller can create and retrieve goals and receive a deterministic progress value without changing assets, liabilities, income, obligations, or snapshots.
- Priority and rationale: Highest next increment. It is the smallest durable Phase 5 vertical slice and gives later planning and scenario work an explicit destination without depending on plan-versus-actual analysis.

## Knowledge classification

### Confirmed inputs

- The roadmap places Goals after financial snapshots and calls for target amounts, target dates, priority, and progress.
- Task 006 is accepted and its follow-up opportunities explicitly include the goals domain.
- The existing product distinguishes goals from facts, assumptions, recommendations, and decisions.

### Product assumptions to validate

- The first goal metric is a monetary target in one currency, represented by a current amount and target amount; non-monetary goal types are deferred.
- Progress is computed as `currentAmount / targetAmount * 100`, bounded to 0–100 for display, while the stored amounts remain the source of truth.
- Current progress is supplied explicitly by a trusted caller; the system does not infer it from unrelated account balances.
- Priority is an integer rank where lower numbers are higher priority.

### Open questions

- Whether future goals need contributions, milestones, ownership by person, or multiple metric types.
- Whether progress should later be derived from snapshots or planning calculations.

## Scope

### In scope

- A household-scoped `FinancialGoal` record with name, target amount, currency, target date, priority, current amount, timestamps, and a stable identifier.
- Deterministic validation for non-blank names, valid positive monetary targets, non-negative current progress, supported three-letter currency shape, non-past target dates (today or later), and positive priority.
- Create and list/retrieve API operations following existing household-scoped validation, ordering, and not-found conventions.
- Deterministic progress calculation and response fields that clearly identify target, current amount, remaining amount, and progress percentage.
- Unit and API/integration tests, including household isolation and persistence across the migration.

### Out of scope

- Editing, deleting, archiving, or completing goals.
- Contributions, recurring allocations, milestones, forecasts, scenario changes, or recommendations.
- Deriving current progress from assets, liabilities, snapshots, income, or obligations.
- Person ownership, shared-goal splits, multiple currencies per goal, non-monetary metrics, frontend, authentication, imports, or AI behavior.
- Any change to canonical financial facts or automatic approval of a household decision.

## User flow or behavior

1. A trusted private API caller supplies a household and goal fields.
2. The system validates the request and confirms the household exists.
3. The system persists the goal without modifying other financial aggregates.
4. A caller retrieves the household's goals or one goal scoped to that household.
5. The response includes deterministic remaining amount and bounded progress percentage.

## Acceptance criteria

- [x] The backend exposes documented household-scoped create and list/retrieve operations for monetary financial goals.
- [x] A goal persists name, target amount, currency, target date, priority, current amount, identifier, and timestamps; currency is normalized consistently with existing API behavior.
- [x] Invalid amounts, dates, priority, blank names, and malformed currency requests return the repository's established structured validation response and do not persist a goal.
- [x] Progress and remaining amount are deterministic, use decimal arithmetic, and progress is bounded to 0–100 without changing stored amounts.
- [x] Unknown households and cross-household goal identifiers are rejected with established not-found semantics.
- [x] Automated tests cover persistence, retrieval, validation, arithmetic, ordering, household isolation, and no mutation of unrelated financial records.
- [x] The implementation is backend-only, keeps domain logic outside transport concerns, adds only the goal schema needed for this increment, and passes `./verify.sh`.

**Target-date clarification:** “non-past” means today or later
(`@FutureOrPresent`). This matches the domain meaning of a goal date being a
date the household is planning toward; a literal “non-future” interpretation
would make real future goals impossible.

## Risks and safeguards

- Financial-data or household-approval boundary: Current amount is an explicit user-supplied planning value, not an inferred fact; creating a goal does not alter canonical financial data or approve a recommendation.
- Privacy or sensitive-data considerations: Every read and write is scoped to the requested household; no cross-household identifiers or data are exposed.
- Accessibility considerations: No UI is in scope; future presentation must expose amounts, dates, and progress in text as well as visual form.
- Failure or misuse risks: A percentage can imply false precision; return the source amounts and clearly label the percentage as computed progress.

## Product decisions

### PD-001 — Monetary goals are the smallest first metric

- Decision: Support one monetary target and one currency per goal, with an explicit current amount.
- Evidence: The roadmap explicitly names target amounts and progress, and the existing model already uses currency-specific decimal values.
- Alternatives considered: Generic metric definitions; progress derived from snapshots; contribution schedules.
- Rationale: This is implementable and testable without inventing goal semantics or coupling Goals to snapshots or the planning engine.
- User input required: `NO` — the household supplies values when using the API; no product preference is needed to frame this increment.

### PD-002 — Progress is derived, not authoritative

- Decision: Store target/current amounts and calculate remaining amount and bounded percentage on read.
- Evidence: Repository principles make canonical financial state authoritative and require deterministic arithmetic.
- Alternatives considered: Persisting a progress percentage; allowing arbitrary manual percentages.
- Rationale: Avoids stale or contradictory derived data while preserving an auditable calculation.
- User input required: `NO` — reversible and not a material household decision.

## Delivery handoff

- Current task: `agent/tasks/007-goals.md`
- Design brief, if applicable: Not applicable; backend-only task.
- Implementation owner: Claude Code
- Review evidence: See PR (linked from the task branch) and
  `agent/implementation-log.md`'s 2026-09-05 "Task 007: Household Financial
  Goals" entry for full changed/tests/decisions/assumptions detail.
  `./verify.sh` result: **187 tests, 0 failures** (33 new: 12
  `FinancialGoalServiceTest` unit tests + 21 `FinancialGoalApiIntegrationTest`
  integration tests). Primary flow manually exercised end-to-end against a
  disposable Postgres container (create household -> create goal -> get by
  id -> list -> validation failures -> unknown-household 404); see the log
  entry's "Tests" section for the exact steps and observed values.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: PR #9 review found all seven acceptance criteria
  satisfied. The implementation is backend-only and exposes documented
  household-scoped create/list/retrieve operations; persists the required
  fields with normalized currency and timestamps; returns structured
  validation and not-found responses; computes decimal remaining amount and
  bounded progress without mutating stored amounts; isolates households; and
  keeps domain logic outside transport concerns. The implementation log
  records 187 tests with 0 failures, including 33 goal tests and an end-to-end
  manual flow. The required GitHub `verify` check passed at
  https://github.com/codelinguist/waypoint/actions/runs/33962931660/job/101297904499.
- Unmet criteria: None known; all acceptance criteria above are checked.
  The target-date wording was clarified from “non-future” to “non-past” in
  this review; this records the domain-sensible interpretation already
  implemented and tested by the PR.
- Returned work: None.
- Follow-up opportunities: Goal updates/completion, contribution schedules, snapshot-derived progress, and non-monetary metrics.
- Accepted or returned by Product Owner Agent: Accepted.
- Accepted or returned at: 2026-09-05.

## Review findings

### 2026-09-05 — PR #9 review

- `RECOMMENDED` — `ACCEPTED`: The brief used “non-future target dates,” while
  the diff implements `@FutureOrPresent` and the integration tests accept
  today/future dates and reject past dates. Evidence: PR #9
  `CreateFinancialGoalRequest`, `FinancialGoalApiIntegrationTest`, and the
  implementation log’s recorded assumption. Acceptance condition: the brief
  must state the implemented domain rule explicitly as “non-past (today or
  later),” which is corrected above. No application-code change is required.

No `BLOCKING` findings remain. This backend-only feature has no applicable
`visual-review.md`; the verified result satisfies the complete acceptance
criteria, so feature acceptance is recorded above.
