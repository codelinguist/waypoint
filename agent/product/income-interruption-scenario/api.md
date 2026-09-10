# API: Income Interruption Scenario

`POST /api/scenarios/income-interruption`

Stateless, disposable calculation. Every input is a caller-supplied temporary
modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and identical requests always
return identical results. Determines the reserve needed to withstand a
caller-defined temporary income interruption by comparing a baseline path
(constant normal income) against a scenario path (income drops for an
explicit interval, then resumes). Uses explicit month indices, not inferred
employment dates. Negative balances are preserved as modeled funding gaps,
not automatic borrowing. This model excludes taxes, benefits, loans, and
interest, and is not a household cash-flow forecast, recommendation, or
approved decision.

## Request

| Field                          | Type    | Rules |
|----------------------------------|---------|-------|
| `currency`                        | string  | Required. 3 letters, case-insensitive; normalized to uppercase in the response. |
| `openingReserve`                  | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits. Shared starting balance for both paths. |
| `normalMonthlyNetIncome`          | decimal | Required. Zero or greater. Same digit/scale limits as `openingReserve`. Income outside the interruption interval. |
| `interruptedMonthlyNetIncome`     | decimal | Required. Zero or greater, and must not exceed `normalMonthlyNetIncome`. Same digit/scale limits. Income during the interruption interval. |
| `monthlyExpenses`                 | decimal | Required. Zero or greater. Same digit/scale limits. Constant every month, shared by both paths. |
| `horizonMonths`                   | integer | Required. Whole number from 1 through 1200 — the projection horizon. A fractional value (e.g. `3.5`) is rejected, not truncated. |
| `interruptionStartMonth`          | integer | Required. Whole number from 1 through `horizonMonths` — the first interrupted month (1-based, inclusive). |
| `interruptionMonths`              | integer | Required. Whole number from 1 through `horizonMonths`. `interruptionStartMonth + interruptionMonths - 1` must not exceed `horizonMonths` — the entire interruption interval must fit inside the horizon. |

## Response

| Field                              | Meaning |
|--------------------------------------|---------|
| `currency`                            | Echoed, normalized to uppercase. |
| `openingReserve`                      | Echoed. |
| `normalMonthlyNetIncome`              | Echoed. |
| `interruptedMonthlyNetIncome`         | Echoed. |
| `monthlyExpenses`                     | Echoed. |
| `horizonMonths`                       | Echoed. |
| `interruptionStartMonth`              | Echoed. |
| `interruptionMonths`                  | Echoed. |
| `baselineRows`                        | Ordered array, one entry per month, at `normalMonthlyNetIncome` throughout; see below. |
| `scenarioRows`                        | Ordered array, one entry per month, with `interruptedMonthlyNetIncome` applied for the inclusive interruption interval and normal income before/after; see below. |
| `closingDeltas`                       | One entry per month: `scenarioRow.closingCash - baselineRow.closingCash`. |
| `endingCash`                          | Scenario closing cash of the final projected month. |
| `minimumCash`                         | The lowest value across `openingReserve` and every scenario closing cash — the minimum cash the household ever holds under the scenario. |
| `firstNegativeMonth`                  | The first month whose scenario closing balance is strictly below zero, or `null` if none is. A closing balance of exactly `0.00` does not count. |
| `additionalOpeningReserveNeeded`      | `max(0, -minimumCash)`: the extra opening reserve, on top of `openingReserve`, that would have kept the scenario path's minimum cash at or above zero. Monthly netting cannot establish intramonth solvency, and any post-interruption recovery income is only an explicit caller assumption, not a confirmed household fact. |

Each entry of `baselineRows` and `scenarioRows`:

| Field           | Meaning |
|------------------|---------|
| `month`           | 1-based month index within the horizon. |
| `openingCash`     | Cash balance at the start of the month. Equals the previous row's `closingCash` within the same path (or the shared `openingReserve` for month 1). |
| `income`          | This path's income for this month (`normalMonthlyNetIncome` for every baseline row; `interruptedMonthlyNetIncome` during the scenario's interruption interval, `normalMonthlyNetIncome` otherwise). |
| `expenses`        | Echoed `monthlyExpenses`. |
| `netCashFlow`     | `income - expenses`, signed. |
| `closingCash`     | `openingCash + income - expenses`. Negative values are valid modeled results, not errors. |

## Examples

### Interruption that exhausts the reserve, with recovery after it ends

Request:

```json
{
  "currency": "usd",
  "openingReserve": "100.00",
  "normalMonthlyNetIncome": "100.00",
  "interruptedMonthlyNetIncome": "0",
  "monthlyExpenses": "80.00",
  "horizonMonths": 5,
  "interruptionStartMonth": 2,
  "interruptionMonths": 2
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "openingReserve": 100.00,
  "normalMonthlyNetIncome": 100.00,
  "interruptedMonthlyNetIncome": 0.00,
  "monthlyExpenses": 80.00,
  "horizonMonths": 5,
  "interruptionStartMonth": 2,
  "interruptionMonths": 2,
  "baselineRows": [
    { "month": 1, "openingCash": 100.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00, "closingCash": 120.00 },
    { "month": 2, "openingCash": 120.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00, "closingCash": 140.00 },
    { "month": 3, "openingCash": 140.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00, "closingCash": 160.00 },
    { "month": 4, "openingCash": 160.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00, "closingCash": 180.00 },
    { "month": 5, "openingCash": 180.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00, "closingCash": 200.00 }
  ],
  "scenarioRows": [
    { "month": 1, "openingCash": 100.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00,  "closingCash": 120.00 },
    { "month": 2, "openingCash": 120.00, "income": 0.00,   "expenses": 80.00, "netCashFlow": -80.00, "closingCash": 40.00  },
    { "month": 3, "openingCash": 40.00,  "income": 0.00,   "expenses": 80.00, "netCashFlow": -80.00, "closingCash": -40.00 },
    { "month": 4, "openingCash": -40.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00,  "closingCash": -20.00 },
    { "month": 5, "openingCash": -20.00, "income": 100.00, "expenses": 80.00, "netCashFlow": 20.00,  "closingCash": 0.00   }
  ],
  "closingDeltas": [0.00, -100.00, -200.00, -200.00, -200.00],
  "endingCash": 0.00,
  "minimumCash": -40.00,
  "firstNegativeMonth": 3,
  "additionalOpeningReserveNeeded": 40.00
}
```

Cash recovers to exactly `0.00` by month 5 once normal income resumes, but
never becomes positive again within this horizon; `additionalOpeningReserveNeeded`
(`40.00`) is the extra starting cash that would have kept month 3 at `0.00`
instead of `-40.00`.

### No-loss interruption (interrupted income equals normal income)

Request:

```json
{
  "currency": "USD",
  "openingReserve": "100.00",
  "normalMonthlyNetIncome": "100.00",
  "interruptedMonthlyNetIncome": "100.00",
  "monthlyExpenses": "80.00",
  "horizonMonths": 3,
  "interruptionStartMonth": 2,
  "interruptionMonths": 2
}
```

Response (`200 OK`): `baselineRows` and `scenarioRows` are identical
month-by-month, every `closingDeltas` entry is `0.00`, `firstNegativeMonth`
is `null`, and `additionalOpeningReserveNeeded` is `0.00`.

### Validation error

Request:

```json
{
  "currency": "USD",
  "openingReserve": "100.00",
  "normalMonthlyNetIncome": "100.00",
  "interruptedMonthlyNetIncome": "100.01",
  "monthlyExpenses": "80.00",
  "horizonMonths": 3,
  "interruptionStartMonth": 2,
  "interruptionMonths": 2
}
```

Response (`400 Bad Request`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "interruptedMonthlyNetIncome must not exceed normalMonthlyNetIncome",
  "details": []
}
```

Other rejected inputs (all `400 VALIDATION_FAILED` unless noted): negative
`openingReserve`, `normalMonthlyNetIncome`, `interruptedMonthlyNetIncome`, or
`monthlyExpenses`; `horizonMonths` of `0`, negative, or above `1200`;
`interruptionStartMonth` or `interruptionMonths` outside `1..horizonMonths`;
an interval where `interruptionStartMonth + interruptionMonths - 1` exceeds
`horizonMonths`; a malformed `currency` code; a missing required field; more
than 17 integer or 2 fraction digits on any amount, including a
negative-scale representation of the same value (e.g. `1E+17`). A fractional
month count (e.g. `3.5`), a month-count value that does not fit in a 32-bit
integer (e.g. `4294967299`, which would otherwise silently narrow to a small
valid value), and a non-JSON body all return `400 MALFORMED_REQUEST` instead,
since they fail before request-object validation runs.

## Manually verified

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against a
throwaway local `postgres:16-alpine` container (`waypoint`/`waypoint`/`waypoint`
on a non-default local port; Flyway applied its 5 existing migrations
unchanged — this feature adds none) on 2026-09-10 via `curl`, and confirmed:

- The acceptance-criterion example (opening reserve 100, normal income 100,
  interrupted income 0, expenses 80, horizon 3, interruption months 2-3) →
  `200`, baseline closes `120.00, 140.00, 160.00`, scenario closes `120.00,
  40.00, -40.00`, `additionalOpeningReserveNeeded: 40.00`,
  `firstNegativeMonth: 3` — matched exactly.
- The no-loss example (interrupted income equals normal income) → `200`,
  identical baseline/scenario closing values, `closingDeltas` all `0.00`,
  `firstNegativeMonth: null`, `additionalOpeningReserveNeeded: 0.00` —
  matched exactly.
- Interrupted income exceeding normal income, an interval extending beyond
  the horizon, negative `openingReserve`, and a malformed `currency` → each
  `400 VALIDATION_FAILED` with a field-specific message.
- A fractional `horizonMonths` (`3.5`), an out-of-32-bit-range
  `interruptionMonths` (`4294967299`), and a non-JSON body → each
  `400 MALFORMED_REQUEST`.
- Two identical requests → byte-identical response bodies.

All responses matched the documented contract above exactly. Stopped the
manual app and the throwaway Postgres container afterward; no container or
process was left running. See `agent/product/income-interruption-scenario/
implementation-log.md` (feature-local, this directory) for the full
verification record.
