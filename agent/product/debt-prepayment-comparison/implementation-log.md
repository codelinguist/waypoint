# Implementation Log: Debt Prepayment Comparison (Task 017 / WAP-11)

Feature-local implementation record, per this task's shared-prose exception (no edits to
`README.md`, `docs/decisions/decisions.md`, or any shared workflow/template doc).

## Changed

- Added `backend/src/main/java/com/waypoint/scenarios/debtprepayment/`:
  - `DebtPrepaymentComparisonCalculator` — pure, stateless static domain calculation.
    Validates `immediatePrepayment` (non-null, non-negative, at most 17 integer / 2
    fraction digits) independently of transport validation, calls the merged, read-only
    `DebtAmortizationCalculator.calculate` twice (once for the full principal as
    `baseline`, once for `principal - immediatePrepayment` as `scenario`, with identical
    currency/rate/payment), rejects `immediatePrepayment > principal` against the
    baseline's own normalized principal, and only populates
    `lifetimeInterestSaved`/`payoffMonthsSaved`/`lifetimeCashSaved` when both paths reach
    `PAID_OFF` (otherwise all three are `null` and `comparisonUnavailableReason` names the
    blocking path(s)).
  - `DebtPrepaymentComparisonResult` — result record wrapping both reused
    `DebtAmortizationResult`s plus the comparison fields.
  - `InvalidDebtPrepaymentInputException` — domain validation failure. The calculator also
    catches the reused `InvalidDebtAmortizationInputException` from either
    `DebtAmortizationCalculator.calculate` call and rethrows it as this feature's own
    exception type, so the HTTP boundary only ever needs one exception handler.
  - `web/DebtPrepaymentComparisonController` — `POST /api/scenarios/debt-prepayment`.
  - `web/DebtPrepaymentComparisonExceptionHandler` — `@RestControllerAdvice` scoped with
    `assignableTypes = DebtPrepaymentComparisonController.class`, so it cannot intercept
    another controller's exceptions; reuses the shared `ErrorResponse` shape read-only,
    without editing the shared `ApiExceptionHandler`.
  - `web/dto/DebtPrepaymentComparisonRequest`, `web/dto/DebtPrepaymentComparisonResponse`,
    `web/dto/DebtPrepaymentPathResponse`, `web/dto/DebtPrepaymentScheduleRowResponse`. The
    row/path response types are this feature's own (not imports from
    `debtamortization.web.dto`), so this feature depends only on the debt-amortization
    *domain* package (`DebtAmortizationCalculator`, `Result`, `Row`, `Status`,
    `InvalidDebtAmortizationInputException`) and not on its web layer, keeping the two
    features' transport concerns independent.
- Added matching tests under
  `backend/src/test/java/com/waypoint/scenarios/debtprepayment/`.
- Added `agent/product/debt-prepayment-comparison/api.md` with the documented
  request/response contract and worked examples for every modeled combination.

No shared file was edited (`ApiExceptionHandler`, `README.md`, `docs/decisions/decisions.md`,
build/migration files) and no existing `debtamortization` file was touched.

## Tests

- `DebtPrepaymentComparisonCalculatorTest` (16 tests) — pure domain unit tests: the
  brief's exact worked example (`1000.00`/`0`/`300.00`/prepayment `400.00` → baseline
  payoff 4 months, scenario payoff 2 months, `scenarioTotalCashPaid` `1000.00`, zero
  lifetime interest/cash savings, 2 months saved), zero prepayment producing identical
  baseline/scenario paths and zero savings, prepayment equal to principal producing a
  zero-month scenario schedule while `scenarioTotalCashPaid` still counts the upfront
  cash, an interest-bearing fixture reconciled against independently-computed
  `DebtAmortizationCalculator` results (not hand-derived numbers), rejection of negative
  prepayment, prepayment above principal, null prepayment, and excessive
  scale/integer-digit prepayment, propagation of the reused calculator's own invalid
  principal/currency as this feature's exception type, both-`NON_AMORTIZING` and
  both-`HORIZON_LIMIT` combinations suppressing lifetime comparison, a prepayment that
  makes only the scenario amortizing (`NON_AMORTIZING` → amortizing) and only the
  scenario repayable within the horizon (`HORIZON_LIMIT` → `PAID_OFF`), and currency
  normalization.
