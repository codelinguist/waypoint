# API: Planning Assumptions Review

Read-only review of a household's current *unsuperseded* planning
assumptions: which are due or overdue for review, and which have expired or
not yet started their effective window, as of an explicit caller-supplied
`asOf` date. It never reads the server clock, never includes an assumption's
`value` or `notes`, and performs no write, supersession, review-date reset,
persistence, migration, notification, or scheduled job. It reviews present
unsuperseded records only — it does not reconstruct which beliefs were
current as of a past `asOf` date.

Mounted at its own path, distinct from the existing
`GET /api/households/{householdId}/assumptions/{assumptionId}` lookup on
`PlanningAssumptionController`, so it can never be mistaken for an
assumption-id route.

## `GET /api/households/{householdId}/planning-assumptions/review`

### Query parameters

| Parameter | Type                 | Required | Constraints |
|-----------|----------------------|----------|-------------|
| `asOf`    | date (`YYYY-MM-DD`)  | yes      | any calendar date; not defaulted to the server's current date |

### Response body

| Field                                | Type                                         | Notes |
|---------------------------------------|-----------------------------------------------|-------|
| `householdId`                         | UUID                                          | echoes the path variable |
| `asOf`                                 | date                                          | echoes the request |
| `rows`                                 | array                                         | every unsuperseded planning assumption owned by the household; empty for a household with none |
| `rows[].assumptionId`                  | UUID                                          | retrieve the full record via the existing `GET /assumptions/{assumptionId}` |
| `rows[].name`                          | string                                        | |
| `rows[].reviewDate`                    | date                                          | |
| `rows[].effectiveFrom`                 | date                                          | |
| `rows[].effectiveUntil`                | date or `null`                                | `null` means open-ended (never expires) |
| `rows[].sourceType`                    | string                                        | e.g. `MANUAL_ENTRY` |
| `rows[].reviewStatus`                  | `OVERDUE` \| `DUE_TODAY` \| `UPCOMING`        | `OVERDUE` when `reviewDate < asOf`; `DUE_TODAY` when equal; `UPCOMING` when later |
| `rows[].effectiveStatus`               | `NOT_YET_EFFECTIVE` \| `EFFECTIVE` \| `EXPIRED` | `NOT_YET_EFFECTIVE` when `asOf < effectiveFrom`; `EXPIRED` when a non-null `effectiveUntil < asOf`; otherwise `EFFECTIVE`. The end date is inclusive: `effectiveUntil == asOf` is still `EFFECTIVE` |
| `rows[].needsAttention`                | boolean                                       | `true` when `reviewStatus` is `OVERDUE` or `DUE_TODAY`, or `effectiveStatus` is `EXPIRED` |
| `totalCount`                           | integer                                       | `rows.length` |
| `needsAttentionCount`                  | integer                                       | count of rows with `needsAttention == true` |
| `countsByReviewStatus`                 | object                                        | `{ "OVERDUE": n, "DUE_TODAY": n, "UPCOMING": n }`; all keys always present |
| `countsByEffectiveStatus`              | object                                        | `{ "NOT_YET_EFFECTIVE": n, "EFFECTIVE": n, "EXPIRED": n }`; all keys always present |
| `modelNote`                            | string                                        | states the limitation described above |

No assumption `value` or `notes` is ever included. Rows are ordered
deterministically: by `reviewDate`, then by `assumptionId`. Identical stored
state and `asOf` always return byte-for-byte identical responses — there is
no clock-dependent field. `reviewStatus` and `effectiveStatus` are
independent dimensions: a future-effective assumption can still be due for
review, and an expired assumption can still have an upcoming review date —
either alone drives `needsAttention`.

### Example: the acceptance-criteria boundary case

`asOf=2026-09-10`. Review dates one day before, on, and one day after `asOf`:

```
GET /api/households/{householdId}/planning-assumptions/review?asOf=2026-09-10
```

