# API: Multiple Goal Funding Check

`POST /api/planning/multi-goal-funding-check`

Stateless, disposable calculation. Every input is a caller-supplied temporary
modeling value — nothing is read from or written to household state, no
household or entity identifier is accepted, and identical requests always
return identical results. Each goal's required monthly contribution is
calculated independently by reusing the accepted
[goal-contribution calculator](../goal-contribution-calculator/api.md)
read-only; this endpoint never chooses a priority order between goals or
reallocates budget. Assumes zero growth, fees and withdrawals, and that every
goal starts contributing in the first modeled month — an explicit model
convention, not a recommended household allocation policy or an optimizer.

## Request

| Field                     | Type    | Rules |
|----------------------------|---------|-------|
| `currency`                  | string  | Required. 3 letters, case-insensitive; normalized to uppercase in the response. |
| `availableMonthlyBudget`    | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits. |
| `goals`                     | array   | Required. 1 through 50 entries; no null entries. |
| `goals[].reference`         | string  | Required. 1 through 64 non-blank characters; unique within the request. |
| `goals[].targetAmount`      | decimal | Required. Greater than zero. Same digit/scale limits as `availableMonthlyBudget`. |
| `goals[].currentAmount`     | decimal | Required. Zero or greater. Same digit/scale limits as `targetAmount`. |
| `goals[].contributionMonths`| integer | Required. Whole number from 1 through 1200. A fractional value (e.g. `3.5`) is rejected, not truncated. |

## Response

| Field                                | Meaning |
|----------------------------------------|---------|
| `currency`                              | Echoed, normalized to uppercase. |
| `availableMonthlyBudget`                | Echoed. |
| `goalResults`                           | Per-goal results, in caller order. |
| `goalResults[].reference`               | Echoed from the matching request goal. |
| `goalResults[].*`                       | Same fields and conventions as the [goal-contribution calculator](../goal-contribution-calculator/api.md) response (`targetAmount`, `currentAmount`, `contributionMonths`, `remainingAmount`, `monthlyContribution`, `totalContributions`, `projectedAmount`, `amountAboveTarget`, `status`). |
| `totalRequiredMonthlyContribution`      | Sum of every goal's rounded `monthlyContribution`, summed after rounding — never a rounded aggregate quotient. |
| `budgetMinusRequired`                   | `availableMonthlyBudget - totalRequiredMonthlyContribution`. May be negative. |
| `shortfall`                             | `max(0, -budgetMinusRequired)`. |
| `unallocatedBudget`                     | `max(0, budgetMinusRequired)`. |
| `status`                                | `FITS` when `budgetMinusRequired >= 0`, otherwise `SHORTFALL`. |

Already-funded goals (`currentAmount >= targetAmount`) contribute `0.00` to
`totalRequiredMonthlyContribution` while retaining their own
`ALREADY_FUNDED` status in `goalResults`.

## Examples

### Two goals, budget falls short

Request:

```json
{
  "currency": "php",
  "availableMonthlyBudget": "120",
  "goals": [
    { "reference": "education", "targetAmount": "100", "currentAmount": "0", "contributionMonths": 3 },
    { "reference": "travel", "targetAmount": "200", "currentAmount": "0", "contributionMonths": 2 }
  ]
}
```

Response (`200 OK`):

```json
{
  "currency": "PHP",
  "availableMonthlyBudget": 120.00,
  "goalResults": [
    {
      "reference": "education",
      "targetAmount": 100.00,
      "currentAmount": 0.00,
      "contributionMonths": 3,
      "remainingAmount": 100.00,
      "monthlyContribution": 33.34,
      "totalContributions": 100.02,
      "projectedAmount": 100.02,
      "amountAboveTarget": 0.02,
      "status": "CONTRIBUTIONS_REQUIRED"
    },
    {
      "reference": "travel",
      "targetAmount": 200.00,
      "currentAmount": 0.00,
      "contributionMonths": 2,
      "remainingAmount": 200.00,
      "monthlyContribution": 100.00,
      "totalContributions": 200.00,
      "projectedAmount": 200.00,
      "amountAboveTarget": 0.00,
      "status": "CONTRIBUTIONS_REQUIRED"
    }
  ],
  "totalRequiredMonthlyContribution": 133.34,
  "budgetMinusRequired": -13.34,
  "shortfall": 13.34,
  "unallocatedBudget": 0.00,
  "status": "SHORTFALL"
}
```

