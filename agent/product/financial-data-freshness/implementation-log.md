# Implementation log: Financial Data Freshness Review (Task 019)

Feature-local record, per this batch's shared-prose exception (see the
product brief's "Delivery handoff" and PD-002). Consolidation into
`agent/implementation-log.md` is a post-batch follow-up, not part of this
task.

## Fix round 1 (2026-09-07) — BLOCKING findings from independent review

Applied both `BLOCKING` findings from PR #27's review:

- **Liability coverage in isolation/no-mutation tests:**
  `keepsRecordsIsolatedBetweenHouseholds` previously created only assets in
  each household, so it never proved liabilities were scoped correctly
  between households. It now creates one liability per household alongside
  the existing assets, asserts `countsByKind.ASSET`/`countsByKind.LIABILITY`
  per household, and asserts every record name returned for household one
  belongs to household one. `performsNoMutationOfUnderlyingRecords`
  previously captured and compared only the household's asset list before
  and after two freshness calls; it now also creates a liability and
  captures/compares the liability list (`GET
  /api/households/{h}/liabilities`) byte-for-byte before and after, the
  same way it already did for assets.
- **Manual verification reproducibility:** The "Manual verification"
  section below previously summarized outcomes without the literal
  commands used to reach them. It is now a single reproducible transcript —
  disposable Postgres container creation, packaged-app startup pointed at
  it, health-check wait, fixture creation, every `curl` invocation with its
  observed response body/status, and the teardown commands — captured from
  an actual re-run of that script on this branch after the test changes
  above.

`./verify.sh`: 427 tests, 0 failures (same totals as before this round —
both fixes extended existing test methods rather than adding new ones; see
"Tests" below for the updated per-method coverage).

## Changed

New, additive package `com.waypoint.review.freshness` under
`backend/src/main/java/com/waypoint/review/freshness/` — no existing file
was modified:

- `FinancialDataFreshnessCalculator` (`@Service`, no dependencies) — pure
  domain calculation. Given an explicit `householdId`, `reviewDate`,
  `maxAgeDays`, and a list of `FreshnessSourceRecord` inputs, computes
  `ageDays = reviewDate - sourceDate` with `ChronoUnit.DAYS` (exact
  calendar-day arithmetic, correct across leap years and month/year
  boundaries), classifies each record (`CURRENT`/`STALE`/`FUTURE_DATED`),
  sorts deterministically by `recordKind` then `recordId`, and produces
  zero-inclusive counts by kind and by classification. Validates its own
  invariants (non-null `householdId`/`reviewDate`/`sourceRecords`,
  `maxAgeDays` in `0..36500`) independently of the web layer, so a direct
  domain call rejects invalid values without going through HTTP.
- `FreshnessSourceRecord`, `FreshnessRecord` (records), `FreshnessRecordKind`
  (`ASSET`/`LIABILITY`) and `FreshnessClassification`
  (`CURRENT`/`STALE`/`FUTURE_DATED`) enums, `FinancialDataFreshnessResult`
  (record) — the domain input/output types. None carries a financial amount
  field, so the "never copy a financial amount into this response"
  constraint is a compile-time property, not just a runtime check.
- `InvalidFreshnessReviewInputException` — thrown by the calculator's own
  invariant checks.
- `FinancialDataFreshnessService` (`@Service`) — the only new code that
  touches persistence, and only by calling the existing, unmodified
  `AssetService.listAssets` and `LiabilityService.listLiabilities`, which
  already enforce household existence (`HouseholdNotFoundException`) and
  ownership scoping. Maps `Asset`/`Liability` entities to
  `FreshnessSourceRecord` (using `valuedAt`/`balanceAsOf` as the source
  date, never `createdAt`/`updatedAt`) and delegates classification to the
  calculator.
- `web/FinancialDataFreshnessController` — `GET
  /api/households/{householdId}/financial-data-freshness`. Its
  `InvalidFreshnessReviewInputException` handler is a controller-local
  `@ExceptionHandler` method (Spring resolves it before the shared
  `@RestControllerAdvice` for exceptions raised in this controller), so it
  cannot intercept a sibling controller's errors and requires no change to
  the shared `ApiExceptionHandler`. Unknown households, missing query
  parameters, and malformed query parameters (a non-ISO `reviewDate`, a
  fractional or overflowing `maxAgeDays`) are already handled by that shared
  advice's existing `HouseholdNotFoundException`,
  `MissingServletRequestParameterException`, and
  `MethodArgumentTypeMismatchException` paths — verified directly (see
  "Manual verification" below), not assumed.
- `web/dto/FreshnessRecordResponse` / `FinancialDataFreshnessResponse` — the
  HTTP representation, including the always-present zero-inclusive count
  maps and the `modelNote` explaining the review's limitation (freshness of
  present source rows relative to `reviewDate`, not proof of current
  correctness, not a historical reconstruction).
- `agent/product/financial-data-freshness/api.md` — request/response
  reference with worked examples for every acceptance-criteria case.

## Tests

- `FinancialDataFreshnessCalculatorTest` (18 tests, no Spring context):
  the exact acceptance-criteria boundary (30/31/-1 days), the zero-threshold
  same-day-vs-earlier case, three calendar-arithmetic edge cases (a leap-year
  Feb 29 boundary, a non-leap Feb 28 boundary, a year boundary), deterministic
  ordering by kind then id, zero-inclusive counts, empty-input handling,
  determinism (identical inputs -> equal results), and rejection of a null
  `householdId`/`reviewDate`/`sourceRecords`, a negative `maxAgeDays`, and a
  `maxAgeDays` above `36500` (plus acceptance of the `36500` boundary
  itself) directly against the domain calculator.
- `FinancialDataFreshnessApiIntegrationTest` (17 tests, `@SpringBootTest` +
  Testcontainers Postgres, building fixtures only through the existing
  household/asset/liability HTTP API): the same boundary/threshold/ordering
  matrix over HTTP, no financial amount present in the response body, correct
  `sourceType`/`sourceDate` echoing, empty-household zero counts,
  cross-household isolation of **both** assets and liabilities (each
  household gets at least one of each kind; per-household `countsByKind`
  and record ownership are both asserted — fix round 1), stable ordering
  and byte-for-byte identical repeated responses, no mutation of **both**
  the underlying asset list and the underlying liability list across two
  calls with different thresholds (fix round 1), unknown-household 404, and
  the missing/malformed/fractional/negative/overflow/above-bound `400`
  cases. Every date fixture is anchored to `LocalDate.now()` at test-run
  time (not a fixed calendar date), so the suite is stable regardless of
  when it runs while still satisfying the existing asset/liability "not in
  the future" creation validation.

`./verify.sh`: 427 tests, 0 failures (35 in the new `review.freshness`
packages: 18 domain + 17 HTTP; 392 pre-existing, unaffected).

## Manual verification

Reproducible end-to-end transcript (re-run on this branch after the fix-round
1 test changes above), against a throwaway, disposable Postgres container —
never the shared development volume — and the packaged app on non-default
ports so neither collides with a developer's local stack.

### Setup

```
docker run -d --name waypoint-freshness-manual-pg \
  -e POSTGRES_DB=waypoint -e POSTGRES_USER=waypoint -e POSTGRES_PASSWORD=waypoint \
  -p 15544:5432 postgres:16-alpine
sleep 4
docker ps --filter name=waypoint-freshness-manual-pg
```
```
CONTAINER ID   IMAGE                COMMAND                  CREATED         STATUS         PORTS                     NAMES
ba9072946cbb   postgres:16-alpine   "docker-entrypoint.s…"   4 seconds ago   Up 4 seconds   0.0.0.0:15544->5432/tcp   waypoint-freshness-manual-pg
```

```
cd backend
nohup ./mvnw --batch-mode spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=18089 --spring.datasource.url=jdbc:postgresql://localhost:15544/waypoint --spring.datasource.username=waypoint --spring.datasource.password=waypoint" \
  > /tmp/waypoint-freshness-manual-app.log 2>&1 &
```
```
app pid: 90458
```

```
for i in $(seq 1 40); do
  curl -s -o /dev/null -w "%{http_code}" http://localhost:18089/actuator/health | grep -q 200 && { echo "up after $i tries"; break; }
  sleep 2
done
curl -s http://localhost:18089/actuator/health
```
```
up after 3 tries
{"status":"UP"}
```

