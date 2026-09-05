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

- [x] Target 1000.00, current 100.00, 3 months returns remaining 900.00, monthly 300.00, total contributions 900.00, projected 1000.00 and excess 0. Verified in `GoalContributionCalculatorTest.calculatesEqualMonthlyContributionsThatExactlyReachTheTarget`, `GoalContributionApiIntegrationTest.returnsEqualMonthlyContributionsForAnUnfundedGoal`, and manually via `curl` (see `api.md`).
- [x] Target 100.00, current 0, 3 months returns monthly 33.34, total 100.02 and excess 0.02; a nonzero gap never rounds down to an insufficient contribution. Verified in `GoalContributionCalculatorTest.roundsMonthlyContributionUpSoTotalContributionsNeverFallShortOfTheGap`, the matching HTTP test, and manually.
- [x] At or above target returns ALREADY_FUNDED and zero contributions while preserving an existing surplus; one month returns the entire positive gap. Verified in `GoalContributionCalculatorTest.returnsAlreadyFundedWhenCurrentAmountEqualsTarget`, `...AndPreservesExistingSurplusWhenCurrentAmountExceedsTarget`, `...oneMonthReturnsTheEntirePositiveGap`, and the matching HTTP test.
- [ ] Tests reject zero, negative, fractional, missing, and greater-than-1200 month counts; results reconcile projectedAmount with currentAmount + totalContributions. Verified in both test classes; fractional months required a scoped `WholeNumberDeserializer` since Jackson otherwise truncates instead of rejecting — see `implementation-log.md` Decisions.
- [x] The documented POST endpoint returns the defined inputs, outputs and statuses using deterministic decimal arithmetic; identical requests return identical results without clock-dependent fields. Verified in `GoalContributionApiIntegrationTest.identicalRequestsProduceIdenticalResponses` and manually (repeated identical `curl` calls).
- [ ] Domain and HTTP tests cover required fields, currency normalization/rejection, amount bounds, precision and scale, all defined edge cases and successful calculation. Direct domain calls reject invalid values too. 18 domain unit tests call `GoalContributionCalculator` directly with no Spring context; 18 HTTP tests cover the same surface through `@WebMvcTest`.
- [x] No database reads/writes, entity changes or migrations are introduced. This endpoint accesses no household data and accepts no household/entity identifier; caller-supplied financial inputs are not logged. Confirmed by inspection: no repository/entity/migration references anywhere in the package, no logging statements, no path/request parameters beyond the JSON body.
- [x] All implementation and evidence changes stay within the exclusive ownership paths below. Existing application startup, shared error handling and the other two increments require no edits. Confirmed via `git diff --stat` against `main` (see `implementation-log.md`); `ApiExceptionHandler` and sibling packages untouched.
- [x] The feature works against the pre-batch main baseline without either sibling. Run `./verify.sh`, exercise the documented primary API flow with synthetic data, and record results and limitations in this brief before review. The required GitHub verify check must be green before merge. `./verify.sh` passed locally: 244 tests, `BUILD SUCCESS`, run before either sibling task's PR merged. Manual API flow exercised against a real running instance (see `implementation-log.md`). GitHub `verify` check pending on the opened PR.

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
- Review evidence: `agent/product/goal-contribution-calculator/api.md` (documented contract and worked examples) and `agent/product/goal-contribution-calculator/implementation-log.md` (changed behavior, tests/commands/results, decisions, assumptions, open questions, recommended next task, system-evolution note).
- Shared prose exception for this batch: Do not edit README.md, agent/implementation-log.md, docs/decisions/decisions.md, roadmap, workflow or shared templates. This task-specific exception to the routine central-log update implements the user's disjoint-file requirement. Consolidating the three feature-local implementation records and any README status/API links into shared docs is an explicit follow-up after this batch; it is not a prerequisite for any sibling. No new long-lived architecture decision is authorized here; return for reframing if one becomes necessary.
- Independence review: 009 owns `planning/runway`, 010 owns `planning/debtamortization`, 011 owns `planning/goalcontribution`, with matching exclusive tests and product directories. All three are stateless and independently deployable on the current baseline. Review each PR for this ownership constraint as well as financial correctness.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Independent review of PR #15 at d81e70cdec26d1d0b4c5c24f55ebf0773d40a1dc completed on 2026-09-05. Acceptance withheld for unresolved R1 and R2 below; passing CI does not cover these cases.
- Unmet criteria: Criterion 4 (reject out-of-range month counts) and criterion 6 (direct domain validation of amount bounds) remain unmet; see R1 and R2. Earlier implementation verification claims above are qualified by this review.
- Returned work: Resolve R1 and R2 with regression tests, rerun ./verify.sh, and update feature-local evidence for independent re-review.
- Follow-up opportunities: Stored-state integration only with a separately framed provenance/approval contract; richer models only when concrete household needs justify them; shared-document consolidation after this batch.
- Accepted or returned by Product Owner Agent: Codex — returned for fixes.
- Accepted or returned at: 2026-09-05

