# Implementation Log: Equal Monthly Goal Contributions (Task 011)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md` for this batch).

## Changed

- Added `backend/src/main/java/com/waypoint/planning/goalcontribution/`:
  - `GoalContributionCalculator` — pure, stateless domain calculation.
    Validates its own inputs (currency format, amount sign/scale/precision,
    `contributionMonths` range 1-1200) independent of any transport
    validation, then computes `remainingAmount`, `monthlyContribution`
    (rounded up via `RoundingMode.CEILING`), `totalContributions`,
    `projectedAmount`, `amountAboveTarget`, and `ALREADY_FUNDED` /
    `CONTRIBUTIONS_REQUIRED` status.
  - `GoalContributionResult` — result record.
  - `GoalContributionStatus` — status enum.
  - `InvalidGoalContributionInputException` — domain validation failure.
  - `web/GoalContributionController` — `POST
    /api/planning/goal-contribution-calculator`.
  - `web/GoalContributionExceptionHandler` — `@RestControllerAdvice`
    scoped with `assignableTypes = GoalContributionController.class` so it
    cannot intercept another controller's exceptions; reuses the shared
    `ErrorResponse` shape read-only, without editing the shared
    `ApiExceptionHandler`.
  - `web/dto/GoalContributionRequest`, `web/dto/GoalContributionResponse`.
  - `web/dto/WholeNumberDeserializer` — see Decisions below.
- Added matching tests under
  `backend/src/test/java/com/waypoint/planning/goalcontribution/`.
- Added `agent/product/goal-contribution-calculator/api.md` with the
  documented request/response contract and worked examples.

No shared file was edited (`ApiExceptionHandler`, `README.md`, central
`agent/implementation-log.md`, `docs/decisions/decisions.md`, build/
migration files, and both sibling tasks' packages are all untouched).

## Tests

- `GoalContributionCalculatorTest` (18 tests) — pure domain unit tests:
  exact-division and round-up-remainder calculations, `ALREADY_FUNDED` at
  and above target (surplus preserved), single-month contribution, currency
  normalization, and rejection of null/blank/malformed currency, null/zero/
  negative `targetAmount`, negative `currentAmount`, excessive fraction/
  integer digits, and out-of-range `contributionMonths` (0, negative,
  1201), called directly against the calculator (no Spring context).
- `GoalContributionApiIntegrationTest` (18 tests) — `@WebMvcTest` slice
  (real MVC dispatch, validation, and exception handling, without a full
  Spring context or Testcontainers/Postgres, since this endpoint has no
  persistence dependency) covering the same success/edge cases through
  HTTP, plus missing-field, malformed-currency, malformed-JSON-body, and
  identical-request-identical-response checks.
- Command: `./mvnw --batch-mode -Dtest="com.waypoint.planning.goalcontribution.**" test`
  → `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) →
  `Tests run: 244, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`,
  run against the pre-batch `main` baseline (neither sibling task's PR
  merged), confirming this feature stands alone.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`)
against a throwaway local `postgres:16-alpine` container matching
`docker-compose.yml`'s connection settings (`waypoint`/`waypoint`/`waypoint`
on port 5432; Flyway applied its 5 existing migrations unchanged — this
feature adds none). Exercised via `curl`:

- Exact-division success (`1000.00` target, `100.00` current, 3 months) →
  `200`, `monthlyContribution: 300.00`, `amountAboveTarget: 0.00`.
- Round-up-remainder success (`100.00` target, `0` current, 3 months) →
  `200`, `monthlyContribution: 33.34`, `totalContributions: 100.02`,
  `amountAboveTarget: 0.02`.
- `ALREADY_FUNDED` with surplus preserved (`500.00` target, `650.00`
  current) → `200`, `amountAboveTarget: 150.00`.
- Zero `targetAmount`, negative `currentAmount`, `contributionMonths` of
  1201, excessive fraction digits, malformed currency, and a missing
  required field → each `400 VALIDATION_FAILED` with a field-specific
  message.
- Fractional `contributionMonths` (`3.5`) → `400 MALFORMED_REQUEST` (see
  Decisions).

