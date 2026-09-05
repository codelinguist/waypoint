# Implementation log: Emergency-Fund Runway (Task 009)

Feature-local record, per this batch's shared-prose exception (see the
product brief's "Delivery handoff"). Consolidation into
`agent/implementation-log.md` is an explicit follow-up after Tasks 009-011,
not part of this task.

## Fix round 1 (2026-09-05) — R1/R2 from Codex's PR #13 review

Applied both `ACCEPTED` `BLOCKING` findings from the product brief's
"Review findings — 2026-09-05" section:

- **R1 (currency validation depended on JVM default locale):**
  `EmergencyFundRunwayCalculator.normalizeCurrency` now validates the
  supplied code is exactly 3 ASCII letters (`^[A-Za-z]{3}$`) *before* any
  case change, then uppercases with `Locale.ROOT` rather than the JVM
  default locale. This fixes both directions of the finding: a valid
  lowercase code (`inr`) no longer fails validation under a Turkish
  default locale (where naive `.toUpperCase()` produces `İNR`), and a
  2-character code that would expand to 3 letters under `Locale.US`
  case-folding (`ßa` -> `SSA`) is rejected as malformed before expansion,
  closing the direct-domain-call gap the HTTP-layer `@Pattern` didn't
  cover. Added
  `normalizesCurrencyCaseIndependentlyOfDefaultLocale` (sets/restores
  `Locale.setDefault` around the call) and
  `rejectsATwoCharacterCodeThatExpandsToThreeUppercaseLetters` to
  `EmergencyFundRunwayCalculatorTest`.
- **R2 (HTTP edge-case evidence incomplete):** Added
  `returnsNoShortfallWithNullMonthValuesWhenIncomeExceedsExpenses` to
  `EmergencyFundRunwayApiIntegrationTest` (reserve 1000.00 / expenses
  400.00 / income 500.00 -> 200, `NO_SHORTFALL`, shortfall `0.00`, both
  month fields present with JSON `null`). Replaced `doesNotExist()` with
  `value(nullValue())` on `runwayMonths`/`fullMonthsCovered` in the
  existing equality and all-zero HTTP tests, since Jackson serializes
  these fields as JSON `null` rather than omitting them and `doesNotExist`
  does not distinguish "field absent" from "field present with null value."
  Added the missing `monthlyShortfall` equal-to-zero assertion to the
  existing domain-level `returnsNoShortfallWhenIncomeExceedsExpenses` test.

`./verify.sh`: 245 tests, 0 failures (37 in the `runway` package: 22
domain + 15 HTTP, up from 34 total/20+14 before this round). No production
behavior changed outside `normalizeCurrency`'s validation order and locale
handling; the documented request/response contract in `api.md` is
unaffected (currency was already documented as case-insensitive-in,
uppercase-out).

Merged `origin/main` into this branch before this round (fast-forward, no
conflicts — no sibling task had merged in the interim).

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

- `EmergencyFundRunwayCalculatorTest` (22 tests): finite runway, the
  1000/600 -> 1.66 rounding-down case, a same-value boundary case
  confirming `fullMonthsCovered` isn't derived from the rounded
  `runwayMonths`, zero-reserve-with-shortfall, all three `NO_SHORTFALL`
  paths (income == expenses, income > expenses, all-zero), currency
  normalization (including locale-independence, per the fix round below),
  determinism, and rejection of null/negative/over-scale/over-precision
  amounts and malformed/blank/null currency directly against the domain
  calculator (no Spring context).
- `EmergencyFundRunwayApiIntegrationTest` (15 tests): the same
  success/edge-case matrix over HTTP, plus validation-error and
  malformed-JSON responses, currency-case normalization, and a
  byte-for-byte identical-response check for repeated identical requests.
  Uses the same `@SpringBootTest` + Testcontainers Postgres pattern as every
  other API integration test in this repository, because the Spring context
  requires a real datasource to boot (Flyway runs on startup) even though
  this feature itself performs no database access — the absence of any
  household/entity identifier on the endpoint, plus the identical-response
  test, is the actual evidence of statelessness.

`./verify.sh`: 245 tests, 0 failures (37 in the `runway` package versus the
pre-batch `main` baseline this branch was cut from; see the fix-round entry
above for the 3 tests added in round 1).

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
