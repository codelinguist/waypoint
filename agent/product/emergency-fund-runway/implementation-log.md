# Implementation log: Emergency-Fund Runway (Task 009)

Feature-local record, per this batch's shared-prose exception (see the
product brief's "Delivery handoff"). Consolidation into
`agent/implementation-log.md` is an explicit follow-up after Tasks 009-011,
not part of this task.

## Changed

New, additive package `com.waypoint.planning.runway` — no existing file was
modified:

- `EmergencyFundRunwayCalculator` (`@Service`, no dependencies) — pure
  domain calculation. Validates its own inputs (non-null, non-negative,
  scale <= 2, at most 17 integer digits, 3-letter currency) independently of
  the web layer, so a direct domain call rejects invalid values without
  going through HTTP.
- `EmergencyFundRunwayResult` (record) and `RunwayStatus` (`FINITE` /
  `NO_SHORTFALL`) — the domain output.
- `InvalidRunwayInputException` — thrown by the calculator's own invariant
  checks.
- `web/EmergencyFundRunwayController` — `POST
  /api/planning/emergency-fund-runway`. Its `InvalidRunwayInputException`
  handler is a controller-local `@ExceptionHandler` method, not an addition
  to the shared `ApiExceptionHandler`, so it cannot intercept a sibling
  task's controller errors.
- `web/dto/EmergencyFundRunwayRequest` / `EmergencyFundRunwayResponse` —
  Bean Validation mirrors the calculator's own bounds (`@Digits(integer =
  17, fraction = 2)`, `@DecimalMin("0")`, a 3-letter `@Pattern` for
  currency) so most invalid requests are rejected by the standard
  `MethodArgumentNotValidException` path before reaching the domain layer;
  the domain layer still enforces the same bounds for callers that bypass
  HTTP.
- `agent/product/emergency-fund-runway/api.md` — request/response reference
  with worked examples for every acceptance-criteria case.

`runwayMonths` and `fullMonthsCovered` are both computed from the *unrounded*
`availableReserve / monthlyShortfall` ratio using exact `BigInteger`
arithmetic on cent-scaled amounts (not floating-point division, and not by
flooring the already-rounded `runwayMonths`), which is what makes the
1000/600 -> 1.66 (not 1.67) rounding-down behavior and the exact full-month
count both hold simultaneously. `fullMonthsCovered` is typed as
`BigInteger` end to end (not `int`/`long`) specifically so an extreme input
(17 integer digits divided by a one-cent shortfall) cannot silently
overflow.

## Tests

- `EmergencyFundRunwayCalculatorTest` (20 tests): finite runway, the
  1000/600 -> 1.66 rounding-down case, a same-value boundary case
  confirming `fullMonthsCovered` isn't derived from the rounded
  `runwayMonths`, zero-reserve-with-shortfall, all three `NO_SHORTFALL`
  paths (income == expenses, income > expenses, all-zero), currency
  normalization, determinism, and rejection of null/negative/over-scale/
  over-precision amounts and malformed/blank/null currency directly against
  the domain calculator (no Spring context).
- `EmergencyFundRunwayApiIntegrationTest` (14 tests): the same
  success/edge-case matrix over HTTP, plus validation-error and
  malformed-JSON responses, currency-case normalization, and a
  byte-for-byte identical-response check for repeated identical requests.
  Uses the same `@SpringBootTest` + Testcontainers Postgres pattern as every
  other API integration test in this repository, because the Spring context
  requires a real datasource to boot (Flyway runs on startup) even though
  this feature itself performs no database access — the absence of any
  household/entity identifier on the endpoint, plus the identical-response
  test, is the actual evidence of statelessness.

`./verify.sh`: 242 tests, 0 failures (34 new versus the pre-batch `main`
baseline this branch was cut from).

## Manual verification

Ran the packaged app locally against a throwaway, disposable Postgres
container (`docker run ... postgres:16-alpine`, not the shared
`waypoint-postgres-data` volume), on a non-default port, then exercised the
endpoint directly with `curl` for: the finite-runway example, the
1000/600 rounding-down example, `NO_SHORTFALL` (income == expenses),
`NO_SHORTFALL` (all inputs zero), a negative-reserve validation error, and a
malformed-currency validation error. All six matched the documented
`api.md` behavior exactly (see that file's examples, which were captured
from these runs). The container and app process were torn down afterward;
nothing was left running or persisted.

## Decisions

- Pure calculation lives in a `@Service` with no repository/persistence
  dependency, mirroring the existing `PlanVersusActualService` pattern
  (normalize -> validate -> compute -> throw a domain exception on
  invariant violation) rather than pushing validation into a record's
  compact constructor, for consistency with the rest of the codebase.
- `fullMonthsCovered` is `BigInteger`, not `Integer`/`long`, specifically to
  satisfy the brief's "without silent overflow" requirement given the
  17-integer-digit input bound.
- The `InvalidRunwayInputException` handler is declared directly on
  `EmergencyFundRunwayController` rather than added to the shared
  `ApiExceptionHandler`, per this batch's exclusive-ownership constraint
  (PD-002) and to guarantee it cannot catch a sibling controller's errors.

## Assumptions

- None beyond what the brief already records as validated product
  assumptions (PD-001, PD-002). No missing household fact or preference was
  encountered; every input this feature needs is caller-supplied at request
  time, as scoped.

## Open questions

- None new. The brief's existing follow-up list (stored-state integration,
  richer modeling, shared-document consolidation after this batch) stands
  unchanged.

## Recommended next task

Per the brief's independence review, no next task is authorized here beyond
what Tasks 010/011 already cover in parallel. A natural follow-up once all
three land would be the shared-document consolidation the brief's Delivery
handoff already calls out (README status/endpoint docs, central
implementation log) — not a new capability.

## System-evolution candidates

None identified specific to this task. The controller-local
`@ExceptionHandler` pattern used here (versus editing the shared
`ApiExceptionHandler`) worked cleanly for a single-endpoint, no-persistence
feature and may be worth naming explicitly in `AGENTS.md` or the
collaboration workflow as the default way to satisfy an exclusive-ownership
constraint like PD-002, if a future parallel batch imposes the same
constraint again.
