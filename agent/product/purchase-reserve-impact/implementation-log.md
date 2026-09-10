# Implementation log: Purchase Impact on Cash Reserves (WAP-10)

Feature-local record, per this task's exclusive-ownership constraint ("no
... central log ... shared-prose edits. Record implementation and
system-evolution findings in the feature-local log for post-batch
consolidation"). Consolidation into `agent/implementation-log.md` is a
follow-up, not part of this task. The Jira issue WAP-10 imported this
feature's legacy `agent/tasks/016-purchase-reserve-impact.md`/product-brief
content as its description per D017; this log otherwise follows the current
`agent/collaboration-workflow.md` Implement stage, not the imported task
file's superseded orchestrator-lifecycle language (see D016).

## Changed

New, additive package `com.waypoint.scenarios.purchasereserve` under
`backend/src/main/java/com/waypoint/scenarios/purchasereserve/` — no
existing file was modified:

- `PurchaseReserveImpactCalculator` (`@Service`, depends only on the
  existing, unmodified `EmergencyFundRunwayCalculator` bean) — pure domain
  calculation. Given explicit `currency`, `availableReserve`,
  `purchaseAmount`, `monthlyExpenses`, `monthlyNetIncome`, and
  `minimumReserve`, computes `reserveAfterPurchase = availableReserve -
  purchaseAmount` (signed, may be negative), `purchaseFundingGap = max(0,
  -reserveAfterPurchase)`, `baselineReserveFloorGap = max(0, minimumReserve -
  availableReserve)`, and `reserveFloorGapAfterPurchase = max(0,
  minimumReserve - reserveAfterPurchase)`, so a pre-existing floor breach is
  always distinguishable from the purchase's own impact. Calls
  `EmergencyFundRunwayCalculator.calculate` directly (a plain Java method
  call, not internal HTTP) once for the before-purchase reserve and, only
  when `purchaseAmount <= availableReserve`, again for the after-purchase
  reserve — the runway calculator's own `FINITE`/`NO_SHORTFALL` and
  null-runway conventions pass through unchanged. When the purchase exceeds
  the reserve, the after-purchase reserve is negative, which the runway
  calculator would reject; rather than clamp it to zero, that case is
  reported as `AfterPurchaseRunwayAvailability.INSUFFICIENT_CASH` with a
  `null` after-purchase runway. Validates its own invariants (each amount
  non-negative, at most 17 integer and 2 fraction digits including the
  negative-scale-representation case, e.g. `1E+17`; currency exactly 3
  ASCII letters, uppercased with `Locale.ROOT` before any case change)
  independently of the web layer, mirroring
  `EmergencyFundRunwayCalculator`'s own pattern so a direct domain call
  rejects invalid values without going through HTTP.
- `PurchaseReserveImpactResult` (record), `AfterPurchaseRunwayAvailability`
  (`AVAILABLE`/`INSUFFICIENT_CASH`) — the domain output types. The result
  embeds the runway calculator's own `EmergencyFundRunwayResult` for
  `beforePurchaseRunway`/`afterPurchaseRunway` rather than duplicating its
  fields.
- `InvalidPurchaseReserveImpactInputException` — thrown by the calculator's
  own invariant checks.
- `web/PurchaseReserveImpactController` — `POST
  /api/scenarios/purchase-reserve-impact`. Its
  `InvalidPurchaseReserveImpactInputException` handler is a controller-local
  `@ExceptionHandler` method (Spring resolves it before the shared
  `@RestControllerAdvice` for exceptions raised in this controller, per the
  existing `EmergencyFundRunwayController` pattern), so it cannot intercept
  a sibling controller's errors and requires no change to the shared
  `ApiExceptionHandler`. Malformed JSON bodies and per-field Bean Validation
  failures (`@NotNull`/`@DecimalMin`/`@Digits`/`@Pattern` on the request DTO)
  are already handled by the shared `ApiExceptionHandler`'s existing
  `HttpMessageNotReadableException`/`MethodArgumentNotValidException` paths
  — verified directly (see "Manual verification" below), not assumed.
