# Product Brief: Equal Monthly Goal Contributions

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-05
- Last updated at: 2026-09-05

## User input

- Problem as presented: Frame three smallest valuable roadmap increments that can be implemented fully in parallel after Tasks 006, 007 and 008 merged.
- Examples or evidence supplied: Roadmap Phase 7 names this calculation; the vision calls for deterministic explanations of the consequences of household choices.
- Corrections and constraints supplied: No shared implementation files, migrations, or entity changes; avoid shared prose conflicts. Stop for any material missing household fact or preference.
- Explicit preferences: Independent worktrees and three QUEUED tasks numbered 009–011; no household financial assumptions invented during framing.

## Product framing

- Underlying problem: Stored financial position and goals alone cannot answer this planning question deterministically.
- Primary user: Ralph and his wife, initially through the trusted private backend API.
- Desired outcome: Find the equal monthly saving amount needed to close an explicitly supplied monetary goal gap over an explicit number of contributions.
- Success measure: One documented API request returns an auditable result from explicit inputs without a database read or write, an LLM, or either sibling increment.
- Priority and rationale: Third within this parallel batch: completes the smallest Phase 7 goal-contribution calculation after merged Goals, while avoiding calendar, investment-return, or allocation-policy assumptions. Phase 6 durable assumptions are deferred for this batch: these disposable calculations require no persisted assumptions or supersession model. A dashboard or broader scenario engine would introduce shared surfaces and larger scope.

## Knowledge classification

### Confirmed inputs

- The user confirms Tasks 006–008 are merged; Goals and plan-versus-actual briefs record acceptance.
- Roadmap Phase 7 explicitly includes this capability. Product problems distinguish facts, assumptions, goals and decisions.
- The recent implementation log records real conflicts in README.md, ApiExceptionHandler.java and the implementation-log insertion point during Tasks 007/008.

### Product assumptions to validate

- A documented private API is a useful first delivery surface, consistent with the accepted backend-only increments.
- A single-currency calculation from caller-supplied temporary values is useful before connecting it to saved state. This is a reversible product scope choice, not a claim about household finances.

### Open questions

- No blocking household input is needed: balances, rates, periods and choices remain required caller inputs at use time.
- Persisted-state integration and richer modeling are follow-ups; if implementation reveals a material missing household choice, stop and ask instead of adding a default.

## Scope

### In scope

- Accept one currency, positive decimal `targetAmount`, non-negative decimal `currentAmount`, and required integer `contributionMonths` from 1 through 1200. This is the count of monthly contributions, not an inferred interval between dates.
- Calculate `remainingAmount = max(targetAmount - currentAmount, 0)` and `monthlyContribution = remainingAmount / contributionMonths`, rounded UP to two decimal places. Assume zero growth, fees and withdrawals, explicitly disclosed as model conventions.
- Return echoed inputs, remainingAmount, monthlyContribution, totalContributions = monthlyContribution * contributionMonths, projectedAmount = currentAmount + totalContributions, and amountAboveTarget = max(projectedAmount - targetAmount, 0).
- Return ALREADY_FUNDED when currentAmount >= targetAmount, with zero remaining amount and contributions; preserve the actual currentAmount and any existing amountAboveTarget. Otherwise return CONTRIBUTIONS_REQUIRED.
- Do not read a stored goal, choose its current amount, infer monthly periods from its date, or check affordability. The caller explicitly supplies inputs for a disposable calculation.
- Expose `POST /api/planning/goal-contribution-calculator` returning HTTP 200 for valid calculations, including modeled edge-case statuses. Accept explicit inputs `targetAmount, currentAmount, contributionMonths` and `currency` in JSON.
- Required monetary inputs use at most 17 integer digits and 2 fractional digits. Currency is a required three-letter alphabetic code, normalized to uppercase; no conversion or implicit household base currency. Reject null, missing, malformed, out-of-range, excessive-scale and excessive-precision inputs with structured HTTP 400 errors. Do not silently round invalid inputs.
- Implement typed, deterministic decimal domain calculation callable independently of HTTP, persistence and an LLM; domain entry points enforce their input invariants as well as transport validation. Output precision must accommodate valid derived values without silent overflow or input-size truncation.
- Dedicated unit and HTTP/integration coverage and a feature-local API guide with synthetic request/response examples.

### Out of scope

- Editing FinancialGoal or its progress semantics, reading stored goals, date arithmetic, contribution persistence, automatic allocations, affordability judgments, investment growth, inflation, or multiple-goal prioritization.
- Persistence, migrations, new or modified JPA entities, any database access, assumptions registry, shared planning framework, frontend, authentication changes, imports, external services, AI recommendations, or transactions.
- Any reference to or import from Tasks 009–011 sibling packages; any dependency on sibling PR merge order.

## User flow or behavior

1. A trusted caller explicitly supplies one currency and the required temporary inputs.
2. The API validates the request and passes typed inputs to the pure domain calculation.
3. The response echoes inputs, explains conventions through named output fields and documentation, and returns deterministic results or a clearly distinguished modeled edge-case status.
4. The caller may change inputs and repeat; nothing is saved or promoted to a financial fact, goal, recommendation or approved decision.

## Acceptance criteria

