# Product Brief: Emergency-Fund Runway

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
- Desired outcome: Understand how long an explicitly selected cash reserve covers an explicitly supplied monthly funding shortfall.
- Success measure: One documented API request returns an auditable result from explicit inputs without a database read or write, an LLM, or either sibling increment.
- Priority and rationale: First within this parallel batch: the vision explicitly asks what happens if an income disappears for six months; a constant-input runway is the smallest useful answer before full cash-flow projection. Phase 6 durable assumptions are deferred for this batch: these disposable calculations require no persisted assumptions or supersession model. A dashboard or broader scenario engine would introduce shared surfaces and larger scope.

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

- Accept one currency and explicit non-negative decimal `availableReserve`, `monthlyExpenses`, and `monthlyNetIncome`; all three are required, including explicit zero income.
- Compute `monthlyShortfall = max(monthlyExpenses - monthlyNetIncome, 0)`. For a positive shortfall, return decimal `runwayMonths = availableReserve / monthlyShortfall`, rounded down to two decimal places, and integer `fullMonthsCovered = floor(availableReserve / monthlyShortfall)` from the unrounded ratio.
- For zero shortfall return `NO_SHORTFALL`, shortfall zero, and null runway/full-month values; otherwise return `FINITE`, including zero runway for zero reserves. Explain that NO_SHORTFALL describes only the supplied constant inputs.
- Echo inputs and currency and identify the output as a constant-input estimate, excluding changes in income, spending, interest, inflation, and timing within a month.
- Expose `POST /api/planning/emergency-fund-runway` returning HTTP 200 for valid calculations, including modeled edge-case statuses. Accept explicit inputs `availableReserve, monthlyExpenses, monthlyNetIncome` and `currency` in JSON.
- Required monetary inputs use at most 17 integer digits and 2 fractional digits. Currency is a required three-letter alphabetic code, normalized to uppercase; no conversion or implicit household base currency. Reject null, missing, malformed, out-of-range, excessive-scale and excessive-precision inputs with structured HTTP 400 errors. Do not silently round invalid inputs.
- Implement typed, deterministic decimal domain calculation callable independently of HTTP, persistence and an LLM; domain entry points enforce their input invariants as well as transport validation. Output precision must accommodate valid derived values without silent overflow or input-size truncation.
- Dedicated unit and HTTP/integration coverage and a feature-local API guide with synthetic request/response examples.

### Out of scope

- Selecting liquid assets automatically, deciding an emergency-fund target, recommending spending cuts, changing income classifications, projecting dated cash flows, or linking to snapshots.
- Persistence, migrations, new or modified JPA entities, any database access, assumptions registry, shared planning framework, frontend, authentication changes, imports, external services, AI recommendations, or transactions.
- Any reference to or import from Tasks 009–011 sibling packages; any dependency on sibling PR merge order.

## User flow or behavior

1. A trusted caller explicitly supplies one currency and the required temporary inputs.
2. The API validates the request and passes typed inputs to the pure domain calculation.
3. The response echoes inputs, explains conventions through named output fields and documentation, and returns deterministic results or a clearly distinguished modeled edge-case status.
4. The caller may change inputs and repeat; nothing is saved or promoted to a financial fact, goal, recommendation or approved decision.

## Acceptance criteria

- [ ] Reserve 1000.00, expenses 400.00, income 100.00 yields shortfall 300.00, runway 3.33, full months 3, and FINITE.
- [ ] Reserve 0 with a positive shortfall yields FINITE and zero months; income equal to or above expenses yields NO_SHORTFALL with null month values, including when all inputs are zero.
- [ ] Tests distinguish rounding down from rounding to nearest (1000/600 -> 1.66), and full-month count is derived without premature rounding.
- [ ] The documented POST endpoint returns the defined inputs, outputs and statuses using deterministic decimal arithmetic; identical requests return identical results without clock-dependent fields.
- [ ] Domain and HTTP tests cover required fields, currency normalization/rejection, amount bounds, precision and scale, all defined edge cases and successful calculation. Direct domain calls reject invalid values too.
- [ ] No database reads/writes, entity changes or migrations are introduced. This endpoint accesses no household data and accepts no household/entity identifier; caller-supplied financial inputs are not logged.
- [ ] All implementation and evidence changes stay within the exclusive ownership paths below. Existing application startup, shared error handling and the other two increments require no edits.
- [ ] The feature works against the pre-batch main baseline without either sibling. Run `./verify.sh`, exercise the documented primary API flow with synthetic data, and record results and limitations in this brief before review. The required GitHub verify check must be green before merge.

## Risks and safeguards

- Financial-data or household-approval boundary: Every input is a temporary caller-supplied modeling value. No canonical state changes or household decisions are authorized by the result.
- Privacy or sensitive-data considerations: Trusted private use only, consistent with current delivery scope. No stored household lookup or request-body logging; use synthetic test and documentation data.
- Accessibility considerations: Backend-only; outputs and model statuses must be understandable in text, without colors or charts.
- Failure or misuse risks: A reserve is not automatically available for emergencies. The caller chooses the amount; this operation neither classifies assets nor recommends how much the household should hold.

## Product decisions

### PD-001 — Deliver one explicit-input calculation without stored-state integration

- Decision: Use a pure domain calculator and an additive stateless HTTP endpoint with explicit model conventions.
- Evidence: Phase 7 names the calculation, and accepted Task 008 already separates disposable analysis inputs from persisted facts. No household-specific terms are supplied in this framing request.
- Alternatives considered: Full planning engine; reading existing entities; persisting assumptions first; UI-first delivery.
- Rationale: Delivers a testable user capability without guessing household facts or creating inter-task dependencies.
- User input required: `NO` — values are supplied at use time; the documented model is not a recommended household policy.

### PD-002 — Enforce exclusive file ownership for this parallel batch

- Decision: Own only new `backend/src/main/java/com/waypoint/planning/runway/**`, matching `backend/src/test/java/com/waypoint/planning/runway/**`, `agent/product/emergency-fund-runway/**`, and this task's own lifecycle file (orchestrator-owned after queueing). Put domain, controller, DTOs and any narrowly controller-scoped error advice inside the exclusive package. Reuse existing infrastructure read-only; do not edit ApiExceptionHandler, shared tests, build configuration or application configuration. Use existing structured error conventions; feature-specific handlers must not catch sibling controllers' errors.
- Evidence: Tasks 007/008 collided in shared exception handling and prose, despite independent business capabilities. The user now explicitly requires disjoint implementation surfaces.
- Alternatives considered: Shared planning module and DTO changes; concurrent README/log updates followed by merge-conflict repair.
- Rationale: These three packages have no dependencies on each other, no migrations and no persisted entities. No new shared abstraction is required. Implementers must confirm compatibility with the existing project during their authorized codebase research and return for reframing if these boundaries cannot be met, rather than expand ownership.
- User input required: `NO` — this is a task-specific execution boundary implementing the explicit parallelism constraint.

## Delivery handoff

- Current task: `agent/tasks/009-emergency-fund-runway.md`
- Design brief, if applicable: Not applicable; backend-only, no UI exploration or implementation.
- Implementation owner: Claude Code in an isolated `task/009-emergency-fund-runway` branch/worktree, starting a fresh implementation conversation.
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
