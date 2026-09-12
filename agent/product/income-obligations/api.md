# API: Income Streams and Obligations

`GET /api/households/{householdId}/income-streams` (list) and
`/{incomeStreamId}` (detail)

`GET /api/households/{householdId}/obligations` (list) and
`/{obligationId}` (detail)

Read-only endpoints over the records created by the existing `POST`
endpoints (see `agent/product/income-obligations/product-brief.md`). No
aggregation, FX conversion, or cash-flow total is computed anywhere in this
contract — each row is returned exactly as recorded, with its own amount,
frequency, and currency, per PD-001 in the product brief.

## Monetary transport convention (differs from financial-position)

`amount` in both `IncomeStreamResponse` and `ObligationResponse` is a plain
**JSON number** (e.g. `1234.56`, not `"1234.56"`), backed by a `BigDecimal`
column with no custom Jackson serializer. This predates the exact-decimal
-string convention documented in
`agent/product/current-financial-position/api.md` and is **not** changed by
this API's frontend consumer (WAP-21) — a read-only view has no license to
alter an already-accepted API contract. The frontend's `money.ts` exposes a
separate `formatAmount(number)` presentation helper for this field,
distinct from `formatMoneyMagnitude(string)` used for financial-position
values. A future task could align this endpoint onto the decimal-string
convention; until then, be aware that very large amounts are subject to
ordinary IEEE-754 double precision once parsed by a JSON client.

## Response fields

### Income stream row

| Field                       | Type           | Meaning |
|------------------------------|----------------|---------|
| `id`                          | UUID           | Record identity. |
| `householdId`                 | UUID           | Echoed path household. |
| `name`                         | string         | Caller-supplied label. |
| `incomeType`                   | enum           | `SALARY`, `HOURLY_CONTRACT`, `BUSINESS_DISTRIBUTION`, `OTHER`. |
| `amount`                       | number         | See monetary transport convention above. |
| `frequency`                    | enum           | `HOURLY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `ANNUAL`. |
| `currency`                     | string         | Three-letter code, not converted. |
| `compensationClassification`   | enum           | `GROSS`, `NET`, `UNKNOWN`. |
| `certainty`                    | enum           | `CONFIRMED`, `EXPECTED`, `VARIABLE` — see "Certainty" below. |
| `startDate`                    | LocalDate      | May be future-dated. |
| `endDate`                      | LocalDate\|null | `null` means ongoing/open-ended. |
| `sourceType`                   | enum           | Always `MANUAL_ENTRY` today; server-assigned. |
| `createdAt` / `updatedAt`      | Instant        | Record bookkeeping timestamps. |

### Obligation row

Same shape minus `compensationClassification` and `certainty` (obligations
carry no income-specific uncertainty classification), and `obligationType`
in place of `incomeType`: `HOUSEHOLD_BASELINE`, `MORTGAGE`, `LOAN_PAYMENT`,
`INSURANCE`, `TUITION`, `TRAVEL_SINKING_FUND`, `DISCRETIONARY`, `OTHER`.

### Certainty

Per AGENTS.md's facts-vs-assumptions distinction (D003) and PD-002 in the
product brief, `certainty` is caller-supplied, never inferred from
`incomeType` or `amount`. Only `CONFIRMED` represents received/contracted
income; `EXPECTED` and `VARIABLE` are planning assumptions and must remain
visually distinguished wherever this field is rendered (see the frontend's
`badge--attention` treatment in `IncomeStreamTable.tsx`).

## Ordering

Both list endpoints return rows ordered by `createdAt` ascending, then `id`
ascending (`findByHousehold_IdOrderByCreatedAtAscIdAsc`) — deterministic
creation order, matching the product brief's acceptance criteria. No
client-side re-sorting is applied.

## Errors

| Status | Body `error`         | Cause |
|--------|------------------------|-------|
| `404`    | `HOUSEHOLD_NOT_FOUND`    | No household with that id (list/detail), or the record id belongs to a different household (detail). |
| `400`    | `VALIDATION_FAILED`      | Malformed path/query input. |

Handled by the shared `ApiExceptionHandler`; this feature adds no exception
handler of its own.

## Example

Request:

```
GET /api/households/3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10/income-streams
```

Response (`200 OK`):

```json
[
  {
    "id": "1b111111-1111-1111-1111-111111111111",
    "householdId": "3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10",
    "name": "October Salary",
    "incomeType": "SALARY",
    "amount": 50000.00,
    "frequency": "MONTHLY",
    "currency": "PHP",
    "compensationClassification": "GROSS",
    "certainty": "EXPECTED",
    "startDate": "2026-10-01",
    "endDate": null,
    "sourceType": "MANUAL_ENTRY",
    "createdAt": "2026-09-03T02:10:00Z",
    "updatedAt": "2026-09-03T02:10:00Z"
  }
]
```

An unknown household returns the same `HOUSEHOLD_NOT_FOUND` body shape shown
in `agent/product/current-financial-position/api.md`.
