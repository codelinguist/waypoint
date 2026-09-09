# Implementation Log: Income Interruption Scenario (WAP-9)

Feature-local implementation record, per this task's exclusive-ownership
constraint (no edits to the central `agent/implementation-log.md` for this
feature).

## Changed

- Added `backend/src/main/java/com/waypoint/scenarios/incomeinterruption/`:
  - `IncomeInterruptionScenarioCalculator` — pure, stateless domain
    calculation. Validates its own inputs (currency format, money
    sign/scale/precision, `interruptedMonthlyNetIncome <=
    normalMonthlyNetIncome`, `horizonMonths` range 1-1200,
    `interruptionStartMonth`/`interruptionMonths` within `1..horizonMonths`,
    and the interruption interval fitting inside the horizon) independent of
    any transport validation, then walks the horizon one month at a time for
    two parallel paths (baseline at constant normal income; scenario with
    interrupted income substituted for the inclusive interruption interval),
    computing `openingCash`, `netCashFlow` (`income - expenses`), and
    `closingCash` for each, tracking the minimum scenario cash (including
    the shared opening reserve) and the first strictly-negative scenario
    closing month.
  - `IncomeInterruptionScenarioRow` — one month of either path.
  - `IncomeInterruptionScenarioResult` — result record (echoed inputs,
    ordered `baselineRows`/`scenarioRows`, per-month `closingDeltas`,
    `endingCash`, `minimumCash`, nullable `firstNegativeMonth`,
    `additionalOpeningReserveNeeded`).
  - `InvalidIncomeInterruptionScenarioInputException` — domain validation
    failure.
  - `web/IncomeInterruptionScenarioController` — `POST
    /api/scenarios/income-interruption`.
  - `web/IncomeInterruptionScenarioExceptionHandler` —
    `@RestControllerAdvice` scoped with `assignableTypes =
    IncomeInterruptionScenarioController.class` so it cannot intercept
    another controller's exceptions; reuses the shared `ErrorResponse` shape
    read-only, without editing the shared `ApiExceptionHandler`.
  - `web/dto/IncomeInterruptionScenarioRequest`,
    `web/dto/IncomeInterruptionScenarioResponse`,
    `web/dto/IncomeInterruptionScenarioRowResponse`.
  - `web/dto/WholeNumberDeserializer` — see Decisions below.
- Added matching tests under
  `backend/src/test/java/com/waypoint/scenarios/incomeinterruption/`.
- Added `agent/product/income-interruption-scenario/api.md` with the
  documented request/response contract and worked examples.

No shared file was edited (`ApiExceptionHandler`, `README.md`, central
`agent/implementation-log.md`, `docs/decisions/decisions.md`, build/
migration files are all untouched). This package imports nothing from
`planning/cashflow` or any other sibling package, per the Jira issue's
exclusive-ownership constraint and its explicit note that this is a bounded
variable-income scenario, not a second constant-input cash-flow projection —
so the two-path monthly walk is implemented independently rather than
composed from `CashFlowProjectionCalculator`, even though that package is
now merged to `main`.

## Tests

- `IncomeInterruptionScenarioCalculatorTest` (30 tests) — pure domain unit
  tests: the acceptance criterion's worked example (opening reserve 100,
  normal income 100, interrupted income 0, expenses 80, horizon 3,
  interruption months 2-3 → baseline closes 120/140/160, scenario closes
  120/40/-40, extra reserve needed 40, first negative month 3); a no-loss
  interruption (interrupted income equals normal income) yielding identical
  paths and zero deltas; interruption starting at month 1 and ending exactly
  at the horizon; a zero closing balance not counted as negative; per-row
  reconciliation and opening-equals-previous-closing within each path;
  recovery after the interruption ends (income resumes, cash partially
  recovers); `minimumCash` including the opening reserve when the scenario
  never dips below it; first-occurrence-wins for the first negative month;
  currency normalization under a Turkish default locale; no truncation of a
  derived balance; rejection of null/malformed currency, null/negative money
  inputs, interrupted income exceeding normal income, excessive
  fraction/integer digits (including the negative-scale representation
  bypass), out-of-range `horizonMonths` (0, 1201),
  `interruptionStartMonth`/`interruptionMonths` outside `1..horizonMonths`,
  and an interval that extends past the horizon; bounds acceptance at 1 and
  1200 months; identical-inputs-identical-results. Called directly against
  the calculator (no Spring context).
