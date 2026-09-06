# Implementation Log: Future-Value Calculator (Task 013)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md` for this batch).

## Changed

- Added `backend/src/main/java/com/waypoint/planning/futurevalue/`:
  - `FutureValueCalculator` — pure, stateless `@Service` domain calculation.
    Validates its own inputs (currency format, money sign/scale/precision,
    `annualRatePercentage` sign/scale/precision, `projectionMonths` range
    1-1200) independent of any transport validation, then runs a
    month-by-month loop: growth accrues on the opening balance at the
    monthly rate (`annualRatePercentage / 1200`, computed with 12 internal
    decimal digits before rounding), is rounded HALF_UP to money scale, and
    the equal monthly contribution is added at month end. Returns
    `endingValue`, `totalContributed`, `totalGrowth`, a human-readable
    `conventions` statement, and the ordered `schedule`.
  - `FutureValueResult`, `FutureValueRow` — result and per-month row
    records.
  - `InvalidFutureValueInputException` — domain validation failure.
  - `web/FutureValueController` — `POST /api/planning/future-value`.
  - `web/FutureValueExceptionHandler` — `@RestControllerAdvice` scoped with
    `assignableTypes = FutureValueController.class` so it cannot intercept
    another controller's exceptions; reuses the shared `ErrorResponse`
    shape read-only, without editing the shared `ApiExceptionHandler`.
  - `web/dto/FutureValueRequest`, `web/dto/FutureValueResponse`,
    `web/dto/FutureValueRowResponse`.
  - `web/dto/WholeNumberDeserializer` — package-scoped copy of the pattern
    used by `goalcontribution`'s deserializer of the same name (see
    Decisions below); not shared/imported across packages, per this task's
    exclusive-ownership constraint.
- Added matching tests under
  `backend/src/test/java/com/waypoint/planning/futurevalue/`.
- Added `agent/product/future-value-calculator/api.md` with the documented
  request/response contract and worked examples.

No shared file was edited (`ApiExceptionHandler`, `README.md`, central
`agent/implementation-log.md`, `docs/decisions/decisions.md`, build/
migration files, and both sibling tasks' packages are all untouched).

## Tests

- `FutureValueCalculatorTest` (24 tests) — pure domain unit tests: the
  brief's exact worked example (starting `1000.00`, contribution `100.00`,
  `12.00%` annual, 2 months → month 1 interest `10.00`/close `1110.00`,
  month 2 interest `11.10`/close `1221.10`, total principal `1200.00`,
  growth `21.10`), zero-rate ending value equals principal plus
  contributions, all-zero money inputs produce a valid zero schedule
  (not rejected), every row reconciling `opening + growth + contribution =
  closing` and final totals reconciling with the last row across an
  arbitrary 24-month run, currency normalization (including under a
  Turkish default locale), and rejection of null/malformed currency,
  null/negative money fields, negative annual rate, excessive fraction/
  integer digits on both money fields and the rate (including the
  negative-scale-representation digit-count bypass, `1E+17`), and
  out-of-range `projectionMonths` (0, negative, 1201). Called directly
  against the calculator (no Spring context).
- `FutureValueApiIntegrationTest` (22 tests) — `@WebMvcTest` slice (real
  MVC dispatch, validation, and exception handling, without a full Spring
  context or Testcontainers/Postgres, since this endpoint has no
  persistence dependency) covering the same success/edge cases through
  HTTP, plus missing-field, malformed-currency, malformed-JSON-body,
  fractional/overflowing `projectionMonths`, and
  identical-request-identical-response checks.
- Command: `./mvnw --batch-mode -Dtest="com.waypoint.planning.futurevalue.**" test`
  → `Tests run: 46, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) →
  `Tests run: 389, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`,
  run against `main` with tasks 012's claim commit already present
  (pre-batch baseline for this worktree), confirming this feature stands
  alone.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`)
against a throwaway local `postgres:16-alpine` container (`waypoint`/
`waypoint`/`waypoint`, mapped to host port 5433 to avoid colliding with any
other running instance; Flyway applied its 5 existing migrations
unchanged — this feature adds none). Exercised via `curl`:

- The brief's exact worked example (`1000.00` / `100.00` / `12.00%` / 2
  months) → `200`, month-by-month `growth`/`closingBalance` and totals
  matching the brief exactly.
- Zero rate (`500.00` / `50.00` / `0%` / 6 months) → `200`,
  `endingValue: 800.00` = `totalContributed`, `totalGrowth: 0.00`.
- All-zero money inputs with a non-zero rate (`0` / `0` / `5.00%` / 3
  months) → `200`, a valid all-zero schedule, confirming this combination
  is not rejected.
- Fractional `projectionMonths` (`3.5`) and an overflowing value
  (`4294967299`) → each `400 MALFORMED_REQUEST` (see Decisions).
- `projectionMonths` of `0` and `1201`, negative `startingPrincipal`,
  excessive fraction digits, a negative-scale digit-limit bypass
  (`1E+17`), excessive `annualRatePercentage` digits (`1000.00`), a
  malformed currency code, and a non-JSON body → each `400
  VALIDATION_FAILED` or `400 MALFORMED_REQUEST` as documented in `api.md`.
- Lowercase currency (`eur`) → `200`, normalized to `EUR`.

All responses matched the documented contract in `api.md` exactly. Full
request/response pairs are recorded there rather than duplicated here.
Stopped the manual app and the throwaway Postgres container afterward; no
container or process was left running.

