# API: Future-Value Calculator

`POST /api/planning/future-value`

Stateless, disposable calculation. Every input is a caller-supplied temporary
modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and identical requests always
return identical results. The nominal annual rate is an explicit assumption,
not a fact, guarantee, forecast, or recommended allocation.

## Model conventions

- The nominal annual percentage rate is converted to a monthly rate by
  dividing by 12 and by 100 (e.g. `12.00` means 12% per year → 1% per month).
- Each month, growth accrues on the opening balance at the monthly rate and
  is rounded **HALF_UP** to 2 decimal places, then the equal monthly
  contribution is added at month end: `closing = opening + round(opening *
  monthlyRate) + contribution`.
- All monetary values (balances, growth, contribution) are kept at 2
  decimals throughout.
- This models one explicit, caller-supplied assumption over a fixed
  horizon — not a guaranteed, historical, or recommended return, and not a
  substitute for inflation, tax, fee, withdrawal, or variable-return
  modeling.

## Request

| Field                    | Type    | Rules |
|---------------------------|---------|-------|
| `currency`                 | string  | Required. 3 letters, case-insensitive; normalized to uppercase in the response, independent of JVM default locale. |
| `startingPrincipal`        | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits. |
| `monthlyContribution`      | decimal | Required. Zero or greater. Same digit/scale limits as `startingPrincipal`. |
| `annualRatePercentage`     | decimal | Required. Zero or greater, expressed as a percentage (e.g. `12.00` for 12%). At most 3 integer digits and 4 fraction digits. |
| `projectionMonths`         | integer | Required. Whole number from 1 through 1200. A fractional value (e.g. `3.5`) or a value that does not fit in a 32-bit integer is rejected, not truncated or narrowed. |

## Response

| Field                 | Meaning |
|------------------------|---------|
| `currency`              | Echoed, normalized to uppercase. |
| `startingPrincipal`     | Echoed. |
| `monthlyContribution`   | Echoed. |
| `annualRatePercentage`  | Echoed. |
| `projectionMonths`      | Echoed. |
| `endingValue`           | Balance after the final projected month; equals the last schedule row's `closingBalance`. |
| `totalContributed`      | `startingPrincipal + (monthlyContribution * projectionMonths)`. |
| `totalGrowth`           | `endingValue - totalContributed`; also the sum of every row's `growth`. |
| `conventions`           | Human-readable statement of the compounding/contribution/rounding conventions applied (see above). |
| `schedule`              | Deterministic, ordered array of monthly rows: `month`, `openingBalance`, `growth`, `contribution`, `closingBalance`. Each row reconciles as `openingBalance + growth + contribution = closingBalance`. |

## Examples

### Worked example (monthly sequencing)

Request:

```json
{
  "currency": "usd",
  "startingPrincipal": "1000.00",
  "monthlyContribution": "100.00",
  "annualRatePercentage": "12.00",
  "projectionMonths": 2
}
```

Response (`200 OK`):

```json
{
  "currency": "USD",
  "startingPrincipal": 1000.00,
  "monthlyContribution": 100.00,
  "annualRatePercentage": 12.00,
  "projectionMonths": 2,
  "endingValue": 1221.10,
  "totalContributed": 1200.00,
  "totalGrowth": 21.10,
  "conventions": "Nominal annual rate divided by 12 for a monthly rate; growth accrues monthly on the opening balance and is rounded HALF_UP to 2 decimal places, then the equal monthly contribution is added at each month's end. This models one explicit, caller-supplied assumption, not a guaranteed, historical, or recommended return.",
  "schedule": [
    { "month": 1, "openingBalance": 1000.00, "growth": 10.00, "contribution": 100.00, "closingBalance": 1110.00 },
    { "month": 2, "openingBalance": 1110.00, "growth": 11.10, "contribution": 100.00, "closingBalance": 1221.10 }
  ]
}
```

### Zero rate

Request:

```json
{
  "currency": "USD",
  "startingPrincipal": "500.00",
  "monthlyContribution": "50.00",
  "annualRatePercentage": "0",
  "projectionMonths": 6
}
```

Response (`200 OK`): `endingValue` equals `totalContributed` (`800.00`) and
`totalGrowth` is `0.00`; every row's `growth` is `0.00`.

### All-zero money inputs

Request:

```json
{
  "currency": "USD",
  "startingPrincipal": "0",
  "monthlyContribution": "0",
  "annualRatePercentage": "5.00",
  "projectionMonths": 3
}
```

Response (`200 OK`): a valid zero schedule — every row is all zeros, since
growth on a zero opening balance is always zero regardless of rate. This
combination is not rejected.

### Validation error

Request:

```json
{
  "currency": "USD",
  "startingPrincipal": "-0.01",
  "monthlyContribution": "0",
  "annualRatePercentage": "0",
  "projectionMonths": 1
}
```

Response (`400 Bad Request`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["startingPrincipal: startingPrincipal must not be negative"]
}
```

Other rejected inputs (all `400 VALIDATION_FAILED` unless noted): negative
`monthlyContribution` or `annualRatePercentage`; `projectionMonths` of `0`,
negative, or above `1200`; a malformed `currency` code; a missing required
field; more than 17 integer or 2 fraction digits on either money field, or
more than 3 integer or 4 fraction digits on `annualRatePercentage`,
including a negative-scale representation of the same value (e.g. `1E+17`
for a money field). A fractional `projectionMonths` (e.g. `3.5`), a
`projectionMonths` that does not fit in a 32-bit integer (e.g.
`4294967299`, which would otherwise silently narrow to `3`), and a
non-JSON body all return `400 MALFORMED_REQUEST` instead, since they fail
before request-object validation runs.

## Manually verified

All of the requests and responses above, plus the full validation-error
list, were exercised against a running instance of the application
(`./mvnw spring-boot:run` against a throwaway local `postgres:16-alpine`
container matching `docker-compose.yml`'s connection settings, on a
non-default host port to avoid colliding with any other running instance)
on 2026-09-06 and matched exactly, including the `400` error paths and the
negative-scale digit-limit and integer-overflow-narrowing defenses. The
manual app and throwaway Postgres container were both stopped afterward;
no container or process was left running. See
`agent/product/future-value-calculator/implementation-log.md` for the full
verification record.