- [ ] Target 1000.00, current 100.00, 3 months returns remaining 900.00, monthly 300.00, total contributions 900.00, projected 1000.00 and excess 0.
- [ ] Target 100.00, current 0, 3 months returns monthly 33.34, total 100.02 and excess 0.02; a nonzero gap never rounds down to an insufficient contribution.
- [ ] At or above target returns ALREADY_FUNDED and zero contributions while preserving an existing surplus; one month returns the entire positive gap.
- [ ] Tests reject zero, negative, fractional, missing, and greater-than-1200 month counts; results reconcile projectedAmount with currentAmount + totalContributions.
- [ ] The documented POST endpoint returns the defined inputs, outputs and statuses using deterministic decimal arithmetic; identical requests return identical results without clock-dependent fields.
- [ ] Domain and HTTP tests cover required fields, currency normalization/rejection, amount bounds, precision and scale, all defined edge cases and successful calculation. Direct domain calls reject invalid values too.
- [ ] No database reads/writes, entity changes or migrations are introduced. This endpoint accesses no household data and accepts no household/entity identifier; caller-supplied financial inputs are not logged.
- [ ] All implementation and evidence changes stay within the exclusive ownership paths below. Existing application startup, shared error handling and the other two increments require no edits.
- [ ] The feature works against the pre-batch main baseline without either sibling. Run `./verify.sh`, exercise the documented primary API flow with synthetic data, and record results and limitations in this brief before review. The required GitHub verify check must be green before merge.

## Risks and safeguards

- Financial-data or household-approval boundary: Every input is a temporary caller-supplied modeling value. No canonical state changes or household decisions are authorized by the result.
- Privacy or sensitive-data considerations: Trusted private use only, consistent with current delivery scope. No stored household lookup or request-body logging; use synthetic test and documentation data.
- Accessibility considerations: Backend-only; outputs and model statuses must be understandable in text, without colors or charts.
- Failure or misuse risks: Required saving does not establish affordability or approve an allocation. Keep the result descriptive and separate from saved goal progress and household decisions.

## Product decisions

### PD-001 — Deliver one explicit-input calculation without stored-state integration

- Decision: Use a pure domain calculator and an additive stateless HTTP endpoint with explicit model conventions.
- Evidence: Phase 7 names the calculation, and accepted Task 008 already separates disposable analysis inputs from persisted facts. No household-specific terms are supplied in this framing request.
- Alternatives considered: Full planning engine; reading existing entities; persisting assumptions first; UI-first delivery.
- Rationale: Delivers a testable user capability without guessing household facts or creating inter-task dependencies.
- User input required: `NO` — values are supplied at use time; the documented model is not a recommended household policy.

### PD-002 — Enforce exclusive file ownership for this parallel batch

- Decision: Own only new `backend/src/main/java/com/waypoint/planning/goalcontribution/**`, matching `backend/src/test/java/com/waypoint/planning/goalcontribution/**`, `agent/product/goal-contribution-calculator/**`, and this task's own lifecycle file (orchestrator-owned after queueing). Put domain, controller, DTOs and any narrowly controller-scoped error advice inside the exclusive package. Reuse existing infrastructure read-only; do not edit ApiExceptionHandler, shared tests, build configuration or application configuration. Use existing structured error conventions; feature-specific handlers must not catch sibling controllers' errors.
- Evidence: Tasks 007/008 collided in shared exception handling and prose, despite independent business capabilities. The user now explicitly requires disjoint implementation surfaces.
- Alternatives considered: Shared planning module and DTO changes; concurrent README/log updates followed by merge-conflict repair.
- Rationale: These three packages have no dependencies on each other, no migrations and no persisted entities. No new shared abstraction is required. Implementers must confirm compatibility with the existing project during their authorized codebase research and return for reframing if these boundaries cannot be met, rather than expand ownership.
- User input required: `NO` — this is a task-specific execution boundary implementing the explicit parallelism constraint.

## Delivery handoff

- Current task: `agent/tasks/011-goal-contribution-calculator.md`
- Design brief, if applicable: Not applicable; backend-only, no UI exploration or implementation.
- Implementation owner: Claude Code in an isolated `task/011-goal-contribution-calculator` branch/worktree, starting a fresh implementation conversation.
- Review evidence: Pending. Add feature-local `api.md` for API examples. Record changed behavior, tests/commands/results, decisions, assumptions, unresolved questions, recommended next task and system-evolution candidates in this brief's delivery evidence or a linked feature-local `implementation-log.md`.
- Shared prose exception for this batch: Do not edit README.md, agent/implementation-log.md, docs/decisions/decisions.md, roadmap, workflow or shared templates. This task-specific exception to the routine central-log update implements the user's disjoint-file requirement. Consolidating the three feature-local implementation records and any README status/API links into shared docs is an explicit follow-up after this batch; it is not a prerequisite for any sibling. No new long-lived architecture decision is authorized here; return for reframing if one becomes necessary.
- Independence review: 009 owns `planning/runway`, 010 owns `planning/debtamortization`, 011 owns `planning/goalcontribution`, with matching exclusive tests and product directories. All three are stateless and independently deployable on the current baseline. Review each PR for this ownership constraint as well as financial correctness.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Pending implementation and independent review.
- Unmet criteria: Not yet implemented.
- Returned work: None.
- Follow-up opportunities: Stored-state integration only with a separately framed provenance/approval contract; richer models only when concrete household needs justify them; shared-document consolidation after this batch.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:
