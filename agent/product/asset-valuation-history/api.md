# API: Asset Valuation Updates and History

`GET /api/households/{householdId}/assets/{assetId}/valuations`
`POST /api/households/{householdId}/assets/{assetId}/valuations`
`GET /api/households/{householdId}/assets/{assetId}/valuations/history`

Lets a trusted manual-entry caller explicitly replace an existing asset's
recorded valuation and inspect its change history, without losing the prior
state or creating a duplicate asset. One asset identity is preserved
throughout: `estimatedValue`, `planningValue`, `valuedAt` and `sourceType`
are replaced in place on the existing `assets` row, and every accepted
change appends an immutable before/after row to a separate audit table
(`asset_valuations`). There is no update or delete endpoint for that audit
trail. `POST /assets` (asset creation), `GET /assets/{assetId}` and
`GET /assets` are unchanged by this feature — their request/response shapes
and behavior stay exactly as before.

Every source type recorded by this endpoint is `MANUAL_ENTRY`: it is a
trusted manual correction, never an import or an AI inference. An AI
suggestion alone must never call this endpoint directly (see `AGENTS.md`).

## Revision and concurrency

`revision` is an opaque, per-asset, monotonically increasing counter,
`0` for an asset that has never been updated. `GET .../valuations` returns
the asset's current valuation together with its current `revision`. A
`POST` must supply `expectedRevision` — the revision the caller last read.
The update is a single atomic conditional statement
(`UPDATE ... WHERE revision = :expectedRevision`): it succeeds and advances
`revision` by exactly one only if the row's current revision still matches.
Otherwise it changes nothing and returns `409`.

This is the lost-update guard required by the acceptance criteria: two
concurrent requests built on the same starting revision can yield at most
one success — the loser, whether truly concurrent or simply late, gets a
`409 ASSET_REVISION_CONFLICT` and appends no audit row. Repeating an
already-accepted request with its now-stale `expectedRevision` is rejected
the same way, so it cannot append a second change. This holds even when a
resubmission's `estimatedValue`/`planningValue`/`valuedAt` are textually
identical to the current row (an explicit same-date confirmation) — the
revision still advances and a new audit row is still appended, because the
update is a direct conditional SQL statement rather than something gated on
whether any field's value actually differs.

History ordering and "what is currently recorded" both use `revision`,
never `valuedAt` — an explicit correction to an earlier or later source
date is fully supported and preserved exactly as supplied, but it is the
most-recently-accepted revision that determines the current state, not the
latest `valuedAt`.

## `GET .../valuations` — current valuation and revision

No request body.

| Field           | Meaning |
|-----------------|---------|
| `assetId`       | Path asset id, echoed. |
| `householdId`   | Path household id, echoed. |
| `estimatedValue`| Exact two-decimal string (see Money below). |
| `planningValue` | Exact two-decimal string. |
| `currency`      | Unchanged by this feature; the asset's existing currency. |
| `valuedAt`      | The source date of the current recorded valuation. |
| `sourceType`    | Always `MANUAL_ENTRY`. |
| `revision`      | Current revision; `0` before any update. |

Response is `200 OK` even when the asset has never been updated — it
reports the state as of creation.

## `POST .../valuations` — submit a replacement valuation

### Request

| Field               | Type    | Rules |
|---------------------|---------|-------|
| `estimatedValue`    | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits; excess precision is rejected, never rounded. |
| `planningValue`     | decimal | Required. Zero or greater. Same digit/scale limits. Must not exceed `estimatedValue`. |
| `valuedAt`           | date    | Required. Not in the future. May be earlier or later than the asset's current `valuedAt` — an explicit correction, not a chronological constraint. |
| `reason`             | string  | Required, non-blank, at most 500 characters. |
| `expectedRevision`   | integer | Required, zero or greater. The revision last read from `GET .../valuations` (or a prior `POST` response). |

### Response (`200 OK`)

Same shape as `GET .../valuations`, reflecting the newly-accepted state —
`revision` is `expectedRevision + 1`.

### Conflict (`409 CONFLICT`)

```json
{
  "error": "ASSET_REVISION_CONFLICT",
  "message": "Asset <id> revision has changed; expected revision 0 is stale",
  "details": []
}
```

Nothing changes on the asset row, and no audit row is appended.

## `GET .../valuations/history` — ordered audit trail

No request body. Returns `200 OK` with an array, oldest revision first,
empty when the asset has never been updated.

| Field                      | Meaning |
|-----------------------------|---------|
| `id`                         | Audit row id. |
| `assetId` / `householdId`    | Identity of the asset and household this change belongs to. |
| `revision`                   | The resulting revision after this change; unique per asset, ascending. |
| `previousEstimatedValue` / `previousPlanningValue` / `previousValuedAt` / `previousSourceType` | Exact state immediately before this change — for the first-ever update, this is the asset's state as of creation. |
| `newEstimatedValue` / `newPlanningValue` / `newValuedAt` / `newSourceType` | Exact state this change produced. |
| `reason`                     | Caller-supplied, trimmed. |
| `recordedAt`                 | Server-assigned audit timestamp — distinct from `newValuedAt`, which is caller-supplied and never implies "confirmed as of today." |

