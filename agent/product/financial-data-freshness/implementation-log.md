# Implementation log: Financial Data Freshness Review (Task 019)

Feature-local record, per this batch's shared-prose exception (see the
product brief's "Delivery handoff" and PD-002). Consolidation into
`agent/implementation-log.md` is a post-batch follow-up, not part of this
task.

## Changed

New, additive package `com.waypoint.review.freshness` under
`backend/src/main/java/com/waypoint/review/freshness/` — no existing file
was modified:

- `FinancialDataFreshnessCalculator` (`@Service`, no dependencies) — pure
  domain calculation. Given an explicit `householdId`, `reviewDate`,
  `maxAgeDays`, and a list of `FreshnessSourceRecord` inputs, computes
  `ageDays = reviewDate - sourceDate` with `ChronoUnit.DAYS` (exact
  calendar-day arithmetic, correct across leap years and month/year
  boundaries), classifies each record (`CURRENT`/`STALE`/`FUTURE_DATED`),
  sorts deterministically by `recordKind` then `recordId`, and produces
  zero-inclusive counts by kind and by classification. Validates its own
  invariants (non-null `householdId`/`reviewDate`/`sourceRecords`,
  `maxAgeDays` in `0..36500`) independently of the web layer, so a direct
  domain call rejects invalid values without going through HTTP.
- `FreshnessSourceRecord`, `FreshnessRecord` (records), `FreshnessRecordKind`
  (`ASSET`/`LIABILITY`) and `FreshnessClassification`
  (`CURRENT`/`STALE`/`FUTURE_DATED`) enums, `FinancialDataFreshnessResult`
  (record) — the domain input/output types. None carries a financial amount
  field, so the "never copy a financial amount into this response"
  constraint is a compile-time property, not just a runtime check.
- `InvalidFreshnessReviewInputException` — thrown by the calculator's own
  invariant checks.
- `FinancialDataFreshnessService` (`@Service`) — the only new code that
  touches persistence, and only by calling the existing, unmodified
  `AssetService.listAssets` and `LiabilityService.listLiabilities`, which
  already enforce household existence (`HouseholdNotFoundException`) and
  ownership scoping. Maps `Asset`/`Liability` entities to
  `FreshnessSourceRecord` (using `valuedAt`/`balanceAsOf` as the source
  date, never `createdAt`/`updatedAt`) and delegates classification to the
  calculator.
- `web/FinancialDataFreshnessController` — `GET
  /api/households/{householdId}/financial-data-freshness`. Its
  `InvalidFreshnessReviewInputException` handler is a controller-local
  `@ExceptionHandler` method (Spring resolves it before the shared
  `@RestControllerAdvice` for exceptions raised in this controller), so it
  cannot intercept a sibling controller's errors and requires no change to
  the shared `ApiExceptionHandler`. Unknown households, missing query
  parameters, and malformed query parameters (a non-ISO `reviewDate`, a
  fractional or overflowing `maxAgeDays`) are already handled by that shared
  advice's existing `HouseholdNotFoundException`,
  `MissingServletRequestParameterException`, and
  `MethodArgumentTypeMismatchException` paths — verified directly (see
  "Manual verification" below), not assumed.
- `web/dto/FreshnessRecordResponse` / `FinancialDataFreshnessResponse` — the
  HTTP representation, including the always-present zero-inclusive count
  maps and the `modelNote` explaining the review's limitation (freshness of
  present source rows relative to `reviewDate`, not proof of current
  correctness, not a historical reconstruction).
- `agent/product/financial-data-freshness/api.md` — request/response
  reference with worked examples for every acceptance-criteria case.

## Tests

- `FinancialDataFreshnessCalculatorTest` (18 tests, no Spring context):
  the exact acceptance-criteria boundary (30/31/-1 days), the zero-threshold
  same-day-vs-earlier case, three calendar-arithmetic edge cases (a leap-year
  Feb 29 boundary, a non-leap Feb 28 boundary, a year boundary), deterministic
  ordering by kind then id, zero-inclusive counts, empty-input handling,
  determinism (identical inputs -> equal results), and rejection of a null
  `householdId`/`reviewDate`/`sourceRecords`, a negative `maxAgeDays`, and a
  `maxAgeDays` above `36500` (plus acceptance of the `36500` boundary
  itself) directly against the domain calculator.
- `FinancialDataFreshnessApiIntegrationTest` (17 tests, `@SpringBootTest` +
  Testcontainers Postgres, building fixtures only through the existing
  household/asset/liability HTTP API): the same boundary/threshold/ordering
  matrix over HTTP, no financial amount present in the response body, correct
  `sourceType`/`sourceDate` echoing, empty-household zero counts,
  cross-household isolation, stable ordering and byte-for-byte identical
  repeated responses, no mutation of the underlying asset list across two
  calls with different thresholds, unknown-household 404, and the missing/
  malformed/fractional/negative/overflow/above-bound `400` cases. Every date
  fixture is anchored to `LocalDate.now()` at test-run time (not a fixed
  calendar date), so the suite is stable regardless of when it runs while
  still satisfying the existing asset/liability "not in the future"
  creation validation.

`./verify.sh`: 427 tests, 0 failures (35 in the new `review.freshness`
packages: 18 domain + 17 HTTP; 392 pre-existing, unaffected).