- `web/dto/PurchaseReserveImpactRequest` — the HTTP request shape, with Bean
  Validation constraints mirroring `EmergencyFundRunwayRequest`'s convention
  for every decimal field (non-negative, at most 17 integer/2 fraction
  digits) plus the same currency pattern.
- `web/dto/PurchaseReserveImpactResponse` — the HTTP response shape. Reuses
  `com.waypoint.planning.runway.web.dto.EmergencyFundRunwayResponse.from(...)`
  read-only (an existing static factory method, not a new dependency edge
  into the runway module's internals) to render `beforePurchaseRunway` and
  `afterPurchaseRunway` in the runway feature's own documented JSON shape,
  including its `status`/`runwayMonths`/`fullMonthsCovered`/`modelNote`
  fields, instead of re-deriving that shape here.
- `agent/product/purchase-reserve-impact/api.md` — request/response
  reference with worked examples for every acceptance-criteria case.

## Tests

- `PurchaseReserveImpactCalculatorTest` (24 tests, no Spring context): the
  exact documented primary scenario (reserve 1000 / purchase 400 / expenses
  300 / income 100 / floor 800 -> remaining cash 600, funding gap 0,
  baseline floor gap 0, after floor gap 200, runway 5 -> 3 months); a
  purchase above the reserve (negative cash balance, exact funding gap,
  `INSUFFICIENT_CASH`, `null` after-runway, but a still-computed
  before-runway); a purchase exactly equal to the reserve (valid
  zero-reserve after-calculation, not an error); a zero purchase preserving
  the baseline reserve, floor gap, and both runways (asserted equal, not
  just equivalent); income covering expenses preserving `NO_SHORTFALL` and
  `null` `runwayMonths`/`fullMonthsCovered` on *both* the before- and
  after-purchase runway; a zero floor never producing a floor gap; an
  already-breached floor whose baseline and after-purchase gaps are both
  reported and differ; an arbitrary-decimal exact funding gap
  (250.75 - 300.00 -> -49.25 / 49.25); determinism (identical inputs ->
  equal results, via record `equals`); currency case-normalization
  independent of the JVM default locale (Turkish-locale dotless-i case) and
  rejection of a 2-character code that expands to 3 letters under
  `Locale.US` case-folding (`ßa`); and rejection of null/negative amounts
  for every field, a null/blank/malformed currency, excessive fraction
  digits, excessive integer digits, and the negative-scale-representation
  bypass (`1E+17`).
- `PurchaseReserveImpactApiIntegrationTest` (13 tests, `@WebMvcTest`, no
  Postgres/Testcontainers boundary — this endpoint has no persistence
  dependency, matching the `EmergencyFundRunwayApiIntegrationTest`/
  `GoalContributionApiIntegrationTest` precedent): the documented primary
  scenario end-to-end over HTTP including the nested runway response shape;
  `INSUFFICIENT_CASH` with the `afterPurchaseRunway` field asserted absent
  (not merely `null` unasserted) when the purchase exceeds the reserve;
  `NO_SHORTFALL`/`null` semantics preserved on both nested runway objects;
  byte-for-byte identical repeated responses; missing/malformed currency,
  negative `purchaseAmount`, negative `minimumReserve`, missing
  `minimumReserve`, excessive fraction/integer digits, and a malformed JSON
  body (`MALFORMED_REQUEST`) all returning `400` with no `500`; and a zero
  purchase with a zero floor accepted and correctly reflected in the
  response.

`./verify.sh`: 464 tests, 0 failures (37 new in the
`scenarios.purchasereserve` packages: 24 domain + 13 HTTP; 427 pre-existing,
unaffected).

## Manual verification

Ran the packaged app locally against a throwaway, disposable Postgres
container (`docker run ... postgres:16-alpine`, not the shared development
volume), on a non-default port, then exercised the endpoint directly with
`curl` for every acceptance-criteria case plus the invalid-input cases.

### Setup

```
docker run -d --name waypoint-purchase-reserve-manual-pg \
  -e POSTGRES_DB=waypoint -e POSTGRES_USER=waypoint -e POSTGRES_PASSWORD=waypoint \
  -p 15546:5432 postgres:16-alpine
```
```
CONTAINER ID   IMAGE                COMMAND                  CREATED         STATUS         PORTS                     NAMES
fa5d95702e49   postgres:16-alpine   "docker-entrypoint.s…"   4 seconds ago   Up 4 seconds   0.0.0.0:15546->5432/tcp   waypoint-purchase-reserve-manual-pg
```

```
cd backend
nohup ./mvnw --batch-mode spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=18091 --spring.datasource.url=jdbc:postgresql://localhost:15546/waypoint --spring.datasource.username=waypoint --spring.datasource.password=waypoint" \
  > /tmp/waypoint-purchase-reserve-manual-app.log 2>&1 &
```
```
app pid: 55043
```

```
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:18091/actuator/health
```
```
200
```

### Primary scenario (matches `api.md`'s first example exactly)

```
curl -s -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"400.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"php"}'
```
```json
{"currency":"PHP","availableReserve":1000.00,"purchaseAmount":400.00,"monthlyExpenses":300.00,"monthlyNetIncome":100.00,"minimumReserve":800.00,"reserveAfterPurchase":600.00,"purchaseFundingGap":0.00,"purchaseFitsAvailableCash":true,"baselineReserveFloorGap":0.00,"reserveFloorGapAfterPurchase":200.00,"reserveMeetsFloorAfterPurchase":false,"beforePurchaseRunway":{"status":"FINITE","runwayMonths":5.00,"fullMonthsCovered":5,...},"afterPurchaseRunwayAvailability":"AVAILABLE","afterPurchaseRunway":{"status":"FINITE","runwayMonths":3.00,"fullMonthsCovered":3,...}}
```
Matches the acceptance criterion exactly: remaining cash 600, funding gap 0,
baseline floor gap 0, after floor gap 200, runway 5 -> 3 months.

### Purchase exceeds the reserve

```
curl -s -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"1200.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"USD"}'
```
```json
{"reserveAfterPurchase":-200.00,"purchaseFundingGap":200.00,"purchaseFitsAvailableCash":false,"reserveFloorGapAfterPurchase":1000.00,"afterPurchaseRunwayAvailability":"INSUFFICIENT_CASH","afterPurchaseRunway":null,"beforePurchaseRunway":{"status":"FINITE","runwayMonths":5.00,"fullMonthsCovered":5,...}}
```
Negative cash balance, exact funding gap, explicit `INSUFFICIENT_CASH`, and
a still-computed before-purchase runway from the unaffected reserve.

### Purchase exactly equal to the reserve

```
curl -s -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"1000.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"USD"}'
```
```json
{"reserveAfterPurchase":0.00,"purchaseFundingGap":0.00,"purchaseFitsAvailableCash":true,"afterPurchaseRunwayAvailability":"AVAILABLE","afterPurchaseRunway":{"availableReserve":0.00,"status":"FINITE","runwayMonths":0.00,"fullMonthsCovered":0,...}}
```
A valid zero-reserve calculation, not an error.

### Zero purchase and income covering expenses

```
curl -s -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"500.00","purchaseAmount":"0","monthlyExpenses":"200.00","monthlyNetIncome":"400.00","minimumReserve":"300.00","currency":"USD"}'
```
```json
{"reserveAfterPurchase":500.00,"purchaseFundingGap":0.00,"baselineReserveFloorGap":0.00,"reserveFloorGapAfterPurchase":0.00,"reserveMeetsFloorAfterPurchase":true,"beforePurchaseRunway":{"status":"NO_SHORTFALL","runwayMonths":null,"fullMonthsCovered":null,...},"afterPurchaseRunwayAvailability":"AVAILABLE","afterPurchaseRunway":{"status":"NO_SHORTFALL","runwayMonths":null,"fullMonthsCovered":null,...}}
```
Baseline preserved exactly by the zero purchase; `NO_SHORTFALL`/`null`
semantics preserved on both nested runway objects, not converted to zero.

### Error cases

```
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"-1.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"USD"}'
```
```
{"error":"VALIDATION_FAILED","message":"Request validation failed","details":["purchaseAmount: purchaseAmount must not be negative"]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"400.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"US1"}'
```
```
{"error":"VALIDATION_FAILED","message":"Request validation failed","details":["currency: currency must be a 3-letter currency code"]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.001","purchaseAmount":"400.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","minimumReserve":"800.00","currency":"USD"}'
```
```
{"error":"VALIDATION_FAILED","message":"Request validation failed","details":["availableReserve: availableReserve must have at most 17 integer digits and 2 fraction digits"]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' \
  -d '{"availableReserve":"1000.00","purchaseAmount":"400.00","monthlyExpenses":"300.00","monthlyNetIncome":"100.00","currency":"USD"}'
```
```
{"error":"VALIDATION_FAILED","message":"Request validation failed","details":["minimumReserve: minimumReserve must not be null"]}
HTTP 400
```

```
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:18091/api/scenarios/purchase-reserve-impact \
  -H 'Content-Type: application/json' -d 'not json'
```
```
{"error":"MALFORMED_REQUEST","message":"Request body is malformed","details":[]}
HTTP 400
```

Every case returned `400` with the structured error convention — never a
`500` — matching `api.md`'s documented behavior exactly (both worked
examples in that file were captured verbatim from this run).