- `IncomeInterruptionScenarioApiIntegrationTest` (22 tests) — `@WebMvcTest`
  slice (real MVC dispatch, validation, and exception handling, without a
  full Spring context or Testcontainers/Postgres, since this endpoint has no
  persistence dependency) covering the same success/edge cases through
  HTTP, plus missing-field, malformed-JSON-body, and
  identical-request-identical-response checks.
- Command: `./mvnw --batch-mode -Dtest="com.waypoint.scenarios.incomeinterruption.**" test`
  → `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) →
  `Tests run: 479, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against a
throwaway local `postgres:16-alpine` Docker container (`waypoint`/`waypoint`/
`waypoint`, mapped to a non-default local port to avoid colliding with any
other running instance; Flyway applied its 5 existing migrations unchanged —
this feature adds none). Exercised via `curl`:

- The acceptance-criterion example → `200`, baseline closes exactly
  `120.00, 140.00, 160.00`, scenario closes exactly `120.00, 40.00, -40.00`,
  `additionalOpeningReserveNeeded: 40.00`, `firstNegativeMonth: 3`,
  `minimumCash: -40.00`, `endingCash: -40.00`.
- A no-loss interruption (interrupted income equals normal income) → `200`,
  identical baseline/scenario closing values, `closingDeltas` all `0.00`,
  `firstNegativeMonth: null`, `additionalOpeningReserveNeeded: 0.00`.
- Interrupted income exceeding normal income, an interval extending beyond
  the horizon (`interruptionStartMonth: 3, interruptionMonths: 2` against
  `horizonMonths: 3`), negative `openingReserve`, and a malformed `currency`
  → each `400 VALIDATION_FAILED` with a field-specific message.
- A fractional `horizonMonths` (`3.5`), an out-of-32-bit-range
  `interruptionMonths` (`4294967299`), and a non-JSON body → each
  `400 MALFORMED_REQUEST`.
- Two identical requests → byte-identical response bodies.

All responses matched the documented contract in `api.md` exactly. Full
request/response pairs are recorded there rather than duplicated here.
Stopped the manual app and the throwaway Postgres container afterward; no
container or process was left running.

## Decisions

- **Two independent monthly paths from a shared opening reserve and
  expenses**, rather than computing the scenario as an adjustment on top of
  the baseline. Both paths are simple enough (constant net flow, or one
  substitution interval) that walking them separately in the same loop is
  clearer than deriving one from the other, and it makes each row's
  `openingCash + income - expenses = closingCash` reconciliation trivially
  checkable per path.
- **`minimumCash` includes the opening reserve as a candidate**, not just
  the scenario closing values, per the brief's explicit "minimum cash
  including opening cash." This matters when the scenario's net flow never
  goes negative relative to the reserve: the true minimum the household ever
  holds is the reserve itself (before any monthly activity), not necessarily
  a later closing value. Covered by
  `minimumCashIncludesOpeningReserveWhenTheScenarioNeverDipsBelowIt`.
  `additionalOpeningReserveNeeded` still comes out to `0.00` in that case
  since `minimumCash` is non-negative.
- **No standalone `status` enum**, unlike `CashFlowProjectionStatus`. The
  brief's acceptance criteria ask only for `firstNegativeMonth` (nullable)
  and `additionalOpeningReserveNeeded`; a status value duplicating
  "`firstNegativeMonth != null`" would be an unrequested, redundant field.
- **No rounding is ever applied to a derived balance**, matching
  `CashFlowProjectionCalculator`'s convention: every monetary input is
  validated to at most 2 decimal places before use, and every derived value
  (`netCashFlow`, `closingCash`, `closingDeltas`) comes only from addition
  and subtraction of already-2-decimal `BigDecimal` values, which preserves
  scale exactly. `RoundingMode.UNNECESSARY` is used only when normalizing an
  already-valid input's scale, which cannot lose information because the
  scale was already checked to be at most 2. `BigDecimal` throughout means
  no overflow across up to 1200 months, unlike a primitive numeric type.