## Manual verification

Ran the packaged app locally against a throwaway, disposable Postgres
container (`docker run ... postgres:16-alpine` on a non-default host port,
not the shared development volume) on a non-default application port, then
exercised the endpoint directly with `curl` for:

- The exact acceptance-criteria example (`reviewDate=2026-09-06`,
  `maxAgeDays=30`): an asset valued `2026-08-07` returned `ageDays: 30`,
  `CURRENT`; a liability last balanced `2026-08-06` returned `ageDays: 31`,
  `STALE`. Matched `api.md`'s first example exactly.
- `FUTURE_DATED`: an asset valued `2026-08-26` reviewed as of
  `reviewDate=2026-08-25` returned `ageDays: -1`, `FUTURE_DATED` (the
  existing `valuedAt`-not-in-the-future creation rule is checked against the
  real current date, not the review's `reviewDate`, so this needed a
  past-dated review rather than the literal `2026-09-07` from the
  acceptance criteria's wording — the automated HTTP test covers the
  identical relative case independent of the real calendar date).
- Unknown household -> `404 HOUSEHOLD_NOT_FOUND`.
- Missing `reviewDate` -> `400 VALIDATION_FAILED`
  ("Required request parameter 'reviewDate' ... is not present").
- Malformed `reviewDate` (`not-a-date`) -> `400 MALFORMED_REQUEST`.
- Fractional `maxAgeDays` (`30.5`) -> `400 MALFORMED_REQUEST`.
- Negative `maxAgeDays` (`-1`) -> `400 VALIDATION_FAILED`
  ("maxAgeDays must be between 0 and 36500").
- Overflowing `maxAgeDays` (a 20-digit value) -> `400 MALFORMED_REQUEST`.
- `maxAgeDays` above the upper bound (`36501`) -> `400 VALIDATION_FAILED`.
- A newly created, empty household -> `200 OK` with an empty `records` array
  and every count key present at `0`.

All nine matched the documented `api.md` behavior exactly (the two worked
examples in that file were captured verbatim from these runs). The
container and app process were torn down afterward; nothing was left
running or persisted.

## Decisions

- The pure classification logic (`FinancialDataFreshnessCalculator`) is
  fully decoupled from the JPA `Asset`/`Liability` entities via the
  `FreshnessSourceRecord` input type, so it is unit-testable with plain
  `List.of(...)` fixtures and no Spring context — mirroring the
  `EmergencyFundRunwayCalculator`/`GoalContributionCalculator` pattern from
  the prior batch (normalize/validate -> compute -> throw a domain
  exception on invariant violation) even though this feature reads
  persisted state rather than taking every input from the request body.
- `FinancialDataFreshnessService` is the sole persistence-reading layer and
  deliberately thin: it calls the two existing, unmodified services and
  maps their entities to the calculator's input type. It adds no new
  household-validation logic, satisfying "Read existing AssetService and
  LiabilityService (and existing household validation) without edits."
- Counts (`countsByKind`, `countsByClassification`) are built as
  `EnumMap`s seeded with every enum constant at `0` before counting, so the
  empty-household case and any single-kind/-classification result both
  serialize with every expected key present — never an omitted key that a
  client would have to distinguish from "count is zero."
- The `InvalidFreshnessReviewInputException` handler is declared directly
  on `FinancialDataFreshnessController`, not added to the shared
  `ApiExceptionHandler`, per this task's exclusive-ownership constraint and
  to guarantee it cannot catch a sibling controller's errors — the same
  approach the emergency-fund-runway and goal-contribution-calculator tasks
  used for the identical constraint in the prior batch.
- `maxAgeDays` is bound as a primitive `int` `@RequestParam` (not a
  `@RequestBody` DTO field with Bean Validation annotations), because this
  is a `GET` with only two scalar query parameters; the range check
  (`0..36500`) instead lives once, in the domain calculator, which the HTTP
  layer already relies on via `InvalidFreshnessReviewInputException` — Spring's
  own type-conversion failure (`MethodArgumentTypeMismatchException`,
  already handled globally) covers the fractional/overflow malformed cases
  before the domain layer is ever reached.

## Assumptions

- None beyond what the brief already records as validated product
  assumptions (PD-001, PD-002). No missing household fact or preference was
  encountered; the only inputs this feature needs (`reviewDate`,
  `maxAgeDays`) are explicit, caller-supplied, and never defaulted from the
  server clock or an inferred cadence.

## Open questions

- None new. The brief's own follow-up list (household-facing integration,
  broader scenario/UI work) stands unchanged; this task does not expand it.

## Recommended next task

Per the brief's independence review (PD-002), no next task is authorized
here beyond what this batch's other queued tasks already cover in parallel.
A natural follow-up once the batch lands would be the shared-document
consolidation the brief's "Delivery handoff" and PD-002 already call out
(central implementation log, any README/status updates) — not a new
capability.

## System-evolution candidates

None identified specific to this task. The controller-local
`@ExceptionHandler` pattern and the "pure calculator over a
persistence-decoupled input type, assembled by a thin service that reuses
existing services" split both worked cleanly here, consistent with the
prior batch's emergency-fund-runway/goal-contribution-calculator precedent;
no new rule or template change is proposed.