All responses matched the documented contract in `api.md` exactly. Full
request/response pairs are recorded there rather than duplicated here.
Stopped the manual app and the throwaway Postgres container afterward; no
container or process was left running.

## Decisions

- **Fractional `contributionMonths` rejection (`WholeNumberDeserializer`).**
  Jackson's default behavior for a JSON number deserialized into an
  `Integer` field truncates toward zero (`3.5` → `3`) rather than failing.
  That would silently round an invalid input, which both the brief
  ("Do not silently round invalid inputs") and this task's acceptance
  criteria ("Tests reject zero, negative, fractional, missing... month
  counts") explicitly forbid. The shared Jackson `ObjectMapper` is outside
  this task's ownership boundary, so instead of a global config change,
  `contributionMonths` is annotated `@JsonDeserialize(using =
  WholeNumberDeserializer.class)` with a deserializer scoped to this
  package. A non-integral JSON number is now a `MALFORMED_REQUEST` (400)
  via the existing shared `HttpMessageNotReadableException` handler, not a
  `VALIDATION_FAILED` (400) — both are 400s with a structured body; the
  `error` code differs from the other rejected-`contributionMonths` cases
  because the failure happens during JSON deserialization, before the
  request object exists for Bean Validation to inspect. This is documented
  explicitly in `api.md` so it is not a surprising inconsistency.
- **`@WebMvcTest` instead of `@SpringBootTest` + Testcontainers for the
  HTTP-level test.** This endpoint has no persistence dependency and reads
  no household data. A full Spring context with a real Postgres
  Testcontainers boundary (the pattern used by every other
  `*ApiIntegrationTest` in this repository) would add an unnecessary
  dependency and slow the test for no correctness benefit here. The
  `@WebMvcTest` slice still exercises the real `DispatcherServlet`,
  request validation, JSON (de)serialization, and the feature's own
  exception handler — the true integration surface for this feature — via
  `@Import({GoalContributionCalculator.class,
  GoalContributionExceptionHandler.class})` to register the real
  (non-mocked) calculator and controller-advice beans.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain
  validation exception handler, matching the shared error-response shape
  without editing `ApiExceptionHandler` itself (PD-002 in the product
  brief).

## Assumptions

- Interpreted "monetary goal gap" (brief) as computed purely from the two
  supplied amounts — `remainingAmount = max(targetAmount - currentAmount,
  0)` — with no reference to any stored `FinancialGoal`, consistent with
  the brief's explicit out-of-scope list ("reading stored goals").
- Treated `currentAmount == targetAmount` as `ALREADY_FUNDED` (not
  `CONTRIBUTIONS_REQUIRED` with a zero contribution), per the brief's
  "`ALREADY_FUNDED` when `currentAmount >= targetAmount`" wording.
- No household fact, assumption, or approval was required or invented;
  every input is caller-supplied for this disposable calculation only, per
  the task's ownership and scope constraints.

## Open questions

None blocking. Per the brief, persisted-state integration (reading a real
`FinancialGoal`'s current amount, deriving `contributionMonths` from a goal
target date, or checking affordability against household cash flow) is an
explicit follow-up requiring its own product framing and provenance/
approval contract — not attempted here.

## Recommended next task

Follow-up framing (Codex) to connect this calculation to a real
`FinancialGoal`: given a household's goal ID, read its current progress and
target date, derive `contributionMonths` from today's date, and surface the
same result — with an explicit decision on whether/how the model result is
distinguished from a persisted plan or recommendation, per the facts/
assumptions/goals/recommendations/decisions distinction in
`docs/product/problems.md`.

## System-evolution candidate

Not proposing a change to `AGENTS.md` or a template from this task. The one
non-obvious wrinkle — Jackson's default float-to-int truncation for a JSON
number targeting an integer field — was fully resolvable within this
task's own ownership boundary (a scoped custom deserializer) and doesn't
require a shared-infrastructure change; noting it here in case a future
task hits the same default behavior and wants to reuse the pattern instead
of rediscovering it.
