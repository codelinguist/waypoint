# API: Emergency-Fund Runway

Stateless, single-currency calculation of how long an explicitly supplied
cash reserve covers an explicitly supplied monthly funding shortfall. Every
input is a temporary, caller-supplied modeling value; nothing is read from
or written to household state, and no household or entity identifier is
accepted.

## `POST /api/planning/emergency-fund-runway`

### Request body

| Field              | Type   | Required | Constraints |
|---------------------|--------|----------|-------------|
| `availableReserve`   | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits |
| `monthlyExpenses`    | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits |
| `monthlyNetIncome`   | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits (explicit zero income is valid) |
| `currency`           | string  | yes | exactly 3 letters; normalized to uppercase, no conversion |

### Response body

| Field                | Type            | Notes |
|-----------------------|-----------------|-------|
| `currency`             | string          | normalized, uppercase |
| `availableReserve`     | decimal         | echoes the request |
| `monthlyExpenses`      | decimal         | echoes the request |
| `monthlyNetIncome`     | decimal         | echoes the request |
| `monthlyShortfall`     | decimal         | `max(monthlyExpenses - monthlyNetIncome, 0)`, scale 2 |
| `status`               | `FINITE` \| `NO_SHORTFALL` | `NO_SHORTFALL` when income covers expenses, including when every input is zero |
| `runwayMonths`         | decimal or `null` | `null` for `NO_SHORTFALL`; otherwise `availableReserve / monthlyShortfall` **rounded down** (truncated, not rounded to nearest) to 2 decimal places |
| `fullMonthsCovered`    | integer or `null` | `null` for `NO_SHORTFALL`; otherwise `floor(availableReserve / monthlyShortfall)`, derived from the unrounded ratio (not from the rounded `runwayMonths`), so it cannot silently overflow a fixed-width type for very large inputs |
| `modelNote`            | string          | states this is a constant-input estimate excluding changes in income, spending, interest, inflation, and timing within a month; for `NO_SHORTFALL` also states the status describes only the supplied constant inputs |

Identical requests always return identical responses: there is no
clock-dependent field, no persistence, and no randomness.

### Example: finite runway

Request:

```json
{
  "availableReserve": "1000.00",
  "monthlyExpenses": "400.00",
  "monthlyNetIncome": "100.00",
  "currency": "usd"
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "availableReserve": 1000.00,
  "monthlyExpenses": 400.00,
  "monthlyNetIncome": 100.00,
  "monthlyShortfall": 300.00,
  "status": "FINITE",
  "runwayMonths": 3.33,
  "fullMonthsCovered": 3,
  "modelNote": "Constant-input estimate computed only from the supplied reserve, expenses, and income; it excludes any change in income, spending, interest, inflation, or timing within a month."
}
```

### Example: rounds down, not to the nearest hundredth

`1000.00 / 600.00 = 1.6666...`, which rounds to `1.67` at the nearest
hundredth but must be reported as `1.66`:

Request:

```json
{
  "availableReserve": "1000.00",
  "monthlyExpenses": "700.00",
  "monthlyNetIncome": "100.00",
  "currency": "USD"
}
```

Response (`200 OK`, relevant fields):

```json
{
  "monthlyShortfall": 600.00,
  "status": "FINITE",
  "runwayMonths": 1.66,
  "fullMonthsCovered": 1
}
```

### Example: no shortfall

Request:

```json
{
  "availableReserve": "1000.00",
  "monthlyExpenses": "400.00",
  "monthlyNetIncome": "400.00",
  "currency": "USD"
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "availableReserve": 1000.00,
  "monthlyExpenses": 400.00,
  "monthlyNetIncome": 400.00,
  "monthlyShortfall": 0.00,
  "status": "NO_SHORTFALL",
  "runwayMonths": null,
  "fullMonthsCovered": null,
  "modelNote": "Constant-input estimate computed only from the supplied reserve, expenses, and income; it excludes any change in income, spending, interest, inflation, or timing within a month. NO_SHORTFALL describes only these supplied constant inputs, not a guarantee that income will continue to cover expenses."
}
```

### Example: zero reserve with a positive shortfall

Reserve `0` with a positive shortfall is still `FINITE`, with zero runway:

```json
{
  "availableReserve": "0",
  "monthlyExpenses": "400.00",
  "monthlyNetIncome": "100.00",
  "currency": "USD"
}
```

```json
{
  "status": "FINITE",
  "runwayMonths": 0.00,
  "fullMonthsCovered": 0
}
```

### Validation errors (`400 Bad Request`)

Structured as the repository's existing error convention:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["availableReserve: availableReserve must not be negative"]
}
```

Rejected without silent rounding or truncation: missing/null required
fields, negative amounts, amounts with more than 2 fraction digits or more
than 17 integer digits, a currency that is not exactly 3 letters, and a
malformed JSON body (`MALFORMED_REQUEST`).
