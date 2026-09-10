# Implementation Log: Current Financial Position API (WAP-14)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md`; see also the
future-value-calculator feature's log for the same precedent).

## Changed

Added `backend/src/main/java/com/waypoint/position/`:

- `PositionAssetRepository`, `PositionLiabilityRepository` — feature-local,
  read-only Spring Data repositories over the existing `Asset`/`Liability`
  entities. Order by `id` ascending, distinct from the household module's own
  `AssetRepository`/`LiabilityRepository`, which order by creation time. A
  new repository was needed (rather than reusing the household module's)
  because the product brief's ordering contract is UUID order, not creation
  order.
- `CurrencyTotalsCalculator` — package-private pure function computing
  per-currency `assetTotal`/`liabilityTotal`/`netWorth` from exactly the
  asset/liability rows handed to it. Mirrors `FinancialSnapshotService`'s
  per-currency reconciliation convention (sum by currency, missing side is
  exact zero, no cross-currency aggregate) without editing or calling into
  that snapshot-only sibling — it reads live `Asset`/`Liability` rows, not
  persisted snapshot line items, so the existing method's signature didn't
  fit. Returns the existing `com.waypoint.household.CurrencyTotals` record
  read-only, since its shape already matches exactly what this feature needs.
- `FinancialPositionResult` — domain result record (household, assets,
  liabilities, totals, `retrievedAt`).
- `FinancialPositionService` — `@Transactional(readOnly = true, isolation =
  Isolation.REPEATABLE_READ)`. One transaction does the household lookup and
  both row reads, so the returned totals are always derived from exactly the
  rows also returned.
- `web/FinancialPositionController` — `GET
  /api/households/{householdId}/financial-position`. No feature-scoped
  exception handler was needed: `HouseholdNotFoundException` (404) and a
  malformed UUID path variable (400) are already handled by the shared
  `ApiExceptionHandler`.
- `web/dto/PositionAssetResponse`, `PositionLiabilityResponse`,
  `PositionCurrencyTotalsResponse`, `FinancialPositionResponse`,
  `MoneyFormat` — response DTOs. Every monetary field is serialized as an
  exact two-decimal string (`BigDecimal.setScale(2,
  RoundingMode.UNNECESSARY).toPlainString()`), specific to this endpoint;
  existing endpoints' DTOs are untouched and keep serializing money as JSON
  numbers.

Added matching tests under `backend/src/test/java/com/waypoint/position/`
and this feature's `agent/product/current-financial-position/api.md`.

No shared file was edited: `ApiExceptionHandler`, `AssetRepository`/
`LiabilityRepository`, `FinancialSnapshotService`, migrations, build/
configuration, `README.md`, the central `agent/implementation-log.md`, and
`docs/decisions/decisions.md` are all untouched. No household type was
modified — only read via its existing repositories/entities.

## Tests

- `CurrencyTotalsCalculatorTest` (9 tests) — pure domain unit tests, no
  Spring context: mixed-currency reconciliation, asset-only/liability-only/
  empty/negative-net-worth cases, zero-valued rows included, no all-currency
  sum or implied conversion, currency ordering, and exact summation above
  individual `NUMERIC(19,2)` storage precision (two
  `99999999999999999.99` rows summing to `199999999999999999.98` with no
  truncation).
- `FinancialPositionApiIntegrationTest` (14 tests, `@SpringBootTest` +
  Testcontainers PostgreSQL) — the same scenarios through the real HTTP/JSON/
  database boundary, plus: decimal-string transport with no scientific
  notation on the precision-overflow case; source date/liquidity/provenance
  fields preserved; a future-dated row returned unfiltered (persisted
  directly via the repository, since the existing `POST /assets` endpoint's
  `@PastOrPresent` validation itself rejects a future `valuedAt` — this
  proves the read model does not apply the snapshot-creation module's
  eligibility filter); household metadata; 404 on an unknown household; 400
  on a malformed household id; household isolation; no write/snapshot
  creation (asserted by re-listing assets and financial-snapshots after two
  GETs); and deterministic UUID ordering, both within one response and
  stable across repeated reads.
- `FinancialPositionTransactionIsolationTest` (2 tests, Testcontainers
  PostgreSQL) — documents and tests the transaction consistency boundary the
  brief requires: a reflection test pins
  `FinancialPositionService`'s `@Transactional(readOnly = true, isolation =
  REPEATABLE_READ)` annotation so a future change silently weakening it fails
  loudly; a concurrency test opens a `REPEATABLE_READ` transaction against
  this feature's own repositories, lets a second thread commit a new
  liability between the asset read and the liability read, and confirms the
  liability read still does not see it (then confirms a fresh transaction
  does, proving the isolation — not a bug — caused the earlier miss).

## Manual verification

Ran `docker compose up --build`, then exercised the full flow with `curl`
against the real running stack: created a household with PHP and USD assets/
liabilities, confirmed the documented mixed-currency response shape and
exact decimal strings; confirmed the `99999999999999999.99` × 2 precision-
overflow case aggregates to `199999999999999999.98` live; confirmed 404 for
an unknown household and 400 for a malformed household id. `./verify.sh`
passes (599 tests).

## Assumptions

- "Deterministically ordered by UUID within kind" means database/repository
  order by the `id` column (Postgres's native unsigned byte-wise UUID
  comparison), not a client-side re-sort by canonical string — the two agree
  for random UUIDv4 values, since hex encoding preserves byte order.
- Zero-scale `BigDecimal.ZERO` used for a missing side is explicitly padded
  to scale 2 by `MoneyFormat.plain` before serialization, so `"0.00"` is
  always returned, never `"0"`.

## Limitations

- No pagination: all recorded rows for a household are returned in one
  response. Acceptable for a single-household, single-family product; would
  need revisiting if a household could accumulate very large row counts.
- No caching: every call re-reads and re-sums the household's rows. Fine for
  the current scale and matches the read-only, always-fresh contract the
  brief asks for.

## Unresolved questions

None blocking. The UI task (queued at
`agent/product/financial-position/implementation-task-draft.md` per the
brief's delivery handoff) can now consume this documented contract.

## Recommended next task

Queue the React/TypeScript current-financial-position UI implementation
task once this PR and Task 020's design are both accepted and merged, per
`docs/product/roadmap.md` Phase 9 and D015.

## System evolution

No rule, template, or doc change is recommended. The feature-scoped
exception-handler pattern (`@RestControllerAdvice(assignableTypes = ...)`)
and the feature-local repository-with-different-ordering pattern both
already existed in the codebase (`futurevalue`, `FinancialSnapshotService`)
and were reusable as-is.
