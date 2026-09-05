# API: Fixed-Payment Debt Amortization

`POST /api/planning/debt-amortization`

Stateless calculation endpoint. Every input is a temporary, caller-supplied modeling
value — nothing is read from or written to household state, no household or entity
identifier is accepted, and the request body is never logged. Identical requests
always return identical results.

This is an illustrative constant-rate schedule, not a lender payoff quote. See
"Model conventions" below.

## Request

```json
{
  "principal": "1000.00",
  "monthlyInterestRate": "0.01",
  "monthlyPayment": "300.00",
  "currency": "USD"
}
```

| Field | Type | Constraints |
| --- | --- | --- |
| `principal` | decimal string | required, `>= 0`, at most 17 integer digits and 2 fractional digits |
| `monthlyInterestRate` | decimal string | required, `0` to `1` inclusive, at most 8 fractional digits. This is an explicit **monthly** rate — `0.01` means 1% per month. It is never inferred or converted from an annual rate. |
| `monthlyPayment` | decimal string | required, `> 0`, at most 17 integer digits and 2 fractional digits |
| `currency` | string | required, 3 alphabetic letters, normalized to uppercase; no currency conversion |

Null, missing, malformed, out-of-range, or excessive-scale/precision values are rejected
with `400 VALIDATION_FAILED` (or `400 MALFORMED_REQUEST` for unparseable JSON). Invalid
values are never silently rounded or coerced.

## Model conventions

- Each month, interest accrues on the **opening** balance, then the payment is applied
  at month end: `payment = min(monthlyPayment, openingBalance + interest)`.
- Monthly interest is rounded to 2 decimal places using HALF_UP before the payment is
  applied. All monetary values are kept at 2 decimals.
- `closingBalance = openingBalance + interest - payment`. A final payment may be smaller
  than the regular fixed payment.
- The schedule is bounded to 1200 months (100 years).

## Response

### Paid off

Request:

```json
{
  "principal": "1000.00",
  "monthlyInterestRate": "0",
  "monthlyPayment": "300.00",
  "currency": "USD"
}
```

Response (`200 OK`):

```json
{
  "principal": 1000.00,
  "monthlyInterestRate": 0,
  "monthlyPayment": 300.00,
  "currency": "USD",
  "status": "PAID_OFF",
  "payoffMonths": 4,
  "totalPaid": 1000.00,
  "totalInterest": 0.00,
  "remainingBalance": 0.00,
  "schedule": [
    { "month": 1, "openingBalance": 1000.00, "interest": 0.00, "payment": 300.00, "principalRepaid": 300.00, "closingBalance": 700.00 },
    { "month": 2, "openingBalance": 700.00,  "interest": 0.00, "payment": 300.00, "principalRepaid": 300.00, "closingBalance": 400.00 },
    { "month": 3, "openingBalance": 400.00,  "interest": 0.00, "payment": 300.00, "principalRepaid": 300.00, "closingBalance": 100.00 },
    { "month": 4, "openingBalance": 100.00,  "interest": 0.00, "payment": 100.00, "principalRepaid": 100.00, "closingBalance": 0.00 }
  ]
}
```

### Paid off with rounding (half-cent HALF_UP example)

Request: `principal=100.00`, `monthlyInterestRate=0.01`, `monthlyPayment=60.00`, `currency=USD`.

```json
{
  "principal": 100.00,
  "monthlyInterestRate": 0.01,
  "monthlyPayment": 60.00,
  "currency": "USD",
  "status": "PAID_OFF",
  "payoffMonths": 2,
  "totalPaid": 101.41,
  "totalInterest": 1.41,
  "remainingBalance": 0.00,
  "schedule": [
    { "month": 1, "openingBalance": 100.00, "interest": 1.00, "payment": 60.00, "principalRepaid": 59.00, "closingBalance": 41.00 },
    { "month": 2, "openingBalance": 41.00,  "interest": 0.41, "payment": 41.41, "principalRepaid": 41.00, "closingBalance": 0.00 }
  ]
}
```

### Non-amortizing (fixed payment does not exceed first month's interest)

Request: `principal=1000.00`, `monthlyInterestRate=0.01`, `monthlyPayment=10.00`, `currency=USD`.

```json
{
  "principal": 1000.00,
  "monthlyInterestRate": 0.01,
  "monthlyPayment": 10.00,
  "currency": "USD",
  "status": "NON_AMORTIZING",
  "payoffMonths": null,
  "totalPaid": 0.00,
  "totalInterest": 0.00,
  "remainingBalance": 1000.00,
  "schedule": []
}
```

The balance never decreases and is never reported as paid off.

### Horizon limit (still amortizing after 1200 months)

For a principal, rate, and payment combination that has not reached zero after 1200
months, the response returns `status: "HORIZON_LIMIT"`, `payoffMonths: null`, the 1200
computed rows, the remaining balance, and totals that are explicitly partial (the sum of
the 1200 computed rows, not the full lifetime cost):

```json
{
  "status": "HORIZON_LIMIT",
  "payoffMonths": null,
  "totalPaid": "<sum of the 1200 rows>",
  "totalInterest": "<sum of the 1200 rows>",
  "remainingBalance": "<balance still owed after month 1200>",
  "schedule": ["... 1200 rows ..."]
}
```

### Zero principal

Request with `principal=0` returns `status: "PAID_OFF"`, `payoffMonths: 0`, zero totals,
and an empty `schedule`, regardless of the supplied rate or payment.

## Status codes

| Status | Meaning |
| --- | --- |
| `200 OK` | Calculation succeeded, including modeled edge-case statuses (`PAID_OFF`, `NON_AMORTIZING`, `HORIZON_LIMIT`) |
| `400 Bad Request` | `VALIDATION_FAILED` for invalid/out-of-range fields, `MALFORMED_REQUEST` for unparseable JSON |
