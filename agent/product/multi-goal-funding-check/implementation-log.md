# Implementation Log: Multiple Goal Funding Check (WAP-12)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md`).

## Changed

- Added `backend/src/main/java/com/waypoint/planning/multigoalfunding/`:
  - `MultiGoalFundingCheckCalculator` — pure, stateless domain calculation.
    Validates `currency`, `availableMonthlyBudget`, and goal-count/reference
    invariants independently of any transport validation, reuses
    `com.waypoint.planning.goalcontribution.GoalContributionCalculator`
    read-only for each goal's own contribution requirement (wrapping any
    `InvalidGoalContributionInputException` into this feature's own
    exception type with the failing goal's `reference` in the message), sums
    the rounded per-goal `monthlyContribution` values, and derives
    `budgetMinusRequired`, `shortfall`, `unallocatedBudget`, and `FITS` /
    `SHORTFALL` status.
  - `GoalFundingInput` — per-goal domain input record (`reference`,
    `targetAmount`, `currentAmount`, `contributionMonths`).
  - `GoalFundingCheckItemResult` — per-goal result record pairing
    `reference` with the reused `GoalContributionResult`.
  - `MultiGoalFundingCheckResult` — aggregate result record.
  - `MultiGoalFundingStatus` — `FITS` / `SHORTFALL` status enum.
  - `InvalidMultiGoalFundingInputException` — domain validation failure.
  - `web/MultiGoalFundingCheckController` — `POST
    /api/planning/multi-goal-funding-check`.
  - `web/MultiGoalFundingCheckExceptionHandler` — `@RestControllerAdvice`
    scoped with `assignableTypes = MultiGoalFundingCheckController.class` so
    it cannot intercept another controller's exceptions; reuses the shared
    `ErrorResponse` shape read-only, without editing the shared
    `ApiExceptionHandler`.
  - `web/dto/MultiGoalFundingCheckRequest`, `web/dto/GoalFundingInputRequest`,
    `web/dto/MultiGoalFundingCheckResponse`, `web/dto/GoalFundingResultResponse`.
  - `web/dto/WholeNumberDeserializer` — package-scoped copy of the same
    deserializer used by `goalcontribution`; see Decisions below.
- Added matching tests under
  `backend/src/test/java/com/waypoint/planning/multigoalfunding/`.
- Added `agent/product/multi-goal-funding-check/api.md` with the documented
  request/response contract and worked examples.

No shared file was edited (`ApiExceptionHandler`, `README.md`, central
`agent/implementation-log.md`, `docs/decisions/decisions.md`, build/
migration files). `com.waypoint.planning.goalcontribution` was consumed
read-only (constructor-injected, no edits) per this task's exclusive
ownership and dependency constraints.

## Tests

- `MultiGoalFundingCheckCalculatorTest` (27 tests) — pure domain unit tests
  called directly against the calculator (no Spring context, using a plain
  `new GoalContributionCalculator()`): the documented two-goal shortfall
  example, budget-exactly-equal `FITS`, unallocated-budget `FITS`,
  already-funded goals contributing zero requirement, zero budget validity,
  caller-order preservation with differing `contributionMonths` per goal,
  summed-rounded-contributions precision, currency normalization,
  identical-input determinism, and rejection of null/malformed currency,
  null/negative/oversized `availableMonthlyBudget`, null/empty/oversized/
  null-containing goal lists, blank/oversized/duplicate references, and
  invalid per-goal amounts/months (delegated-and-wrapped from
  `GoalContributionCalculator`, including the negative-scale digit-limit
  case).
- `MultiGoalFundingCheckApiIntegrationTest` (28 tests) — `@WebMvcTest` slice
  (real MVC dispatch, validation, and exception handling, without a full
  Spring context or Testcontainers/Postgres, since this endpoint has no
  persistence dependency) covering the same success/edge cases through
  HTTP, plus missing-field, malformed-JSON-body, container-element
  `@NotNull` null-goal rejection, and identical-request-identical-response
  checks.
- Command: `./mvnw --batch-mode -Dtest="com.waypoint.planning.multigoalfunding.**" test`
  → `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) →
  `Tests run: 577, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against
a throwaway local `postgres:16-alpine` container matching
`docker-compose.yml`'s connection settings (`waypoint`/`waypoint`/`waypoint`,
mapped to a free host port; Flyway applied its 6 existing migrations
unchanged — this feature adds none). Exercised via `curl`; full
request/response pairs are recorded in `api.md` rather than duplicated here.
Confirmed:

- The documented two-goal shortfall example, budget-exactly-equal `FITS`,
  and already-funded/zero-budget `FITS` examples all matched `api.md`
  exactly.