### Cleanup

```
pkill -9 -f "server.port=18091"
docker rm -f waypoint-purchase-reserve-manual-pg
```
```
app stopped: yes (confirmed via ps -p 55043 returning nothing)
container removed: waypoint-purchase-reserve-manual-pg (confirmed via docker ps -a --filter name=... returning no rows)
```

## Decisions

- The nested `beforePurchaseRunway`/`afterPurchaseRunway` response fields
  reuse `EmergencyFundRunwayResponse.from(...)` directly rather than
  introducing a parallel DTO shape for the same data, so the runway
  feature's documented JSON contract (including its `modelNote`) is
  inherited verbatim instead of risking drift between two representations
  of the same calculator's output.
- `AfterPurchaseRunwayAvailability` is a new, feature-local enum rather than
  an extension of the runway module's own `RunwayStatus`, since
  `INSUFFICIENT_CASH` describes *this* feature's own precondition (the
  purchase exceeds the reserve) rather than a runway-calculator outcome —
  extending `RunwayStatus` would have required editing a file outside this
  task's exclusive-ownership paths.
- `purchaseFitsAvailableCash` and `reserveMeetsFloorAfterPurchase` are
  returned as plain booleans alongside the numeric gaps (rather than only
  the gaps) to satisfy the brief's "return neutral facts: whether purchase
  fits supplied cash and whether remaining cash meets supplied floor"
  requirement as an explicit, named field — while still avoiding any
  approval/denial/recommendation language.