- `DebtPrepaymentComparisonApiTest` (25 tests) — `@WebMvcTest` slice (real MVC dispatch,
  bean validation, and this feature's exception advice, without a full Spring context or
  Testcontainers/Postgres, matching this feature's no-persistence scope) covering the
  same worked example, prepayment-equal-to-principal, non-amortizing and
  horizon-to-paid-off comparisons at the HTTP boundary, missing/null required fields
  (parameterized over all five fields including `immediatePrepayment`), every rejection
  case (negative/excessive-precision prepayment, prepayment above principal, negative
  principal/rate, zero payment, malformed currency, malformed JSON body), currency
  normalization, and identical-request-identical-response determinism.
- Command: `./mvnw --batch-mode -o test -Dtest=DebtPrepaymentComparisonCalculatorTest,DebtPrepaymentComparisonApiTest`
  → `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.
- Full suite: `./verify.sh` (repository root) → `Tests run: 615, Failures: 0, Errors: 0,
  Skipped: 0` — `BUILD SUCCESS`.

## Manual verification

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against a throwaway
local `postgres:16-alpine` container (`waypoint`/`waypoint`/`waypoint`, mapped to host
port 5433 to avoid colliding with any other running instance; Flyway applied its existing
migrations unchanged — this feature adds none). Exercised via `curl`:

- The brief's exact worked example (`1000.00`/`0`/`300.00`/prepayment `400.00`) → `200`,
  matching the brief's numbers exactly (baseline payoff 4 months, scenario payoff 2
  months, `scenarioTotalCashPaid: 1000.00`, zero lifetime interest/cash savings, 2 months
  saved).
- Zero prepayment (`100.00`/`0.01`/`60.00`/prepayment `0.00`) → `200`, identical
  baseline/scenario paths, zero savings.
- Prepayment equal to principal (`100.00`/`0.01`/`60.00`/prepayment `100.00`) → `200`,
  scenario `PAID_OFF` in 0 months with an empty schedule, `scenarioTotalCashPaid: 100.00`,
  lifetime interest/cash saved `1.41`, 2 months saved.
- Both paths `NON_AMORTIZING` (`1000.00`/`0.01`/`10.00`/prepayment `0.00`) → `200`,
  `comparisonUnavailableReason` naming both paths, all three savings fields absent.
- A prepayment that makes only the scenario repayable
  (`1000.00`/`0.01`/`10.00`/prepayment `700.00`) → `200`, baseline `NON_AMORTIZING`,
  scenario amortizing normally, `comparisonUnavailableReason` naming the baseline.
- Both paths `HORIZON_LIMIT` (`1000000.00`/`0.001`/`1005.00`/prepayment `0.00`) → `200`,
  matching remaining balance/totals from the debt-amortization API guide's own
  `HORIZON_LIMIT` worked example, `comparisonUnavailableReason` naming both paths.
- A prepayment that brings the scenario inside the horizon (same base inputs, prepayment
  `999000.00`) → `200`, baseline still `HORIZON_LIMIT`, scenario `PAID_OFF` in 1 month,
  `scenarioTotalCashPaid: 1000001.00`, `comparisonUnavailableReason` naming the baseline.
- Negative prepayment (`-1.00`) and prepayment above principal (`1000.01` against
  principal `1000.00`) → each `400 VALIDATION_FAILED` with a field-specific message.
- Non-JSON body → `400 MALFORMED_REQUEST`.

All responses matched the documented contract in `api.md` exactly. Full request/response
pairs are recorded there rather than duplicated here. Stopped the manual app and the
throwaway Postgres container afterward; no container or process was left running.

## Decisions

- **Own row/path response DTOs, not imports from `debtamortization.web.dto`.** The
  reused `DebtAmortizationCalculator`/`Result`/`Row`/`Status` domain classes are consumed
  directly (per the task's explicit "consume merged debtamortization classes read-only"
  dependency), but this feature defines its own `DebtPrepaymentPathResponse` and
  `DebtPrepaymentScheduleRowResponse` rather than importing `debtamortization`'s
  controller-facing response types. This keeps each feature's transport layer
  independent and deployable on its own, consistent with the sibling batch's
  independence rationale (each package "has no dependencies on each other" at the web
  layer, only at the shared domain layer explicitly named as reusable).
- **Domain calculator translates the reused calculator's exception, so the HTTP layer
  needs only one exception handler.** `DebtPrepaymentComparisonCalculator` catches
  `InvalidDebtAmortizationInputException` from either `DebtAmortizationCalculator.calculate`
  call and rethrows it as `InvalidDebtPrepaymentInputException`. This avoids duplicating
  `debtamortization`'s own principal/rate/payment/currency validation logic in this
  feature (which would drift from the reused calculator's own rules over time) while
  still keeping this feature's `@RestControllerAdvice` scoped to a single exception type,
  matching the `futurevalue`/`goalcontribution` precedent of one feature-scoped advice
  class per controller.
- **`immediatePrepayment > principal` is checked against the baseline's own normalized
  principal**, not the raw request value, so the comparison is against the same
  2-decimal-scaled value the reused calculator itself uses — the baseline call already
  validates and normalizes `principal` before this check runs, so an invalid `principal`
  is rejected (as a translated `InvalidDebtAmortizationInputException`) before the
  prepayment-vs-principal comparison is ever reached.
- **DTO-level validation duplicates the debt-amortization request's own field
  annotations** for `principal`/`monthlyInterestRate`/`monthlyPayment`/`currency` (rather
  than only relying on the domain-level translated exception), matching every sibling
  feature's convention of full field-level `jakarta.validation` annotations on its own
  request DTO. The one cross-field rule (`immediatePrepayment <= principal`) cannot be
  expressed as a per-field annotation and is enforced only in the domain calculator.
- Reused `com.waypoint.web.ErrorResponse` read-only for the domain validation exception
  handler, matching the shared error-response shape without editing `ApiExceptionHandler`
  itself, per this task's ownership constraint.

## Assumptions

- `comparisonUnavailableReason`'s exact wording is this task's own reversible response
  convention, not a specified contract — the brief requires "explicit
  comparison-unavailable reasons" but does not name their text. Chose a sentence that
  always names which status is at fault (baseline, scenario, or both) so a caller does
  not have to re-derive it from the two `status` fields.
- `DebtPrepaymentPathResponse.startingBalance` (the principal each path actually
  amortizes from) is included as an explicit field beyond the brief's literal wording, so
  a caller can see the scenario's post-prepayment principal without re-deriving
  `principal - immediatePrepayment` itself. This mirrors the debt-amortization
  response's own `remainingBalance` addition (a minor, reversible response-shape choice).
- No household fact, assumption, or approval was required or invented; every input is
  caller-supplied for this disposable comparison only, per the task's ownership and scope
  constraints.

## Open questions

None blocking. Per the brief, lender payoff quotes, fees, variable rates, recurring
extra payments, invest-versus-prepay advice, reserve affordability, liability
lookup/update, UI, and persistence are explicit future/out-of-scope work, not attempted
here.

## Recommended next task

Shared-document consolidation (README/API index) across the debt-amortization,
future-value, goal-contribution, and debt-prepayment-comparison feature-local `api.md`
files, once this batch of scenario/planning primitives has settled — none of these
features currently link to each other from a central index.

## System-evolution candidate

None identified. This task's ownership, dependency, and evidence conventions all matched
existing precedent (`future-value-calculator`, `goal-contribution-calculator`) without
needing a rule or template correction.