- Duplicate `reference`, empty `goals`, a `null` entry inside `goals`,
  fractional `contributionMonths`, a negative-scale digit-limit bypass
  attempt (`1E+17`), 51 `goals` entries, and a non-JSON body each returned
  the documented `400` response.

Stopped the manual app and the throwaway Postgres container afterward; no
container or process was left running.

## Decisions

- **Delegate per-goal validation and calculation to
  `GoalContributionCalculator` instead of duplicating it.** The brief
  requires reusing the accepted calculator "independently for every goal."
  Rather than re-validating `targetAmount`/`currentAmount`/
  `contributionMonths` a second time with duplicated logic, this feature
  calls `goalContributionCalculator.calculate(...)` per goal and catches
  its `InvalidGoalContributionInputException`, rewrapping it as
  `InvalidMultiGoalFundingInputException` with the failing goal's
  `reference` prefixed onto the message. This keeps a single source of
  truth for per-goal amount/month invariants (including the negative-scale
  digit-limit and locale-independent currency handling already fixed in
  that calculator) while still surfacing which goal failed.
  `currency`, `availableMonthlyBudget`, goal-count bounds, and reference
  uniqueness/format are this feature's own invariants and are validated
  locally, mirroring `GoalContributionCalculator`'s own validation style
  (each feature enforces its own rules independent of transport).
- **Container element constraint (`List<@NotNull @Valid
  GoalFundingInputRequest> goals`) to reject a `null` entry in `goals`.**
  Bean Validation's default cascaded `@Valid` on a collection field
  validates each non-null element but silently skips `null` elements
  unless a constraint is placed on the type argument itself (a Bean
  Validation 2.0+ "container element constraint"). Verified working via
  both the automated `rejectsNullGoalEntry` HTTP test and manual `curl`
  verification (`goals[1]: goals must not contain null entries`,
  `400 VALIDATION_FAILED`, not a `NullPointerException`).
  `MultiGoalFundingCheckCalculator` also independently rejects a `null`
  `GoalFundingInput` in its own list-iteration loop, so the domain layer
  enforces the same invariant regardless of transport, consistent with
  `GoalContributionCalculator`'s pattern.
  `MultiGoalFundingCheckController.toGoalFundingInput` additionally
  null-checks before mapping so a `null` element that reached the
  controller (e.g. if the container-element constraint were ever removed)
  would surface as this feature's own domain exception rather than a raw
  `NullPointerException`.
- **`@WebMvcTest` instead of `@SpringBootTest` + Testcontainers for the
  HTTP-level test**, importing both `MultiGoalFundingCheckCalculator` and
  the real (non-mocked) `GoalContributionCalculator` bean — matching the
  precedent set by `GoalContributionApiIntegrationTest` for this
  endpoint's lack of a persistence dependency.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain
  validation exception handler, matching the shared error-response shape
  without editing `ApiExceptionHandler` itself (PD-002 in the product
  brief).

## Assumptions

- "Reuse the accepted goal-contribution calculator independently for every
  goal" (brief) was read as: call the calculator once per goal with no
  shared state or interaction between goals, which is what the delegation
  in the Decisions section above does — not an instruction to fork or
  duplicate its source.
- Treated `budgetMinusRequired == 0` (budget exactly equal to the total
  requirement) as `FITS`, per the brief's explicit "a budget exactly equal
  to the total FITS" acceptance criterion.
- Goal `reference` uniqueness is an exact (case-sensitive) string match;
  the brief does not specify case-insensitive comparison and no household
  reference-naming convention exists yet to justify one.
- No household fact, assumption, or approval was required or invented;
  every input is caller-supplied for this disposable calculation only, per
  the task's ownership and scope constraints.

## Open questions

None blocking. Per the brief, connecting this check to real persisted
`FinancialGoal` records (reading current progress/target dates instead of
caller-supplied amounts) and any household-facing priority/reallocation
logic are explicit follow-ups requiring their own product framing — not
attempted here.

## Recommended next task

Follow-up framing (Codex) to connect this check to real `FinancialGoal`
records: given a household's goal IDs, read each goal's current progress
and derive `contributionMonths` from its target date and today's date, then
run the same simultaneous-funding check — with an explicit decision on how
household budget is supplied (a stored assumption vs. a caller-supplied
value) and how a `SHORTFALL` result is surfaced as a recommendation rather
than a silent fact, per the facts/assumptions/goals/recommendations/
decisions distinction in `docs/product/problems.md`.

## System-evolution candidate

Not proposing a change to `AGENTS.md` or a template from this task. The one
non-obvious wrinkle — Bean Validation's default silent skip of `null`
collection elements under cascaded `@Valid`, and the container-element-
constraint fix — is noted here in case a future task adds another
list-of-objects request field and wants to reuse the pattern instead of
rediscovering it.
