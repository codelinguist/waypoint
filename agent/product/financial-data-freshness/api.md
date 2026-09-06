# API: Financial Data Freshness Review

Read-only review of how old a household's *currently stored* asset
valuations and liability balances are, relative to an explicit
caller-supplied review date and age threshold. It never reads the server
clock, never copies a financial amount into its response, and performs no
write, migration, or scheduling. It does not verify a value is still
correct today, and it does not reconstruct historical household state as of
`reviewDate` — it reviews present source rows only.

## `GET /api/households/{householdId}/financial-data-freshness`

### Query parameters

| Parameter    | Type        | Required | Constraints |
|--------------|-------------|----------|-------------|
| `reviewDate` | date (`YYYY-MM-DD`) | yes | any calendar date; not defaulted to the server's current date |
| `maxAgeDays` | integer     | yes | whole number, `0..36500` inclusive |

### Response body

| Field                     | Type                         | Notes |
|---------------------------|-------------------------------|-------|
| `householdId`              | UUID                          | echoes the path variable |
| `reviewDate`                | date                          | echoes the request |
| `maxAgeDays`                | integer                       | echoes the request |
| `records`                   | array                         | every current asset and liability owned by the household — not only stale ones; empty for a household with no records |
| `records[].recordId`        | UUID                          | the asset or liability's own id |
| `records[].recordKind`      | `ASSET` \| `LIABILITY`        | |
| `records[].name`            | string                        | |
| `records[].currency`        | string                        | |
| `records[].sourceType`      | string                        | e.g. `MANUAL_ENTRY` |
| `records[].sourceDate`      | date                          | the asset's `valuedAt` or the liability's `balanceAsOf` — never `createdAt`/`updatedAt` |
| `records[].ageDays`         | integer (signed)              | `reviewDate - sourceDate` in exact calendar days; negative when `sourceDate` is after `reviewDate` |
| `records[].classification`  | `CURRENT` \| `STALE` \| `FUTURE_DATED` | `STALE` when `ageDays > maxAgeDays`; `CURRENT` when `0 <= ageDays <= maxAgeDays`; `FUTURE_DATED` when `ageDays < 0` |
| `countsByKind`              | object                        | `{ "ASSET": n, "LIABILITY": n }`; both keys always present, `0` when there are no records of that kind |
| `countsByClassification`    | object                        | `{ "CURRENT": n, "STALE": n, "FUTURE_DATED": n }`; all three keys always present |
| `modelNote`                 | string                        | states the limitation described above |

No financial amount (`estimatedValue`, `planningValue`, `outstandingBalance`)
is ever included. Records are ordered deterministically: by `recordKind`
(`ASSET` before `LIABILITY`), then by `recordId`. Identical inputs against
unchanged source records always return byte-for-byte identical responses —
there is no clock-dependent field.

### Example: the acceptance-criteria boundary case

`reviewDate=2026-09-06`, `maxAgeDays=30`. An asset valued `2026-08-07` is
exactly 30 days old (`CURRENT`); a liability last balanced `2026-08-06` is
31 days old (`STALE`):

```
GET /api/households/{householdId}/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30
```

```json
{
  "householdId": "d6a49b39-52fc-4c8a-a1df-3eb41cbd18a4",
  "reviewDate": "2026-09-06",
  "maxAgeDays": 30,
  "records": [
    {
      "recordId": "df4e89fb-c33e-4fc4-a50b-e803f00a11df",
      "recordKind": "ASSET",
      "name": "Emergency Fund",
      "currency": "PHP",
      "sourceType": "MANUAL_ENTRY",
      "sourceDate": "2026-08-07",
      "ageDays": 30,
      "classification": "CURRENT"
    },
    {
      "recordId": "60acf2a1-3dc6-4851-8390-7f2d55e539da",
      "recordKind": "LIABILITY",
      "name": "Credit Card",
      "currency": "PHP",
      "sourceType": "MANUAL_ENTRY",
      "sourceDate": "2026-08-06",
      "ageDays": 31,
      "classification": "STALE"
    }
  ],
  "countsByKind": { "ASSET": 1, "LIABILITY": 1 },
  "countsByClassification": { "CURRENT": 1, "STALE": 1, "FUTURE_DATED": 0 },
  "modelNote": "ageDays is reviewDate minus each record's stored source date (valuedAt for an asset, balanceAsOf for a liability), classified against the supplied maxAgeDays threshold. This reviews the freshness of present source rows relative to the supplied reviewDate; it is not proof a value is still correct today, and it does not reconstruct historical household state as of reviewDate."
}
```

### Example: `FUTURE_DATED`

An asset valued `2026-08-26` reviewed as of `2026-08-25` (`maxAgeDays=30`)
is one day *after* the review date:

```
GET /api/households/{householdId}/financial-data-freshness?reviewDate=2026-08-25&maxAgeDays=30
```

Relevant record:

```json
{
  "recordKind": "ASSET",
  "name": "Future-dated demo",
  "sourceDate": "2026-08-26",
  "ageDays": -1,
  "classification": "FUTURE_DATED"
}
```

### Example: zero threshold

`maxAgeDays=0` marks a source dated the same day as `reviewDate` `CURRENT`
and any earlier date `STALE` — there is no grace period.

### Example: empty household

A household with no assets or liabilities returns an empty `records` array
and every count key present at `0`:

```json
{
  "householdId": "f0cd5e9f-c6ee-4bba-8ba5-382005db9015",
  "reviewDate": "2026-09-06",
  "maxAgeDays": 30,
  "records": [],
  "countsByKind": { "ASSET": 0, "LIABILITY": 0 },
  "countsByClassification": { "CURRENT": 0, "STALE": 0, "FUTURE_DATED": 0 },
  "modelNote": "..."
}
```

### Errors

`404 Not Found` — unknown household, using the existing shared convention:

```json
{ "error": "HOUSEHOLD_NOT_FOUND", "message": "Household not found: <id>", "details": [] }
```

`400 Bad Request` — missing `reviewDate`/`maxAgeDays` and a `maxAgeDays`
outside `0..36500` (including negative) are `VALIDATION_FAILED`; a malformed
`reviewDate`, a fractional `maxAgeDays` (e.g. `30.5`), or a `maxAgeDays` that
overflows a 32-bit integer are `MALFORMED_REQUEST` (Spring rejects the type
conversion before the handler runs):

```json
{ "error": "VALIDATION_FAILED", "message": "maxAgeDays must be between 0 and 36500", "details": [] }
```

```json
{ "error": "MALFORMED_REQUEST", "message": "Request parameter is malformed", "details": [] }
```

All error shapes reuse the repository's existing `ErrorResponse` convention
and the shared `ApiExceptionHandler` (`HouseholdNotFoundException`,
`MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`)
without modification; only the domain-specific `maxAgeDays` range check is a
controller-local handler scoped to this feature's own
`InvalidFreshnessReviewInputException`.
