# API: Equal Monthly Goal Contributions

`POST /api/planning/goal-contribution-calculator`

Stateless, disposable calculation. Every input is a caller-supplied temporary
modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and identical requests always
return identical results. Assumes zero growth, fees and withdrawals; this is
an explicit model convention, not a recommended household allocation policy.

## Request

| Field                | Type    | Rules |
|-----------------------|---------|-------|
| `currency`            | string  | Required. 3 letters, case-insensitive; normalized to uppercase in the response. |
| `targetAmount`         | decimal | Required. Greater than zero. At most 17 integer digits and 2 fraction digits. |
| `currentAmount`        | decimal | Required. Zero or greater. Same digit/scale limits as `targetAmount`. |
| `contributionMonths`   | integer | Required. Whole number from 1 through 1200 — the count of monthly contributions, not a date interval. A fractional value (e.g. `3.5`) is rejected, not truncated. |

## Response

| Field                 | Meaning |
|------------------------|---------|
| `currency`              | Echoed, normalized to uppercase. |
| `targetAmount`          | Echoed. |
| `currentAmount`         | Echoed. |
| `contributionMonths`    | Echoed. |
| `remainingAmount`       | `max(targetAmount - currentAmount, 0)`. |
| `monthlyContribution`   | `remainingAmount / contributionMonths`, rounded **up** to 2 decimal places so the goal is never left short by rounding. `0.00` when `ALREADY_FUNDED`. |
| `totalContributions`    | `monthlyContribution * contributionMonths`. `0.00` when `ALREADY_FUNDED`. |
| `projectedAmount`       | `currentAmount + totalContributions`. Equals `currentAmount` when `ALREADY_FUNDED`. |
| `amountAboveTarget`     | `max(projectedAmount - targetAmount, 0)`. Preserves an existing surplus when `ALREADY_FUNDED`. |
| `status`                | `ALREADY_FUNDED` when `currentAmount >= targetAmount`, otherwise `CONTRIBUTIONS_REQUIRED`. |

## Examples

### Exact division

Request:

```json
{
  "currency": "php",
  "targetAmount": "1000.00",
  "currentAmount": "100.00",
  "contributionMonths": 3
}
```

Response (`200 OK`):

```json
{
  "currency": "PHP",
  "targetAmount": 1000.00,
  "currentAmount": 100.00,
  "contributionMonths": 3,
  "remainingAmount": 900.00,
  "monthlyContribution": 300.00,
  "totalContributions": 900.00,
  "projectedAmount": 1000.00,
  "amountAboveTarget": 0.00,
  "status": "CONTRIBUTIONS_REQUIRED"
}
```

### Round-up remainder

A gap of 100.00 over 3 months does not divide evenly; the monthly amount is
rounded up so the goal is met a little early rather than falling short.

Request:

```json
{
  "currency": "PHP",
  "targetAmount": "100.00",
  "currentAmount": "0",
  "contributionMonths": 3
}
```

Response (`200 OK`):

```json
{
  "currency": "PHP",
  "targetAmount": 100.00,
  "currentAmount": 0.00,
  "contributionMonths": 3,
  "remainingAmount": 100.00,
  "monthlyContribution": 33.34,
  "totalContributions": 100.02,
  "projectedAmount": 100.02,
  "amountAboveTarget": 0.02,
  "status": "CONTRIBUTIONS_REQUIRED"
}
```

### Already funded, with surplus preserved

Request:

```json
{
  "currency": "PHP",
  "targetAmount": "500.00",
  "currentAmount": "650.00",
  "contributionMonths": 12
}
```

Response (`200 OK`):

```json
{
  "currency": "PHP",
  "targetAmount": 500.00,
  "currentAmount": 650.00,
  "contributionMonths": 12,
  "remainingAmount": 0.00,
  "monthlyContribution": 0.00,
  "totalContributions": 0.00,
  "projectedAmount": 650.00,
  "amountAboveTarget": 150.00,
  "status": "ALREADY_FUNDED"
}
```

### Validation error

Request:

```json
{
  "currency": "PHP",
  "targetAmount": "0",
  "currentAmount": "0",
  "contributionMonths": 3
}
```

Response (`400 Bad Request`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["targetAmount: targetAmount must be greater than zero"]
}
```

Other rejected inputs (all `400 VALIDATION_FAILED` unless noted): negative
`currentAmount`; `contributionMonths` of `0`, negative, or above `1200`; a
malformed `currency` code; a missing required field; more than 17 integer or
2 fraction digits on either amount. A fractional `contributionMonths` (e.g.
`3.5`) and a non-JSON body both return `400 MALFORMED_REQUEST` instead, since
they fail before request-object validation runs.

## Manually verified

All of the requests and responses above were exercised against a running
instance of the application (`./mvnw spring-boot:run` against a local
`postgres:16-alpine` container, per `docker-compose.yml`'s connection
settings) on 2026-09-05 and matched exactly, including the `400` error
paths. See `agent/implementation-log.md` (feature-local, this directory) for
the full verification record.
