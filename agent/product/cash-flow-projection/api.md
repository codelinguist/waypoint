# API: Constant Monthly Cash-Flow Projection

`POST /api/planning/cash-flow-projection`

Stateless, disposable calculation. Every input is a caller-supplied temporary
modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and identical requests always
return identical results. Assumes the same inflow and outflow recur unchanged
every month; this model excludes timing within a month, irregular events,
interest, inflation, and taxes, and is not a household cash-flow forecast,
recommendation, or approved decision.

## Request

| Field            | Type    | Rules |
|-------------------|---------|-------|
| `currency`         | string  | Required. 3 letters, case-insensitive; normalized to uppercase in the response. |
| `startMonth`       | string  | Required. `YYYY-MM` (4-digit year, 2-digit month `01`-`12`). The first projected month. |
| `startingCash`     | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits. |
| `monthlyInflow`    | decimal | Required. Zero or greater. Same digit/scale limits as `startingCash`. Constant every month. |
| `monthlyOutflow`   | decimal | Required. Zero or greater. Same digit/scale limits as `startingCash`. Constant every month. |
| `months`           | integer | Required. Whole number from 1 through 1200 — the projection horizon. A fractional value (e.g. `3.5`) is rejected, not truncated. |

## Response

| Field                       | Meaning |
|------------------------------|---------|
| `currency`                    | Echoed, normalized to uppercase. |
| `startMonth`                  | Echoed. |
| `startingCash`                | Echoed. |
| `monthlyInflow`               | Echoed. |
| `monthlyOutflow`               | Echoed. |
| `months`                      | Echoed. |
| `rows`                        | Ordered array, one entry per projected month; see below. |
| `endingCash`                  | Closing cash of the final projected month. |
| `lowestClosingBalance`        | The lowest closing balance across every projected month. |
| `lowestClosingBalanceMonth`   | The first month (`YYYY-MM`) at which `lowestClosingBalance` occurs, if the same lowest value recurs. |
| `firstNegativeMonth`          | The first month (`YYYY-MM`) whose closing balance is strictly below zero, or `null` if none is. A closing balance of exactly `0.00` does not count. |
| `status`                      | `REMAINS_NONNEGATIVE` if every closing balance stayed at or above zero, otherwise `BECOMES_NEGATIVE`. Because `startingCash` cannot be negative, a `STARTS_NEGATIVE` status is not modeled. |

Each entry of `rows`:

| Field           | Meaning |
|------------------|---------|
| `month`           | `YYYY-MM` for this row, advancing one calendar month per row (correctly rolling over year boundaries) from `startMonth`. |
| `openingCash`     | Cash balance at the start of the month. Equals the previous row's `closingCash` (or `startingCash` for the first row). |
| `inflow`          | Echoed `monthlyInflow`. |
| `outflow`         | Echoed `monthlyOutflow`. |
| `netCashFlow`     | `inflow - outflow`, signed. |
| `closingCash`     | `openingCash + inflow - outflow`. Negative values are valid modeled results, not errors. |

## Examples

### Constant outflow exceeding inflow, becoming negative

Request:

```json
{
  "currency": "usd",
  "startMonth": "2027-01",
  "startingCash": "1000.00",
  "monthlyInflow": "300.00",
  "monthlyOutflow": "500.00",
  "months": 6
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "startMonth": "2027-01",
  "startingCash": 1000.00,
  "monthlyInflow": 300.00,
  "monthlyOutflow": 500.00,
  "months": 6,
  "rows": [
    { "month": "2027-01", "openingCash": 1000.00, "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": 800.00 },
    { "month": "2027-02", "openingCash": 800.00,  "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": 600.00 },
    { "month": "2027-03", "openingCash": 600.00,  "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": 400.00 },
    { "month": "2027-04", "openingCash": 400.00,  "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": 200.00 },
    { "month": "2027-05", "openingCash": 200.00,  "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": 0.00   },
    { "month": "2027-06", "openingCash": 0.00,    "inflow": 300.00, "outflow": 500.00, "netCashFlow": -200.00, "closingCash": -200.00 }
  ],
  "endingCash": -200.00,
  "lowestClosingBalance": -200.00,
  "lowestClosingBalanceMonth": "2027-06",
  "firstNegativeMonth": "2027-06",
  "status": "BECOMES_NEGATIVE"
}
```

