# API: Liability Balance History (WAP-17)

`POST /api/households/{householdId}/liabilities/{liabilityId}/balances`
`GET  /api/households/{householdId}/liabilities/{liabilityId}/balances`

Replaces a liability's current outstanding balance — the current row is
never edited in place from the caller's point of view, and the previous
balance and its context are never lost. Identity, household, name, type, and
currency are unaffected by a balance replacement.

`GET /api/households/{householdId}/liabilities/{liabilityId}` (existing
endpoint) now also returns `revision`, a `@Version`-backed optimistic-lock
counter starting at `0` on creation. A caller reads the current `revision`
and echoes it back as `expectedRevision` on the POST; the current row and an
immutable audit row are appended atomically in one transaction only when
that revision is still current.

## Concurrency contract

- `expectedRevision` must equal the liability's current `revision`. A stale
  value — including replaying an already-applied revision — is rejected with
  `409 LIABILITY_REVISION_CONFLICT` and appends nothing.
- Two concurrent submissions starting from the same revision resolve to
  exactly one `201` success and one `409`, with exactly one audit row
  appended for the winner. The losing request's optimistic-lock flush fails
  before its audit row would be inserted — see
  [D019](../../../docs/decisions/decisions.md#d019--bounded-balance-replacement-is-an-append-only-audit-not-event-sourcing).
- A successful replacement always increments `revision` by exactly `1`, and
  the appended audit row's `revision` equals the liability's new `revision`.

## Request (`POST .../balances`)

| Field                | Type    | Rules |
|----------------------|---------|-------|
| `outstandingBalance` | decimal | Required. Zero or greater. At most 17 integer digits and 2 fraction digits; a scale finer than 2 is rejected, not rounded. Increases are as valid as decreases. |
| `balanceAsOf`        | date    | Required. Not in the future. May be the same date, earlier, or later than the liability's current `balanceAsOf` — the supplied date is always preserved as given; history is never reordered by source date. |
| `reason`             | string  | Required, non-blank, at most 500 characters. |
| `expectedRevision`   | integer | Required. Must equal the liability's current `revision`. |

Any other field (e.g. a client-supplied `sourceType`) is rejected as
`400 MALFORMED_REQUEST` — the endpoint always re-asserts server-assigned
`MANUAL_ENTRY` provenance, matching the existing create-liability contract.

## Response

### `201 Created` (POST) — one `LiabilityBalanceHistoryResponse`

| Field                 | Meaning |
|-----------------------|---------|
| `id`                  | The history row's own identity. |
| `liabilityId`         | The liability this change belongs to. |
| `householdId`         | The owning household. |
| `currency`            | The liability's currency at the time of the change (never changes). |
| `previousBalance`     | Exact prior `outstandingBalance`, as a two-decimal string (e.g. `"500.00"`), not a JSON number — see D019 and the exact-decimal rationale already used by the financial-position endpoint. |
| `previousBalanceAsOf` | Exact prior `balanceAsOf`. |
| `previousSourceType`  | Exact prior `sourceType`. |
| `newBalance`          | The replacement balance, same string format as `previousBalance`. |
| `newBalanceAsOf`      | The replacement source date. |
| `newSourceType`       | Always `MANUAL_ENTRY` for this endpoint. |
| `reason`              | Echoed, trimmed. |
| `revision`            | The liability's resulting revision; unique and gap-free per liability. |
| `recordedAt`          | Server time the change was accepted. |

### `200 OK` (GET) — array of the same shape, oldest first (`revision` ascending)

Empty (`[]`) for a liability that has never been updated — no synthetic
"initial" history row is invented; `GET .../liabilities/{id}` is always the
source of a liability's current state, updated or not.

### Errors

| Status | `error`                        | When |
|--------|--------------------------------|------|
| 400    | `VALIDATION_FAILED`            | A field fails the rules above. |
| 400    | `MALFORMED_REQUEST`            | Unparseable JSON or an unsupported field (e.g. `sourceType`). |
| 404    | `HOUSEHOLD_NOT_FOUND`          | Unknown `householdId`. |
| 404    | `LIABILITY_NOT_FOUND`          | Unknown `liabilityId`, or one that belongs to a different household (no disclosure). |
| 409    | `LIABILITY_REVISION_CONFLICT`  | `expectedRevision` is not the liability's current revision. |

## Example

Request:

```json
{
  "outstandingBalance": "300.00",
  "balanceAsOf": "2026-09-10",
  "reason": "Paid down with September bonus",
  "expectedRevision": 0
}
```

Response (`201 Created`):

```json
{
  "id": "e184ca5b-11e5-4f52-b4ec-3719a174938b",
  "liabilityId": "2888d0f4-0f7f-40c4-b59b-6224396a3471",
  "householdId": "18ebc62a-1829-4930-bc41-4bf0b0538511",
  "currency": "PHP",
  "previousBalance": "500.00",
  "previousBalanceAsOf": "2026-09-01",
  "previousSourceType": "MANUAL_ENTRY",
  "newBalance": "300.00",
  "newBalanceAsOf": "2026-09-10",
  "newSourceType": "MANUAL_ENTRY",
  "reason": "Paid down with September bonus",
  "revision": 1,
  "recordedAt": "2026-09-10T17:04:35.997242Z"
}
```

Replaying the same request again (`expectedRevision: 0`, now stale) returns:

```json
{
  "error": "LIABILITY_REVISION_CONFLICT",
  "message": "Liability 2888d0f4-0f7f-40c4-b59b-6224396a3471 balance update rejected: revision 0 is not the current revision",
  "details": []
}
```

## Manually verified

Exercised against a running instance (`./mvnw spring-boot:run` against a
throwaway local `postgres:16-alpine` container on an isolated port/database,
matching `docker-compose.yml`'s connection settings) on 2026-09-11:

- Create household and liability, `GET` liability shows `revision: 0`.
- First balance replacement (`expectedRevision: 0`) → `201`, exact
  before/after decimals and dates as above, `GET` liability now shows the
  replaced `outstandingBalance`/`balanceAsOf` and `revision: 1`.
- Replaying the same now-stale `expectedRevision: 0` → `409
  LIABILITY_REVISION_CONFLICT`, no new history row appended.
- Second replacement with the current revision (`expectedRevision: 1`),
  including a zero balance (`"0.00"`) → `201`, `revision: 2`.
- `GET .../balances` returns both history rows in ascending `revision`
  order with the correct before/after pairs.
- Negative `outstandingBalance` → `400 VALIDATION_FAILED`.
- Blank `reason` → `400 VALIDATION_FAILED`.
- Unknown `householdId` → `404 HOUSEHOLD_NOT_FOUND`.
- Liability requested through a different household → `404
  LIABILITY_NOT_FOUND` (no disclosure of the record's existence).
- Unknown `liabilityId` under a valid household → `404 LIABILITY_NOT_FOUND`.
- A client-supplied `sourceType` field → `400 MALFORMED_REQUEST`.

Automated coverage (`./verify.sh`, 757/757 passing) additionally proves,
against real PostgreSQL:

- Two concurrent submissions from the same revision resolve to exactly one
  `201` and one `409`, with exactly one audit row appended
  (`LiabilityBalanceHistoryApiIntegrationTest.concurrentSubmissionsFromSameRevisionProduceExactlyOneSuccessAndOneConflict`).
- Deterministic revision ordering, zero-balance and balance-increase
  acceptance, excessive-fraction-scale rejection, and the empty-history
  state before any update.