- `PurchaseReserveImpactCalculator` computes `beforePurchaseRunway`
  unconditionally, including when the purchase exceeds the reserve, since
  `availableReserve` alone is always a valid runway-calculator input
  regardless of `purchaseAmount`; only the after-purchase call is
  conditional on `purchaseFitsAvailableCash`.

## Assumptions

- None beyond what the brief already records as validated product
  assumptions (PD-001, PD-002). No household fact or preference was
  invented; `minimumReserve` is always a caller-supplied scenario input,
  never defaulted from a household policy.

## Open questions

- None new. The brief's own follow-up list (household-facing integration
  after these bounded APIs are accepted) stands unchanged; this task does
  not expand it.

## Recommended next task

Per the brief's independence review (PD-002), no next task is authorized
here. A natural follow-up once this and its sibling bounded scenario/review
tasks land would be the shared-document consolidation already called out in
those tasks' own logs (central `agent/implementation-log.md`, any
README/status updates) — not a new capability.

## System-evolution candidates

None identified specific to this task. The "pure calculator with its own
independent invariant checks, reused by a thin controller with a
controller-local exception handler" pattern, and reusing a sibling
feature's response DTO read-only for a nested field, both worked cleanly
here, consistent with the `emergency-fund-runway`/`goal-contribution-
calculator`/`financial-data-freshness` precedent; no new rule or template
change is proposed.