```json
{
  "householdId": "d6a49b39-52fc-4c8a-a1df-3eb41cbd18a4",
  "asOf": "2026-09-10",
  "rows": [
    {
      "assumptionId": "60acf2a1-3dc6-4851-8390-7f2d55e539da",
      "name": "Tuition inflation",
      "reviewDate": "2026-09-09",
      "effectiveFrom": "2026-01-01",
      "effectiveUntil": null,
      "sourceType": "MANUAL_ENTRY",
      "reviewStatus": "OVERDUE",
      "effectiveStatus": "EFFECTIVE",
      "needsAttention": true
    },
    {
      "assumptionId": "df4e89fb-c33e-4fc4-a50b-e803f00a11df",
      "name": "Expected investment return",
      "reviewDate": "2026-09-10",
      "effectiveFrom": "2026-01-01",
      "effectiveUntil": null,
      "sourceType": "MANUAL_ENTRY",
      "reviewStatus": "DUE_TODAY",
      "effectiveStatus": "EFFECTIVE",
      "needsAttention": true
    },
    {
      "assumptionId": "1b1e2f3a-4c5d-6e7f-8091-a2b3c4d5e6f7",
      "name": "Future monthly income",
      "reviewDate": "2026-09-11",
      "effectiveFrom": "2026-01-01",
      "effectiveUntil": null,
      "sourceType": "MANUAL_ENTRY",
      "reviewStatus": "UPCOMING",
      "effectiveStatus": "EFFECTIVE",
      "needsAttention": false
    }
  ],
  "totalCount": 3,
  "needsAttentionCount": 2,
  "countsByReviewStatus": { "OVERDUE": 1, "DUE_TODAY": 1, "UPCOMING": 1 },
  "countsByEffectiveStatus": { "NOT_YET_EFFECTIVE": 0, "EFFECTIVE": 3, "EXPIRED": 0 },
  "modelNote": "..."
}
```

### Example: independent dimensions

An assumption expired on `2026-09-01` but with a review date of `2026-12-01`
is `EXPIRED` and `UPCOMING` at once, and still needs attention because of
expiry alone:

```json
{
  "name": "Expired, not due",
  "reviewDate": "2026-12-01",
  "effectiveUntil": "2026-09-01",
  "reviewStatus": "UPCOMING",
  "effectiveStatus": "EXPIRED",
  "needsAttention": true
}
```

Conversely, an assumption that has not started yet (`effectiveFrom` in the
future) but whose review date has already passed is `NOT_YET_EFFECTIVE` and
`OVERDUE` at once:

```json
{
  "name": "Not started, overdue",
  "reviewDate": "2026-09-01",
  "effectiveFrom": "2026-12-01",
  "reviewStatus": "OVERDUE",
  "effectiveStatus": "NOT_YET_EFFECTIVE",
  "needsAttention": true
}
```

### Example: empty household

```json
{
  "householdId": "f0cd5e9f-c6ee-4bba-8ba5-382005db9015",
  "asOf": "2026-09-10",
  "rows": [],
  "totalCount": 0,
  "needsAttentionCount": 0,
  "countsByReviewStatus": { "OVERDUE": 0, "DUE_TODAY": 0, "UPCOMING": 0 },
  "countsByEffectiveStatus": { "NOT_YET_EFFECTIVE": 0, "EFFECTIVE": 0, "EXPIRED": 0 },
  "modelNote": "..."
}
```

### Errors

`404 Not Found` — unknown household, using the existing shared convention:

```json
{ "error": "HOUSEHOLD_NOT_FOUND", "message": "Household not found: <id>", "details": [] }
```

`400 Bad Request` — missing `asOf` is `VALIDATION_FAILED`; a malformed
`asOf` is `MALFORMED_REQUEST` (Spring rejects the type conversion before the
handler runs):

```json
{ "error": "VALIDATION_FAILED", "message": "Required parameter 'asOf' is not present", "details": [] }
```

```json
{ "error": "MALFORMED_REQUEST", "message": "Request parameter is malformed", "details": [] }
```

All error shapes reuse the repository's existing `ErrorResponse` convention
and the shared `ApiExceptionHandler` (`HouseholdNotFoundException`,
`MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`)
without modification; `InvalidPlanningAssumptionReviewInputException` has a
controller-local handler but is a defensive guard only — `asOf` is required
transport-side, so it cannot fire over HTTP in practice.