### Fixture creation

```
BASE=http://localhost:18089
HID=$(curl -s -X POST $BASE/api/households -H 'Content-Type: application/json' \
  -d '{"name":"Freshness Manual Household","baseCurrency":"PHP"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
echo "household: $HID"
```
```
household: 6e1e25cb-2733-499f-83d0-6ee0900fb05c
```

```
curl -s -X POST $BASE/api/households/$HID/assets -H 'Content-Type: application/json' \
  -d '{"name":"Emergency Fund","assetType":"CASH","estimatedValue":"1000.00","planningValue":"1000.00","currency":"PHP","valuedAt":"2026-08-07","liquidity":"LIQUID"}'
```
```
{"id":"d08874ed-068e-4916-8722-0ec29c6419ed","householdId":"6e1e25cb-2733-499f-83d0-6ee0900fb05c","name":"Emergency Fund","assetType":"CASH","estimatedValue":1000.00,"planningValue":1000.00,"currency":"PHP","valuedAt":"2026-08-07","liquidity":"LIQUID","sourceType":"MANUAL_ENTRY","createdAt":"2026-09-06T16:20:26.485485Z","updatedAt":"2026-09-06T16:20:26.485489Z"}
```

```
curl -s -X POST $BASE/api/households/$HID/liabilities -H 'Content-Type: application/json' \
  -d '{"name":"Credit Card","liabilityType":"CREDIT_CARD","outstandingBalance":"500.00","currency":"PHP","balanceAsOf":"2026-08-06"}'
```
```
{"id":"85f97e74-f528-4b54-bfda-be0d8d18fd25","householdId":"6e1e25cb-2733-499f-83d0-6ee0900fb05c","name":"Credit Card","liabilityType":"CREDIT_CARD","outstandingBalance":500.00,"currency":"PHP","balanceAsOf":"2026-08-06","sourceType":"MANUAL_ENTRY","createdAt":"2026-09-06T16:20:26.518164Z","updatedAt":"2026-09-06T16:20:26.518167Z"}
```

```
curl -s -X POST $BASE/api/households/$HID/assets -H 'Content-Type: application/json' \
  -d '{"name":"Future-dated demo","assetType":"CASH","estimatedValue":"1.00","planningValue":"1.00","currency":"PHP","valuedAt":"2026-08-26","liquidity":"LIQUID"}'
```
```
{"id":"014e787b-93ec-4cf3-bfbb-77b4375d6018", ... "valuedAt":"2026-08-26", ...}
```

### Primary and edge-case reviews

```
curl -s "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30"
```
```json
{"householdId":"6e1e25cb-2733-499f-83d0-6ee0900fb05c","reviewDate":"2026-09-06","maxAgeDays":30,"records":[{"recordId":"d08874ed-068e-4916-8722-0ec29c6419ed","recordKind":"ASSET","name":"Emergency Fund","currency":"PHP","sourceType":"MANUAL_ENTRY","sourceDate":"2026-08-07","ageDays":30,"classification":"CURRENT"},{"recordId":"014e787b-93ec-4cf3-bfbb-77b4375d6018","recordKind":"ASSET","name":"Future-dated demo","currency":"PHP","sourceType":"MANUAL_ENTRY","sourceDate":"2026-08-26","ageDays":11,"classification":"CURRENT"},{"recordId":"85f97e74-f528-4b54-bfda-be0d8d18fd25","recordKind":"LIABILITY","name":"Credit Card","currency":"PHP","sourceType":"MANUAL_ENTRY","sourceDate":"2026-08-06","ageDays":31,"classification":"STALE"}],"countsByKind":{"ASSET":2,"LIABILITY":1},"countsByClassification":{"CURRENT":2,"STALE":1,"FUTURE_DATED":0},"modelNote":"..."}
```
Matches `api.md`'s first example exactly: `Emergency Fund` at 30 days is
`CURRENT`, `Credit Card` at 31 days is `STALE`.