`2027-05`'s closing balance of exactly `0.00` is not treated as negative; the
first strictly-negative month is `2027-06`.

### Equal inflow and outflow across a year boundary, remaining non-negative

Request:

```json
{
  "currency": "USD",
  "startMonth": "2026-11",
  "startingCash": "500.00",
  "monthlyInflow": "200.00",
  "monthlyOutflow": "200.00",
  "months": 4
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "startMonth": "2026-11",
  "startingCash": 500.00,
  "monthlyInflow": 200.00,
  "monthlyOutflow": 200.00,
  "months": 4,
  "rows": [
    { "month": "2026-11", "openingCash": 500.00, "inflow": 200.00, "outflow": 200.00, "netCashFlow": 0.00, "closingCash": 500.00 },
    { "month": "2026-12", "openingCash": 500.00, "inflow": 200.00, "outflow": 200.00, "netCashFlow": 0.00, "closingCash": 500.00 },
    { "month": "2027-01", "openingCash": 500.00, "inflow": 200.00, "outflow": 200.00, "netCashFlow": 0.00, "closingCash": 500.00 },
    { "month": "2027-02", "openingCash": 500.00, "inflow": 200.00, "outflow": 200.00, "netCashFlow": 0.00, "closingCash": 500.00 }
  ],
  "endingCash": 500.00,
  "lowestClosingBalance": 500.00,
  "lowestClosingBalanceMonth": "2026-11",
  "firstNegativeMonth": null,
  "status": "REMAINS_NONNEGATIVE"
}
```

Every month ties at the same closing balance, so `lowestClosingBalanceMonth`
reports the first occurrence (`2026-11`), not a later tied month.

### Validation error

Request:

```json
{
  "currency": "USD",
  "startMonth": "2027-13",
  "startingCash": "0",
  "monthlyInflow": "0",
  "monthlyOutflow": "0",
  "months": 1
}
```

Response (`400 Bad Request`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["startMonth: startMonth must be in YYYY-MM format"]
}
```

Other rejected inputs (all `400 VALIDATION_FAILED` unless noted): negative
`startingCash`, `monthlyInflow`, or `monthlyOutflow`; `months` of `0`,
negative, or above `1200`; a malformed `currency` code; a malformed
`startMonth` (wrong digit count, month `00`/`13`+); a missing required field;
more than 17 integer or 2 fraction digits on any amount, including a
negative-scale representation of the same value (e.g. `1E+17`). A fractional
`months` (e.g. `3.5`), a `months` value that does not fit in a 32-bit integer
(e.g. `4294967299`, which would otherwise silently narrow to a small valid
value), and a non-JSON body all return `400 MALFORMED_REQUEST` instead, since
they fail before request-object validation runs.

## Manually verified

Ran the real application (`./mvnw spring-boot:run` from `backend/`) against a
throwaway local `postgres:16-alpine` container (`waypoint`/`waypoint`/`waypoint`
on a non-default local port; Flyway applied its 5 existing migrations
unchanged — this feature adds none) on 2026-09-06 via `curl`, and confirmed:

- The constant-outflow-exceeding-inflow example above → `200`, all six
  closing balances (`800.00, 600.00, 400.00, 200.00, 0.00, -200.00`),
  `firstNegativeMonth: "2027-06"`, `lowestClosingBalance: -200.00`, `status:
  "BECOMES_NEGATIVE"` — matched exactly.
- The equal-inflow-and-outflow year-boundary example above → `200`, every
  closing balance `500.00`, `lowestClosingBalanceMonth: "2026-11"` (first
  occurrence of the tie), `firstNegativeMonth: null`, `status:
  "REMAINS_NONNEGATIVE"` — matched exactly, confirming month labels advance
  correctly from November into the next year.
- Malformed `startMonth` (`2027-13`), negative `startingCash`, `months` above
  `1200`, excessive fraction digits, and a negative-scale digit-limit bypass
  (`1E+17`) → each `400 VALIDATION_FAILED` with a field-specific message.
- Fractional `months` (`3.5`), an out-of-32-bit-range `months`
  (`4294967299`), and a non-JSON body → each `400 MALFORMED_REQUEST`.
- Two identical requests → byte-identical response bodies.

All responses matched the documented contract above exactly. Stopped the
manual app and the throwaway Postgres container afterward; no container or
process was left running. See `agent/product/cash-flow-projection/
implementation-log.md` (feature-local, this directory) for the full
verification record.