## Review findings — 2026-09-05

Reviewed PR #15 using `gh pr diff 15`, head
`d81e70cdec26d1d0b4c5c24f55ebf0773d40a1dc`, against the approved criteria and
collaboration workflow. Context was limited to the four requested files and
the actual PR diff; no other conversation history was used. No visual-review
file exists; UI criteria do not apply to this backend-only feature.

### R1 — BLOCKING — ACCEPTED — Unresolved: month overflow silently changes the input

- Evidence: `backend/src/main/java/com/waypoint/planning/goalcontribution/web/dto/WholeNumberDeserializer.java:24`
  returns `node.intValue()` after checking only `isIntegralNumber()`. A JSON
  integer `4294967299` narrows to `3`; the request's subsequent 1–1200
  validation therefore accepts it and calculates three contributions instead
  of rejecting the supplied out-of-range count. Negative `-4294967293` also
  narrows to `3`. The HTTP tests only test the upper violation `1201`.
- Impact: Violates criterion 4 and the explicit rejection/no-silent-conversion
  contract, returning a successful financial calculation for invalid inputs.
- Acceptance condition: Check representability/range before narrowing. HTTP
  regression tests for both values above and an integer beyond the long range
  must return structured HTTP 400, while 1 and 1200 remain valid and existing
  fractional rejection continues to pass.

### R2 — BLOCKING — ACCEPTED — Unresolved: negative decimal scale bypasses domain amount limits

- Evidence: `backend/src/main/java/com/waypoint/planning/goalcontribution/GoalContributionCalculator.java:91`
  counts digits as `value.precision() - Math.max(value.scale(), 0)`.
  `new BigDecimal("1E+17")` has precision 1 and scale -17, so this check counts
  one integer digit and accepts `100000000000000000.00` (18 digits). Both
  target and current amounts use this helper. The existing excessive-integer
  test uses a plain decimal with positive scale and misses this case.
- Impact: Criterion 6 explicitly requires direct domain entry points to reject
  invalid amounts independently of HTTP validation. The 17-digit invariant is
  bypassed by an equivalent numeric representation.
- Acceptance condition: Account for negative scale when enforcing integer
  digits, before expanding the value. Direct-domain tests must reject `1E+17`
  for either amount with `InvalidGoalContributionInputException`, accept the
  valid `1E+16` boundary representation, and preserve valid derived values
  beyond the input digit limit without truncation.

### R3 — RECOMMENDED — ACCEPTED — Open: normalize currency independently of locale

- Evidence: `backend/src/main/java/com/waypoint/planning/goalcontribution/GoalContributionCalculator.java:78`
  uses default-locale `currency.toUpperCase()`. Under Turkish locale, valid
  input `inr` becomes `İNR` instead of the ASCII currency code `INR`.
- Impact: Currency output depends on host configuration. No affected deployment
  locale is evidenced here, so this is recommended rather than merge-blocking.
- Acceptance condition: Use locale-independent normalization (for example
  `Locale.ROOT`) and add a regression that verifies `inr` becomes `INR` with a
  Turkish default locale, restoring the locale after the test.

### Verification and acceptance decision

- Independently confirmed the required `verify` check is SUCCESS for the
  reviewed head: https://github.com/codelinguist/waypoint/actions/runs/33970465781/job/101317945138.
  The diff records local `./verify.sh` success (244 tests), 36 feature tests,
  and synthetic manual API exercises; these are implementer-provided evidence,
  not new reviewer test runs.
- Reviewer JShell probes confirmed integer narrowing of `4294967299` to `3`,
  the digit expression returning 1 for `1E+17` and accepting its scale-2
  expansion, and Turkish uppercasing of `inr` to `İNR`. Findings follow the
  exact code paths in the diff; no full HTTP reproduction or suite rerun was
  performed during this review.
- Criteria 1–3 and the ordinary decimal arithmetic/reconciliation paths have
  supporting code and tests. Criterion 5's response mapping and repeatability
  have supporting tests, with the locale improvement recorded as R3. Criteria
  7–8 are supported by the diff's stateless implementation, scoped advice and
  exclusive paths; the task lifecycle change is within authorized ownership.
  Criterion 9 has recorded baseline/manual evidence and independently green CI.
  Criteria 4 and 6 remain unmet as detailed above. Feature acceptance remains
  `PENDING`; the feature is returned for fixes and automatic merge is not
  authorized.
- System evolution: Existing invariant rules already require these rejections;
  no shared rule change is necessary. Add feature-local regression coverage
  for representation boundaries in the fix round and record it in this
  feature's implementation log, preserving the shared-prose exception.