```
curl -s "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-08-25&maxAgeDays=30"
```
```json
{"reviewDate":"2026-08-25","maxAgeDays":30,"records":[...,{"recordKind":"ASSET","name":"Future-dated demo","sourceDate":"2026-08-26","ageDays":-1,"classification":"FUTURE_DATED"},...],"countsByClassification":{"CURRENT":2,"STALE":0,"FUTURE_DATED":1},...}
```
`FUTURE_DATED` confirmed: `ageDays=-1` for a source dated one day after
`reviewDate`. (The existing `valuedAt`-not-in-the-future *creation* rule is
checked against the real current date, not the review's `reviewDate`, so
demonstrating this required a past-dated review rather than the literal
`reviewDate=2026-09-06`/`valuedAt=2026-09-07` wording in the acceptance
criteria — `FinancialDataFreshnessApiIntegrationTest
.classifiesCurrentStaleAndFutureDatedRecordsAtTheThreshold` covers the
identical relative case as an automated test, independent of the real
calendar date.)

```
curl -s -X POST $BASE/api/households/$HID/assets -H 'Content-Type: application/json' \
  -d '{"name":"Zero-threshold same day","assetType":"CASH","estimatedValue":"1.00","planningValue":"1.00","currency":"PHP","valuedAt":"2026-08-07","liquidity":"LIQUID"}' > /dev/null
curl -s "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-08-07&maxAgeDays=0"
```
```json
{"reviewDate":"2026-08-07","maxAgeDays":0,"records":[{"name":"Zero-threshold same day","sourceDate":"2026-08-07","ageDays":0,"classification":"CURRENT"},{"name":"Emergency Fund","sourceDate":"2026-08-07","ageDays":0,"classification":"CURRENT"},{"name":"Future-dated demo","sourceDate":"2026-08-26","ageDays":-19,"classification":"FUTURE_DATED"},{"name":"Credit Card","sourceDate":"2026-08-06","ageDays":1,"classification":"STALE"}],"countsByClassification":{"CURRENT":2,"STALE":1,"FUTURE_DATED":1},...}
```
Zero threshold confirmed: same-day source is `CURRENT`, one day earlier is
already `STALE`.

