# Implementation Log: Constant Monthly Cash-Flow Projection (Task 014)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md` for this batch).

## Changed

- Added `backend/src/main/java/com/waypoint/planning/cashflow/`:
  - `CashFlowProjectionCalculator` — pure, stateless domain calculation.
    Validates its own inputs (currency format, money sign/scale/precision,
    non-null `startMonth`, `months` range 1-1200) independent of any
    transport validation, then walks the horizon one month at a time,
    computing `openingCash`, `netCashFlow` (`inflow - outflow`), and
    `closingCash` (`opening + inflow - outflow`), tracking the first
    occurrence of the lowest closing balance and the first strictly-negative
    closing balance.
  - `CashFlowProjectionRow` — one projected month (dated via `YearMonth`).
  - `CashFlowProjectionResult` — result record (echoed inputs, ordered rows,
    `endingCash`, `lowestClosingBalance`/month, nullable
    `firstNegativeMonth`, `status`).
  - `CashFlowProjectionStatus` — `REMAINS_NONNEGATIVE` / `BECOMES_NEGATIVE`
    only; see Decisions below for why `STARTS_NEGATIVE` is omitted.
  - `InvalidCashFlowProjectionInputException` — domain validation failure.
  - `web/CashFlowProjectionController` — `POST
    /api/planning/cash-flow-projection`.
  - `web/CashFlowProjectionExceptionHandler` — `@RestControllerAdvice`
    scoped with `assignableTypes = CashFlowProjectionController.class` so it
    cannot intercept another controller's exceptions; reuses the shared
    `ErrorResponse` shape read-only, without editing the shared
    `ApiExceptionHandler`.
  - `web/dto/CashFlowProjectionRequest`, `web/dto/CashFlowProjectionResponse`,
    `web/dto/CashFlowProjectionRowResponse`.
  - `web/dto/WholeNumberDeserializer` — see Decisions below.
- Added matching tests under
  `backend/src/test/java/com/waypoint/planning/cashflow/`.
- Added `agent/product/cash-flow-projection/api.md` with the documented
  request/response contract and worked examples.

No shared file was edited (`ApiExceptionHandler`, `README.md`, central
`agent/implementation-log.md`, `docs/decisions/decisions.md`, build/
migration files, and every sibling task's package are all untouched — this
package imports nothing from `debtamortization`, `goalcontribution`, or
`runway`, and vice versa).

## Tests

- `CashFlowProjectionCalculatorTest` (28 tests) — pure domain unit tests:
  the acceptance criterion's worked example (starting cash 1000.00, inflow
  300.00, outflow 500.00, 6 months → closes 800/600/400/200/0/-200, first
  negative June 2027, lowest -200 in June); equal inflow/outflow preserving
  the balance; positive net flow increasing it; zero starting cash with zero
  flows; per-row reconciliation and opening-equals-previous-closing; a
  year-boundary month-label rollover (Nov 2026 → Feb 2027); first-occurrence-
  wins for a tied lowest balance; a `0.00` closing balance not counted as
  "first negative"; only the first negative month reported when several
  months go negative; currency normalization (including under a Turkish
  default locale); no truncation of a derived balance; rejection of
  null/malformed currency, null start month, null/negative money inputs,
  excessive fraction/integer digits (including the negative-scale
  representation bypass), and out-of-range `months` (0, negative, 1201);
  bounds acceptance at 1 and 1200 months; identical-inputs-identical-results.
  Called directly against the calculator (no Spring context).
- `CashFlowProjectionApiIntegrationTest` (21 tests) — `@WebMvcTest` slice
  (real MVC dispatch, validation, and exception handling, without a full
  Spring context or Testcontainers/Postgres, since this endpoint has no
  persistence dependency) covering the same success/edge cases through
  HTTP, plus missing-field, malformed-`startMonth`, malformed-JSON-body,
  and identical-request-identical-response checks.
- Command: `./mvnw --batch-mode -Dtest="com.waypoint.planning.cashflow.**" test`
  → `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) →
  `Tests run: 392, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against a
throwaway local `postgres:16-alpine` Docker container (`waypoint`/`waypoint`/
`waypoint`, mapped to a non-default local port to avoid colliding with any
other running instance; Flyway applied its 5 existing migrations unchanged —
this feature adds none). Exercised via `curl`:

- The becomes-negative example (`1000.00` starting cash, `300.00` inflow,
  `500.00` outflow, 6 months) → `200`, all six closing balances exactly as
  specified, `firstNegativeMonth: "2027-06"`, `lowestClosingBalance:
  -200.00`, `status: "BECOMES_NEGATIVE"`.
- The equal-inflow/outflow year-boundary example (`500.00` starting cash,
  `200.00`/`200.00`, starting `2026-11`, 4 months) → `200`, every closing
  balance `500.00`, `lowestClosingBalanceMonth: "2026-11"` (first tie),
  `firstNegativeMonth: null`, `status: "REMAINS_NONNEGATIVE"`, month labels
  rolling `2026-11 → 2026-12 → 2027-01 → 2027-02` correctly.
- Malformed `startMonth` (`2027-13`), negative `startingCash`, `months`
  above `1200`, excessive fraction digits, and a negative-scale digit-limit
  bypass (`1E+17`) → each `400 VALIDATION_FAILED` with a field-specific
  message.
