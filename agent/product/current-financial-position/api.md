# API: Current Financial Position

`GET /api/households/{householdId}/financial-position`

Read-only. One coherent read of a household's recorded asset and liability
rows, plus per-currency totals derived from exactly those rows. Nothing is
written; no snapshot is created. `retrievedAt` is when the read happened, not
a valuation date — the returned rows may include future-dated records.

## Monetary transport convention

Every monetary field in this endpoint — row values and totals — is an exact
**decimal string** with two fractional digits (e.g. `"1234.56"`, never
`1234.56` as a JSON number, and never scientific notation). This is
deliberate and specific to this endpoint: a browser consumer parsing this
response as a JSON `Number` can silently lose cents on large balances (a
JavaScript `Number` only has ~15-17 significant decimal digits of exact
integer precision). Existing endpoints (`/assets`, `/liabilities`,
`/financial-snapshots`, ...) are unchanged and continue to serialize money as
JSON numbers.

## Response

| Field              | Type   | Meaning |
|---------------------|--------|---------|
| `householdId`        | UUID   | Echoed path household. |
| `householdName`       | string | Metadata only — not part of any calculation. |
| `baseCurrency`        | string | Metadata only — totals are never converted into it; see "No FX" below. |
| `retrievedAt`          | instant | When this read happened. Not a valuation date; does not imply the rows are current as of "now". |
| `assets`               | array  | See "Asset row" below. Deterministically ordered by `id` (UUID) ascending. |
| `liabilities`          | array  | See "Liability row" below. Deterministically ordered by `id` (UUID) ascending. |
| `totalsByCurrency`      | array  | See "Currency totals" below. Ordered by `currency` ascending. |

### Asset row

Copied read-only from the existing `Asset` record: `id`, `name`,
`assetType`, `estimatedValue`, `planningValue`, `currency`, `valuedAt`,
`liquidity`, `sourceType`. `estimatedValue`/`planningValue` are decimal
strings per the convention above; all other fields match the existing
`/assets` contract's shape and values exactly.

### Liability row

Copied read-only from the existing `Liability` record: `id`, `name`,
`liabilityType`, `outstandingBalance`, `currency`, `balanceAsOf`,
`sourceType`. `outstandingBalance` is a decimal string per the convention
above.

### Currency totals

One entry per currency present in either `assets` or `liabilities` (never
both merged, never a currency neither list uses):

| Field            | Meaning |
|-------------------|---------|
| `currency`          | ISO-style currency code. |
| `assetTotal`         | Sum of that currency's `assets[].planningValue` — **not** `estimatedValue`. Exact zero if the currency has no asset rows. |
| `liabilityTotal`     | Sum of that currency's `liabilities[].outstandingBalance`. Exact zero if the currency has no liability rows. |
| `netWorth`           | `assetTotal - liabilityTotal`. Can be negative. |

No all-currency sum is returned and no currency conversion is applied —
mixing `PHP` and `USD` totals into one number would silently assert an
exchange rate this system does not have an opinion about. An empty household
returns empty `assets`/`liabilities`/`totalsByCurrency`; no currency group is
invented for a household with no recorded rows.

## Consistency boundary

The household lookup and both row reads run inside one PostgreSQL
`REPEATABLE READ` transaction (`FinancialPositionService`,
`@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)`).
That means every total in the response is derived from exactly the rows also
returned in `assets`/`liabilities` — a write committed by another request
while this read is in flight can never appear in the totals without also
appearing in the rows, or vice versa.
`FinancialPositionTransactionIsolationTest` proves this directly: it opens
a `REPEATABLE_READ` transaction, reads assets, lets a second thread commit a
new liability, and confirms the liability read in the same transaction still
does not see it.

## Errors

| Status | Body `error`              | Cause |
|--------|-----------------------------|-------|
| `404`    | `HOUSEHOLD_NOT_FOUND`         | No household with that id. |
| `400`    | `MALFORMED_REQUEST`           | `householdId` path segment is not a valid UUID. |

Both are handled by the shared `ApiExceptionHandler`; this feature adds no
exception handler of its own.

## Examples

### Mixed-currency household

Request:

```
GET /api/households/3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10/financial-position
```

Response (`200 OK`):

```json
{
  "householdId": "3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10",
  "householdName": "Ralph Household",
  "baseCurrency": "PHP",
  "retrievedAt": "2026-09-10T06:15:22.104Z",
  "assets": [
    {
      "id": "1b111111-1111-1111-1111-111111111111",
      "name": "Urban Lot",
      "assetType": "PROPERTY",
      "estimatedValue": "3000000.00",
      "planningValue": "2500000.00",
      "currency": "PHP",
      "valuedAt": "2026-08-01",
      "liquidity": "ILLIQUID",
      "sourceType": "MANUAL_ENTRY"
    },
    {
      "id": "2c222222-2222-2222-2222-222222222222",
      "name": "Brokerage",
      "assetType": "INVESTMENT",
      "estimatedValue": "200.00",
      "planningValue": "200.00",
      "currency": "USD",
      "valuedAt": "2026-09-01",
      "liquidity": "LIQUID",
      "sourceType": "MANUAL_ENTRY"
    }
  ],
  "liabilities": [
    {
      "id": "3d333333-3333-3333-3333-333333333333",
      "name": "Mortgage",
      "liabilityType": "MORTGAGE",
      "outstandingBalance": "500000.00",
      "currency": "PHP",
      "balanceAsOf": "2026-09-01",
      "sourceType": "MANUAL_ENTRY"
    },
    {
      "id": "4e444444-4444-4444-4444-444444444444",
      "name": "Card",
      "liabilityType": "CREDIT_CARD",
      "outstandingBalance": "50.00",
      "currency": "USD",
      "balanceAsOf": "2026-09-01",
      "sourceType": "MANUAL_ENTRY"
    }
  ],
  "totalsByCurrency": [
    { "currency": "PHP", "assetTotal": "2500000.00", "liabilityTotal": "500000.00", "netWorth": "2000000.00" },
    { "currency": "USD", "assetTotal": "200.00", "liabilityTotal": "50.00", "netWorth": "150.00" }
  ]
}
```

### Empty household

Response (`200 OK`):

```json
{
  "householdId": "5f555555-5555-5555-5555-555555555555",
  "householdName": "New Household",
  "baseCurrency": "PHP",
  "retrievedAt": "2026-09-10T06:16:01.442Z",
  "assets": [],
  "liabilities": [],
  "totalsByCurrency": []
}
```

### Unknown household

Response (`404 Not Found`):

```json
{ "error": "HOUSEHOLD_NOT_FOUND", "message": "Household not found: 00000000-0000-0000-0000-000000000000", "details": [] }
```