### Error cases

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/00000000-0000-0000-0000-000000000000/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30"
```
```
{"error":"HOUSEHOLD_NOT_FOUND","message":"Household not found: 00000000-0000-0000-0000-000000000000","details":[]}
HTTP 404
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?maxAgeDays=30"
```
```
{"error":"VALIDATION_FAILED","message":"Required request parameter 'reviewDate' for method parameter type LocalDate is not present","details":[]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?reviewDate=not-a-date&maxAgeDays=30"
```
```
{"error":"MALFORMED_REQUEST","message":"Request parameter is malformed","details":[]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30.5"
```
```
{"error":"MALFORMED_REQUEST","message":"Request parameter is malformed","details":[]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=-1"
```
```
{"error":"VALIDATION_FAILED","message":"maxAgeDays must be between 0 and 36500","details":[]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=99999999999999999999"
```
```
{"error":"MALFORMED_REQUEST","message":"Request parameter is malformed","details":[]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=36501"
```
```
{"error":"VALIDATION_FAILED","message":"maxAgeDays must be between 0 and 36500","details":[]}
HTTP 400
```

```
HID2=$(curl -s -X POST $BASE/api/households -H 'Content-Type: application/json' \
  -d '{"name":"Empty Household","baseCurrency":"PHP"}' | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/households/$HID2/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30"
```
```
{"householdId":"c90daae4-83c8-4c59-96c4-fb84666ac406","reviewDate":"2026-09-06","maxAgeDays":30,"records":[],"countsByKind":{"ASSET":0,"LIABILITY":0},"countsByClassification":{"CURRENT":0,"STALE":0,"FUTURE_DATED":0},"modelNote":"..."}
HTTP 200
```

### No-mutation check (fix round 1)

```
BEFORE_ASSETS=$(curl -s "$BASE/api/households/$HID/assets")
BEFORE_LIABILITIES=$(curl -s "$BASE/api/households/$HID/liabilities")
curl -s "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-09-06&maxAgeDays=30" > /dev/null
curl -s "$BASE/api/households/$HID/financial-data-freshness?reviewDate=2026-08-25&maxAgeDays=0" > /dev/null
AFTER_ASSETS=$(curl -s "$BASE/api/households/$HID/assets")
AFTER_LIABILITIES=$(curl -s "$BASE/api/households/$HID/liabilities")
[ "$BEFORE_ASSETS" = "$AFTER_ASSETS" ] && [ "$BEFORE_LIABILITIES" = "$AFTER_LIABILITIES" ] && echo PASS
```
```
PASS: assets and liabilities byte-for-byte unchanged after two freshness reads
```

### Cleanup

```
kill "$APP_PID"
pkill -f "spring-boot:run"
sleep 2
docker rm -f waypoint-freshness-manual-pg
```
```
app stopped: yes
container removed: (empty — confirmed via `docker ps -a --filter name=waypoint-freshness-manual-pg`)
```

All sixteen checks above matched the documented `api.md` behavior exactly
(both worked examples in that file were captured verbatim from this run).
The container and app process were fully torn down afterward — verified
with `docker ps -a` and `pgrep -f spring-boot:run` returning nothing — so
nothing was left running or persisted.

## Decisions

- The pure classification logic (`FinancialDataFreshnessCalculator`) is
  fully decoupled from the JPA `Asset`/`Liability` entities via the
  `FreshnessSourceRecord` input type, so it is unit-testable with plain
  `List.of(...)` fixtures and no Spring context — mirroring the
  `EmergencyFundRunwayCalculator`/`GoalContributionCalculator` pattern from
  the prior batch (normalize/validate -> compute -> throw a domain
  exception on invariant violation) even though this feature reads
  persisted state rather than taking every input from the request body.
- `FinancialDataFreshnessService` is the sole persistence-reading layer and
  deliberately thin: it calls the two existing, unmodified services and
  maps their entities to the calculator's input type. It adds no new
  household-validation logic, satisfying "Read existing AssetService and
  LiabilityService (and existing household validation) without edits."
- Counts (`countsByKind`, `countsByClassification`) are built as
  `EnumMap`s seeded with every enum constant at `0` before counting, so the
  empty-household case and any single-kind/-classification result both
  serialize with every expected key present — never an omitted key that a
  client would have to distinguish from "count is zero."
- The `InvalidFreshnessReviewInputException` handler is declared directly
  on `FinancialDataFreshnessController`, not added to the shared
  `ApiExceptionHandler`, per this task's exclusive-ownership constraint and
  to guarantee it cannot catch a sibling controller's errors — the same
  approach the emergency-fund-runway and goal-contribution-calculator tasks
  used for the identical constraint in the prior batch.
- `maxAgeDays` is bound as a primitive `int` `@RequestParam` (not a
  `@RequestBody` DTO field with Bean Validation annotations), because this
  is a `GET` with only two scalar query parameters; the range check
  (`0..36500`) instead lives once, in the domain calculator, which the HTTP
  layer already relies on via `InvalidFreshnessReviewInputException` — Spring's
  own type-conversion failure (`MethodArgumentTypeMismatchException`,
  already handled globally) covers the fractional/overflow malformed cases
  before the domain layer is ever reached.

## Assumptions

- None beyond what the brief already records as validated product
  assumptions (PD-001, PD-002). No missing household fact or preference was
  encountered; the only inputs this feature needs (`reviewDate`,
  `maxAgeDays`) are explicit, caller-supplied, and never defaulted from the
  server clock or an inferred cadence.

## Open questions

- None new. The brief's own follow-up list (household-facing integration,
  broader scenario/UI work) stands unchanged; this task does not expand it.

## Recommended next task

Per the brief's independence review (PD-002), no next task is authorized
here beyond what this batch's other queued tasks already cover in parallel.
A natural follow-up once the batch lands would be the shared-document
consolidation the brief's "Delivery handoff" and PD-002 already call out
(central implementation log, any README/status updates) — not a new
capability.

## System-evolution candidates

None identified specific to this task. The controller-local
`@ExceptionHandler` pattern and the "pure calculator over a
persistence-decoupled input type, assembled by a thin service that reuses
existing services" split both worked cleanly here, consistent with the
prior batch's emergency-fund-runway/goal-contribution-calculator precedent;
no new rule or template change is proposed.