## Money

`estimatedValue`/`planningValue` (and their `previous`/`new` counterparts in
history) are exact two-decimal strings, never JSON numbers — the same
non-exponential, zero-padded convention as the financial-position API
(`MoneyFormat.plain`, scoped to this package's response DTOs; existing
`AssetResponse` money fields are unchanged JSON numbers).

## Examples

### First update on a never-updated asset

Request:

```json
POST /api/households/{h}/assets/{a}/valuations
{
  "estimatedValue": "5200000.00",
  "planningValue": "4700000.00",
  "valuedAt": "2026-08-01",
  "reason": "Independent appraisal",
  "expectedRevision": 0
}
```

Response (`200 OK`):

```json
{
  "assetId": "65413fd7-baa8-40a3-9672-87f1f1f036d8",
  "householdId": "9de68ded-9a9a-49af-b8cf-a269b926cbef",
  "estimatedValue": "5200000.00",
  "planningValue": "4700000.00",
  "currency": "PHP",
  "valuedAt": "2026-08-01",
  "sourceType": "MANUAL_ENTRY",
  "revision": 1
}
```

`GET .../valuations/history` afterward returns exactly one entry, whose
`previousEstimatedValue`/`previousPlanningValue`/`previousValuedAt` equal
the asset's values at creation (`5000000.00` / `4500000.00` / `2026-06-01`
in this example) — proving the first update preserves the exact prior
state rather than fabricating it.

### Replaying the same (now-stale) revision

Request: identical `POST` body as above, sent again after it already
succeeded (`expectedRevision: 0`, but the asset is now at revision `1`).

Response (`409 CONFLICT`):

```json
{
  "error": "ASSET_REVISION_CONFLICT",
  "message": "Asset 65413fd7-baa8-40a3-9672-87f1f1f036d8 revision has changed; expected revision 0 is stale",
  "details": []
}
```

The asset and its history are unchanged — no second audit row is added.

### Validation errors

All `400 VALIDATION_FAILED` unless noted: `planningValue` greater than
`estimatedValue` (`"planningValue must not exceed estimatedValue"`); a
negative `estimatedValue`/`planningValue`; more than 2 fraction digits
(e.g. `"100.005"`) or more than 17 integer digits, rejected rather than
rounded or truncated; a blank or 501+ character `reason`; a `valuedAt` in
the future; a missing `expectedRevision`. A non-JSON body or a
non-numeric/non-integer field returns `400 MALFORMED_REQUEST` instead.

### Household scoping

`GET`/`POST .../valuations` and `GET .../valuations/history` for an unknown
`householdId` return `404 HOUSEHOLD_NOT_FOUND`. For an asset that exists
but belongs to a different household, all three return
`404 ASSET_NOT_FOUND` — the same not-found semantics as the existing
`GET /assets/{assetId}` endpoint.

## Manually verified

The primary examples above and the following were exercised against a
running instance of the application (`./mvnw spring-boot:run` against a
throwaway local `postgres:16-alpine` container, matching
`docker-compose.yml`'s connection settings) on 2026-09-11 and matched the
documented contract exactly:

- `GET .../valuations` before any update → `200`, `revision: 0`, values
  equal to the asset's creation-time values.
- First `POST` update (`expectedRevision: 0`) → `200`, `revision: 1`,
  updated values.
- `GET .../valuations/history` after that update → one entry, `revision: 1`,
  `previous*` fields matching the asset's exact creation-time state.
- Replaying the same now-stale `expectedRevision: 0` →
  `409 ASSET_REVISION_CONFLICT`, asset and history unchanged.
- `planningValue > estimatedValue` on update → `400 VALIDATION_FAILED`,
  `"planningValue must not exceed estimatedValue"`.
- `estimatedValue: "100.005"` (excess fractional scale) on update →
  `400 VALIDATION_FAILED`, `"estimatedValue must have at most 17 integer
  digits and 2 fraction digits"`.
- Blank `reason` (`"   "`) on update → `400 VALIDATION_FAILED`,
  `"reason must not be blank"`.
- `GET .../valuations` for a random unknown `householdId` →
  `404 HOUSEHOLD_NOT_FOUND`.

The PostgreSQL-backed integration test
`AssetValuationApiIntegrationTest.concurrentUpdatesBasedOnSameRevisionYieldExactlyOneSuccessAndOneStructuredConflict`
additionally proves, against real Testcontainers PostgreSQL, that two
requests issued concurrently from separate threads on the same starting
revision yield exactly one `200` and one `409`, with exactly one audit row
committed — the atomic commit/rollback and concurrency behavior required by
the acceptance criteria.