### Budget exactly equal to the total

Request:

```json
{
  "currency": "PHP",
  "availableMonthlyBudget": "100",
  "goals": [
    { "reference": "g1", "targetAmount": "300", "currentAmount": "0", "contributionMonths": 3 }
  ]
}
```

Response (`200 OK`): `totalRequiredMonthlyContribution: 100.00`,
`budgetMinusRequired: 0.00`, `shortfall: 0.00`, `unallocatedBudget: 0.00`,
`status: "FITS"`.

### Already-funded goal, zero budget

Request:

```json
{
  "currency": "PHP",
  "availableMonthlyBudget": "0",
  "goals": [
    { "reference": "g1", "targetAmount": "100", "currentAmount": "150", "contributionMonths": 12 }
  ]
}
```

Response (`200 OK`): `goalResults[0].status: "ALREADY_FUNDED"`,
`goalResults[0].monthlyContribution: 0.00`,
`totalRequiredMonthlyContribution: 0.00`, `status: "FITS"` (a zero budget is
valid whenever nothing is required).

### Validation error — duplicate reference

Request:

```json
{
  "currency": "PHP",
  "availableMonthlyBudget": "1000",
  "goals": [
    { "reference": "dup", "targetAmount": "100", "currentAmount": "0", "contributionMonths": 1 },
    { "reference": "dup", "targetAmount": "50", "currentAmount": "0", "contributionMonths": 1 }
  ]
}
```

Response (`400 Bad Request`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "duplicate goal reference: dup",
  "details": []
}
```

Other rejected inputs (all `400 VALIDATION_FAILED` unless noted): a blank or
missing `currency`; a negative `availableMonthlyBudget`; an empty or
missing `goals` array; more than 50 `goals` entries; a `null` entry inside
`goals`; a blank or 65+ character `reference`; a `targetAmount` of zero or
below; a negative `currentAmount`; `contributionMonths` of `0`, negative, or
above `1200`; and more than 17 integer or 2 fraction digits on any amount,
including a negative-scale representation of the same value (e.g. `1E+17`).
A fractional `contributionMonths` (e.g. `3.5`), a `contributionMonths` that
does not fit in a 32-bit integer, and a non-JSON body all return
`400 MALFORMED_REQUEST` instead, since they fail before request-object
validation runs.

## Manually verified

The primary examples above and the following edge cases were exercised
against a running instance of the application (`./mvnw spring-boot:run`
against a throwaway local `postgres:16-alpine` container, matching
`docker-compose.yml`'s connection settings) on 2026-09-10 and matched the
documented contract exactly:

- Two-goal shortfall, budget-exactly-equal `FITS`, and already-funded
  goal with zero budget (`200`, matching the examples above verbatim).
- Duplicate `reference` → `400 VALIDATION_FAILED`,
  `"duplicate goal reference: dup"`.
- Empty `goals` array → `400 VALIDATION_FAILED`,
  `"goals must not be empty"`.
- A `null` entry inside `goals` → `400 VALIDATION_FAILED`,
  `"goals[1]: goals must not contain null entries"`.
- Fractional `contributionMonths` (`3.5`) → `400 MALFORMED_REQUEST`.
- `targetAmount: "1E+17"` (negative-scale 18-digit bypass attempt) →
  `400 VALIDATION_FAILED`, caught by the shared `@Digits` bean validation
  before reaching the domain layer.
- 51 `goals` entries → `400 VALIDATION_FAILED`,
  `"goals must contain at most 50 entries"`.
- A non-JSON body → `400 MALFORMED_REQUEST`.

See `agent/implementation-log.md` (feature-local, this directory) for the
full verification record.
