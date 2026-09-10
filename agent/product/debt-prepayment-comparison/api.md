# API: Debt Prepayment Comparison

`POST /api/scenarios/debt-prepayment`

Stateless comparison of an explicit immediate principal prepayment against continuing the
same fixed monthly payment on the un-prepaid balance. Every input is a temporary,
caller-supplied modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and the request body is never logged.
Identical requests always return identical results.

This reuses the merged, read-only `DebtAmortizationCalculator` twice — once for the
un-prepaid `baseline`, once for the prepaid `scenario` — with identical currency, monthly
interest rate, and monthly payment. The prepayment is applied before the first month's
interest accrues; no fees or penalties are modeled. See
[`agent/product/debt-amortization/api.md`](../debt-amortization/api.md) for the reused
amortization model conventions (opening-balance interest, HALF_UP rounding, 1200-month
horizon).

## Request

```json
{
  "principal": "1000.00",
  "monthlyInterestRate": "0",
  "monthlyPayment": "300.00",
  "currency": "USD",
  "immediatePrepayment": "400.00"
}
```

| Field | Type | Constraints |
| --- | --- | --- |
| `principal` | decimal string | required, `>= 0`, at most 17 integer digits and 2 fractional digits |
| `monthlyInterestRate` | decimal string | required, `0` to `1` inclusive, at most 8 fractional digits. Explicit **monthly** rate — never inferred or converted from an annual rate. |
| `monthlyPayment` | decimal string | required, `> 0`, at most 17 integer digits and 2 fractional digits |
| `currency` | string | required, 3 alphabetic letters, normalized to uppercase; no currency conversion |
| `immediatePrepayment` | decimal string | required, `>= 0`, no greater than `principal`, at most 17 integer digits and 2 fractional digits |

Null, missing, malformed, out-of-range, or excessive-scale/precision values are rejected
with `400 VALIDATION_FAILED` (or `400 MALFORMED_REQUEST` for unparseable JSON). Invalid
values are never silently rounded or coerced. `immediatePrepayment` exceeding `principal`
is rejected the same way, even though each field independently passes its own bound.

## Response

| Field | Meaning |
| --- | --- |
| `principal`, `monthlyInterestRate`, `monthlyPayment`, `currency`, `immediatePrepayment` | Echoed, normalized inputs. |
| `baseline` | The un-prepaid path: continuing the same fixed monthly payment on the full principal. |
| `scenario` | The prepaid path: the same fixed monthly payment on `principal - immediatePrepayment`. |
| `baseline.startingBalance` / `scenario.startingBalance` | The principal each path amortizes from. |
| `baseline.status` / `scenario.status` | `PAID_OFF`, `NON_AMORTIZING`, or `HORIZON_LIMIT` — see the debt-amortization API guide. |
| `baseline.schedule` / `scenario.schedule` | Ordered monthly rows for that path (`month`, `openingBalance`, `interest`, `payment`, `principalRepaid`, `closingBalance`). |
| `scenarioTotalCashPaid` | `immediatePrepayment + scenario.totalPaid` — the scenario's full cash outlay, upfront prepayment included. |
| `lifetimeInterestSaved` | `baseline.totalInterest - scenario.totalInterest`. Only present when both paths reach `PAID_OFF`. |
| `payoffMonthsSaved` | `baseline.payoffMonths - scenario.payoffMonths`. Only present when both paths reach `PAID_OFF`. |
| `lifetimeCashSaved` | `baseline.totalPaid - scenarioTotalCashPaid`. Only present when both paths reach `PAID_OFF`. |
| `comparisonUnavailableReason` | `null` when both paths reach `PAID_OFF`; otherwise a human-readable reason naming which path(s) do not, and `lifetimeInterestSaved` / `payoffMonthsSaved` / `lifetimeCashSaved` are all `null`. A truncated `HORIZON_LIMIT` total or an absent `NON_AMORTIZING` total is never compared against the other path's lifetime total. |

## Examples

### Worked example (zero interest)

Request: `principal=1000.00`, `monthlyInterestRate=0`, `monthlyPayment=300.00`,
`immediatePrepayment=400.00`, `currency=USD`.

```json
{
  "principal": 1000.00,
  "monthlyInterestRate": 0,
  "monthlyPayment": 300.00,
  "currency": "USD",
  "immediatePrepayment": 400.00,
  "baseline": {
    "startingBalance": 1000.00,
    "status": "PAID_OFF",
    "payoffMonths": 4,
    "totalPaid": 1000.00,
    "totalInterest": 0.00,
    "remainingBalance": 0.00,
    "schedule": ["... 4 rows ..."]
  },
  "scenario": {
    "startingBalance": 600.00,
    "status": "PAID_OFF",
    "payoffMonths": 2,
    "totalPaid": 600.00,
    "totalInterest": 0.00,
    "remainingBalance": 0.00,
    "schedule": ["... 2 rows ..."]
  },
  "scenarioTotalCashPaid": 1000.00,
  "lifetimeInterestSaved": 0.00,
  "payoffMonthsSaved": 2,
  "lifetimeCashSaved": 0.00,
  "comparisonUnavailableReason": null
}
```

### Zero prepayment (identical paths)

Request: `principal=100.00`, `monthlyInterestRate=0.01`, `monthlyPayment=60.00`,
`immediatePrepayment=0.00`. `baseline` and `scenario` are identical (`payoffMonths: 2`,
`totalPaid: 101.41`, `totalInterest: 1.41`); `scenarioTotalCashPaid` equals
`baseline.totalPaid`, and all three savings fields are `0.00`.

### Prepayment equal to principal (upfront cash retained in totals)

Request: `principal=100.00`, `monthlyInterestRate=0.01`, `monthlyPayment=60.00`,
`immediatePrepayment=100.00`.

```json
{
  "baseline": {
    "startingBalance": 100.00,
    "status": "PAID_OFF",
    "payoffMonths": 2,
    "totalPaid": 101.41,
    "totalInterest": 1.41
  },
  "scenario": {
    "startingBalance": 0.00,
    "status": "PAID_OFF",
    "payoffMonths": 0,
    "totalPaid": 0.00,
    "totalInterest": 0.00,
    "schedule": []
  },
  "scenarioTotalCashPaid": 100.00,
  "lifetimeInterestSaved": 1.41,
  "payoffMonthsSaved": 2,
  "lifetimeCashSaved": 1.41,
  "comparisonUnavailableReason": null
}
```

The scenario reaches `PAID_OFF` in zero schedule months (the reused calculator's
zero-principal short-circuit), but `scenarioTotalCashPaid` still counts the upfront
`immediatePrepayment` — the caller's cash outlay is never dropped from the comparison.

### Both paths non-amortizing (comparison unavailable)

Request: `principal=1000.00`, `monthlyInterestRate=0.01`, `monthlyPayment=10.00`,
`immediatePrepayment=0.00` (payment does not exceed the first month's interest on either
principal).

```json
{
  "baseline": { "status": "NON_AMORTIZING", "payoffMonths": null, "totalPaid": 0.00, "remainingBalance": 1000.00, "schedule": [] },
  "scenario": { "status": "NON_AMORTIZING", "payoffMonths": null, "totalPaid": 0.00, "remainingBalance": 1000.00, "schedule": [] },
  "scenarioTotalCashPaid": 0.00,
  "lifetimeInterestSaved": null,
  "payoffMonthsSaved": null,
  "lifetimeCashSaved": null,
  "comparisonUnavailableReason": "Neither the baseline (NON_AMORTIZING) nor the scenario (NON_AMORTIZING) path reaches PAID_OFF, so lifetime savings cannot be compared."
}
```

### A prepayment that makes only the scenario repayable

Request: `principal=1000.00`, `monthlyInterestRate=0.01`, `monthlyPayment=10.00`,
`immediatePrepayment=700.00`. The baseline's first-month interest on `1000.00` is `10.00`
(equal to the payment, so `NON_AMORTIZING`); the scenario's first-month interest on the
reduced `300.00` principal is `3.00` (below the payment, so it amortizes normally and
eventually reaches `PAID_OFF`). `comparisonUnavailableReason` still names the baseline as
the blocking path, and all three savings fields remain `null` — a real (scenario-only)
payoff is never compared against a baseline that never pays off.

### Both paths at the horizon limit

Request: `principal=1000000.00`, `monthlyInterestRate=0.001`, `monthlyPayment=1005.00`,
`immediatePrepayment=0.00` — the same combination that is `HORIZON_LIMIT` in the
debt-amortization API guide.

```json
{
  "baseline": { "status": "HORIZON_LIMIT", "payoffMonths": null, "remainingBalance": 988409.00, "totalPaid": 1206000.00, "totalInterest": 1194409.00 },
  "scenario": { "status": "HORIZON_LIMIT", "payoffMonths": null, "remainingBalance": 988409.00 },
  "comparisonUnavailableReason": "Neither the baseline (HORIZON_LIMIT) nor the scenario (HORIZON_LIMIT) path reaches PAID_OFF, so lifetime savings cannot be compared."
}
```

### A prepayment that brings the scenario inside the horizon

Same inputs, `immediatePrepayment=999000.00` (scenario principal `1000.00`): the baseline
remains `HORIZON_LIMIT` (`remainingBalance: 988409.00`), but the scenario pays off in 1
month (`totalPaid: 1001.00`). `scenarioTotalCashPaid` is `1000001.00`
(`999000.00 + 1001.00`), and `comparisonUnavailableReason` names the baseline as the
blocking (`HORIZON_LIMIT`) path — the scenario's real payoff is never compared against the
baseline's truncated partial total.

### Rejected: prepayment above principal

Request: `principal=1000.00`, `immediatePrepayment=1000.01` (other fields valid).

```json
{ "error": "VALIDATION_FAILED", "message": "immediatePrepayment must not exceed principal", "details": [] }
```

### Rejected: negative prepayment

Request: `immediatePrepayment=-1.00`.

```json
{ "error": "VALIDATION_FAILED", "message": "Request validation failed", "details": ["immediatePrepayment: immediatePrepayment must not be negative"] }
```

## Status codes

| Status | Meaning |
| --- | --- |
| `200 OK` | Comparison succeeded, including modeled edge-case statuses on either path |
| `400 Bad Request` | `VALIDATION_FAILED` for invalid/out-of-range fields (including `immediatePrepayment` exceeding `principal`), `MALFORMED_REQUEST` for unparseable JSON |