## Decisions

- **Monthly-rate internal precision.** `annualRatePercentage` is divided by
  `1200` (12 months × 100) with 12 internal decimal digits (`HALF_UP`)
  before use, rather than rounding the rate itself to money scale. Only
  the resulting per-month *growth amount* is rounded HALF_UP to 2 decimals
  before being added to the balance, matching PD-001's explicit convention
  (monetary balances and growth are rounded each month; the rate itself is
  not a monetary balance). This keeps the worked example exact (`12.00% /
  1200 = 0.01` precisely) while avoiding compounding drift for
  non-terminating rate/12 divisions (e.g. `7.25%`).
- **`annualRatePercentage` digit bounds (3 integer / 4 fraction digits).**
  The brief specifies the rate is "explicit" and must reject "excessive
  scale/precision" but does not name a bound. Chose 3 integer digits (caps
  just under 1000%, comfortably above any realistic household return
  assumption) and 4 fraction digits (finer-grained than the 2-decimal
  worked example, e.g. `4.375%`) as a deliberately generous but still
  bounded precision limit — see Assumptions.
- **No special-case for all-zero money inputs.** The brief allows (does
  not require rejecting) `startingPrincipal == 0 && monthlyContribution ==
  0`. Because growth on a zero opening balance is always zero regardless
  of rate, the general month-by-month loop already produces a well-defined
  all-zero schedule without a special early-return branch (unlike, e.g.,
  `DebtAmortizationCalculator`'s zero-principal short-circuit, which was
  unnecessary here).
- **`WholeNumberDeserializer` scoped copy, not a shared import.** Reused
  the same fix already applied to `goalcontribution`'s deserializer of the
  same name (reject non-integral values, and reject values that don't fit
  a 32-bit int via `canConvertToInt()`, closing the truncation/narrowing
  gap Jackson's default `Integer` deserialization has) but as a new,
  package-private class inside `futurevalue.web.dto` rather than importing
  the sibling package's class, per this task's "no sibling import"
  exclusive-ownership constraint.
- **Digit-count formula uses `Math.max(precision - scale, 0)`, and
  currency uppercasing uses `Locale.ROOT`.** Both were written correctly
  from the start (not discovered via review) based on the fix already
  recorded in `agent/product/goal-contribution-calculator/
  implementation-log.md`'s "Fix round 1": a plain `precision - scale`
  undercounts integer digits for a negative-scale representation like
  `1E+17`, and `String.toUpperCase()` without `Locale.ROOT` misbehaves
  under a Turkish default locale. Regression tests for both are included
  in `FutureValueCalculatorTest`.
- **`@WebMvcTest` instead of `@SpringBootTest` + Testcontainers for the
  HTTP-level test**, matching the precedent in
  `GoalContributionApiIntegrationTest`: this endpoint has no persistence
  dependency, so a full Spring context with a real Postgres Testcontainers
  boundary would add cost with no correctness benefit. `@Import({
  FutureValueCalculator.class, FutureValueExceptionHandler.class})`
  registers the real (non-mocked) calculator and controller-advice beans.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain
  validation exception handler, matching the shared error-response shape
  without editing `ApiExceptionHandler` itself (PD-002 in the product
  brief).

## Assumptions

- `annualRatePercentage` bounds (non-negative; at most 3 integer and 4
  fraction digits) are this task's own reversible calculator-convention
  choice, not a household return assumption — consistent with PD-001's
  "reversible calculator convention, not a household return assumption"
  framing for the compounding convention itself. No user input was sought
  per the task's unattended-worker instructions; recorded here for
  Product Owner review instead.
- Interpreted "total contributed principal" (brief) as `startingPrincipal +
  (monthlyContribution * projectionMonths)` — i.e. every dollar the
  caller-supplied inputs describe as contributed, starting balance
  included — since the worked example's `1200.00` total only matches under
  that definition (`1000.00 + 100.00×2`).
- Interpreted the brief's "reject a case where both starting principal and
  contribution are zero only if the result remains well-defined — prefer
  returning a valid zero projection" as: do not add a special-case
  rejection or short-circuit at all, since the general calculation is
  already well-defined for that input (see Decisions).
- No household fact, assumption, or approval was required or invented;
  every input is caller-supplied for this disposable calculation only, per
  the task's ownership and scope constraints.

## Open questions

None blocking. Per the brief, inflation, taxes, fees, irregular
contributions, and probabilistic/variable returns are explicit future
models, not attempted here.

## Recommended next task

Follow-up framing (Codex) to connect this calculation to a real household
goal or plan: given a household's stated savings rate and a chosen return
assumption, surface the same projection against an actual `FinancialGoal`
or plan horizon — with an explicit decision on how the model result is
distinguished from a persisted plan or recommendation, per the facts/
assumptions/goals/recommendations/decisions distinction in
`docs/product/problems.md`, and on whether a second (real, non-nominal)
rate-quoting convention is ever needed alongside this one.

## System-evolution candidate

Not proposing a change to `AGENTS.md` or a template from this task. The
negative-scale digit-count bug and locale-dependent uppercasing were both
already documented in a sibling task's implementation log
(`goal-contribution-calculator`) from an earlier review round, and this
task's calculator applied both fixes from the start rather than
rediscovering them — worth noting as a case where a fix recorded in one
feature's implementation log successfully prevented a repeat defect in a
parallel task, without needing a shared-code or `AGENTS.md` change to do
so.