- Fractional `months` (`3.5`) and an out-of-32-bit-range `months`
  (`4294967299`) → each `400 MALFORMED_REQUEST` (see Decisions).
- A non-JSON body → `400 MALFORMED_REQUEST`.
- Two identical requests → byte-identical response bodies.

All responses matched the documented contract in `api.md` exactly. Full
request/response pairs are recorded there rather than duplicated here.
Stopped the manual app and the throwaway Postgres container afterward; no
container or process was left running.

## Decisions

- **Two-status model (`REMAINS_NONNEGATIVE` / `BECOMES_NEGATIVE`), no
  `STARTS_NEGATIVE`.** The brief requires non-negative `startingCash`
  (enforced at both the domain and HTTP boundaries), so a starting-negative
  outcome can never occur; adding a status value that can never be produced
  would be dead code and a misleading contract. Matches the brief's explicit
  instruction to use only these two statuses under this constraint.
- **No rounding is ever applied to a derived balance.** Every monetary input
  is validated to at most 2 decimal places before use, and every derived
  value (`netCashFlow`, `closingCash`) comes only from addition and
  subtraction of already-2-decimal `BigDecimal` values, which preserves
  scale exactly. Unlike `DebtAmortizationCalculator` (which must round
  interest, a multiplication result, before applying a payment), this
  calculator never needs a `RoundingMode` for a derived value — only
  `RoundingMode.UNNECESSARY` when normalizing an already-valid input's
  scale (e.g. padding `1000` to `1000.00`), which cannot lose information
  because the scale was already checked to be at most 2.
  "Overflow-safe" is satisfied by using `BigDecimal` throughout: unlike a
  primitive numeric type, it does not overflow across up to 1200 additions,
  and no result is silently clamped or wrapped.
- **`YearMonth` as the domain type for month labels**, not a raw string.
  `startMonth.plusMonths(index)` correctly advances across year boundaries
  (December → January) for free, using the JDK's own calendar arithmetic
  instead of hand-rolled month/year increment logic that would need its own
  correctness argument. The HTTP boundary still validates the raw
  `YYYY-MM` string with a regex before the controller parses it with
  `YearMonth.parse`, so a malformed value is rejected before it ever
  reaches the domain layer.
- **Fractional/overflowing `months` rejection (`WholeNumberDeserializer`).**
  Same problem and same fix as Task 011's `contributionMonths`: Jackson's
  default behavior for a JSON number deserialized into an `Integer` field
  truncates toward zero and silently narrows an out-of-32-bit-range value,
  which would silently accept an invalid `months` input. Fixed with a
  `@JsonDeserialize`-scoped deserializer requiring both
  `isIntegralNumber()` and `canConvertToInt()`. This task's package cannot
  import Task 011's `goalcontribution.web.dto.WholeNumberDeserializer`
  (exclusive-ownership / no-sibling-import constraint), so the small class
  is duplicated here rather than shared — see the System-evolution
  candidate below.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain
  validation exception handler, matching the shared error-response shape
  without editing `ApiExceptionHandler` itself (PD-002 in the product
  brief).

## Assumptions

- Interpreted "lowest closing balance" (brief) as computed only over the
  projected `closingCash` values, not including `startingCash` itself
  (which is an opening balance, not a closing one) — moot in practice since
  `startingCash` is enforced non-negative and can never be the true minimum
  unless a later closing balance ties it, which the first-occurrence rule
  already handles correctly (see the equal-inflow/outflow test).
- Treated a closing balance of exactly `0.00` as not negative for both
  `firstNegativeMonth` and `status`, per the brief's explicit "first
  negative means strictly below zero, not zero."
- No household fact, assumption, or approval was required or invented;
  every input is caller-supplied for this disposable calculation only, per
  the task's ownership and scope constraints.

## Open questions

None blocking. Per the brief, variable/irregular schedules, inflation,
multiple currencies, and integration with stored income/obligation records
or the future Scenario Engine are explicit follow-ups requiring their own
product framing — not attempted here.

## Recommended next task

Follow-up framing (Codex) to connect this calculation to real household
state: given a household's stored income streams and obligations, derive
`monthlyInflow`/`monthlyOutflow` (or a richer per-item schedule) instead of
caller-supplied aggregates, with an explicit decision on how the result is
distinguished from a persisted plan, forecast, or recommendation, per the
facts/assumptions/goals/recommendations/decisions distinction in
`docs/product/problems.md`.

## System-evolution candidate

Not proposing a change to `AGENTS.md` or a template from this task. This is
the second sibling-isolated feature (after Task 011) to duplicate the same
small `WholeNumberDeserializer` utility because of the exclusive-ownership/
no-sibling-import constraint that made this batch's parallel execution safe.
Flagging for whoever plans the next batch: if a third bounded-whole-number
HTTP field shows up, a shared `com.waypoint.web` Jackson utility (reviewed
and merged outside any single task's worktree, the same way
`ApiExceptionHandler`/`ErrorResponse` already are) would remove the
duplication without reintroducing a cross-task edit conflict — but two
occurrences isn't yet enough evidence to justify that shared-file edit now.