- **Explicit 1-based month indices (`int`), not a dated type.** The Jira
  issue explicitly calls for "explicit month indices, not inferred
  employment dates" — unlike `CashFlowProjectionCalculator`'s `YearMonth`,
  there is no calendar date in this contract at all, so no date parsing or
  formatting is needed at either boundary.
- **Fractional/overflowing month-count rejection
  (`WholeNumberDeserializer`)**, applied to all three integer fields
  (`horizonMonths`, `interruptionStartMonth`, `interruptionMonths`). Same
  problem and same fix as `CashFlowProjectionCalculator`'s `months` field:
  Jackson's default `Integer` deserialization truncates a fractional JSON
  number toward zero and silently narrows an out-of-32-bit-range value. This
  package cannot import a sibling package's copy (exclusive-ownership / no-
  sibling-import constraint), so the small class is duplicated here — this
  is now the third occurrence in the codebase (after `goalcontribution` and
  `cashflow`); see the System-evolution note below.
- **Cross-field checks (`interruptedMonthlyNetIncome <=
  normalMonthlyNetIncome`; the interruption interval fitting inside the
  horizon) are enforced only in the domain calculator, not as a Bean
  Validation annotation.** Per-field HTTP annotations
  (`@NotNull`/`@DecimalMin`/`@Digits`/`@Min`/`@Max`) cover every
  independently-checkable field; `interruptionStartMonth` and
  `interruptionMonths` are each annotated `1..1200` at the HTTP layer as a
  structural bound, with the tighter `1..horizonMonths` and interval-fits
  checks left to the domain calculator the controller always calls. This
  matches the existing precedent in `GoalContributionCalculator` (its
  `currentAmount >= targetAmount` branch is domain-only) and still rejects
  every such case at the full HTTP boundary, since the controller invokes
  the calculator before returning a response.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain
  validation exception handler, matching the shared error-response shape
  without editing `ApiExceptionHandler` itself.

## Assumptions

- Interpreted "ending cash" and "minimum cash" (Jira issue scope) as
  referring to the scenario path specifically, not the baseline: this
  endpoint's purpose is sizing the reserve needed for the interruption
  scenario, and `additionalOpeningReserveNeeded`'s formula
  (`max(0, -minimumScenarioCash)`) in the issue text confirms `minimumCash`
  means the scenario path's minimum. The baseline path's own ending/minimum
  values are still fully recoverable from `baselineRows` if ever needed.
- Treated a closing balance of exactly `0.00` as not negative for
  `firstNegativeMonth`, per the acceptance criterion's explicit "zero cash
  is not negative."
- No household fact, assumption, or approval was required or invented;
  every input is caller-supplied for this disposable calculation only, per
  the issue's scope and constraints.

## Open questions

None blocking. Per the product brief, persisted scenarios, automatic
household-state cloning, employment lookup, benefits, taxes, loans,
interest, goal-delay claims, recommendations, UI, and canonical writes are
explicit follow-ups requiring their own product framing — not attempted
here.

## Recommended next task

Follow-up framing (Codex) to connect this calculation to real household
state: given a household's stored income streams and obligations, derive
`normalMonthlyNetIncome`/`monthlyExpenses` (or a richer per-item schedule)
instead of caller-supplied aggregates, with an explicit decision on how the
result is distinguished from a persisted plan, forecast, or recommendation,
per the facts/assumptions/goals/recommendations/decisions distinction in
`docs/product/problems.md`.

## System-evolution candidate

Not proposing a change to `AGENTS.md` or a template from this task. This is
now the third sibling-isolated feature (after `goalcontribution`'s
`contributionMonths` and `cashflow`'s `months`) to duplicate the same small
`WholeNumberDeserializer` utility because of the exclusive-ownership/
no-sibling-import constraint that keeps parallel feature work safe. Three
occurrences is the threshold the `cashflow` implementation log flagged as
worth reconsidering: a shared `com.waypoint.web` Jackson utility (reviewed
and merged outside any single task's worktree, the same way
`ApiExceptionHandler`/`ErrorResponse` already are) would remove the
duplication without reintroducing a cross-task edit conflict, now that a
third occurrence has actually shown up.
