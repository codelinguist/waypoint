# Product Brief: Current Financial Position Read Model

## Status

`READY`

## Ownership and user input

Product Owner: Codex. Users: Ralph and his wife. Created 2026-09-06. Derived from the explicitly requested first financial-position frontend, linked in ../financial-position/product-brief.md.

## Product framing

Existing asset/liability list APIs expose records, while net-worth totals exist only on persisted snapshots. The first current-position page needs one coherent read of source rows and deterministic per-currency totals without writing snapshots or duplicating math in the browser. Success means each returned total exactly reconciles with the returned rows.

## Knowledge classification

Confirmed: existing Asset planningValue and Liability outstandingBalance define snapshot net worth; currencies have no FX policy. Assumption: the first screen shows all currently recorded rows, even future-dated rows, with source dates visible. It does not reconstruct history or assert those values are true today. No blocking household question.

## Scope and contract

- GET /api/households/{householdId}/financial-position; validate household with existing semantics.
- Return householdId, householdName, baseCurrency (metadata only), retrievedAt, assets, liabilities, totalsByCurrency.
- Copy asset fields id/name/assetType/estimatedValue/planningValue/currency/valuedAt/liquidity/sourceType and liability fields id/name/liabilityType/outstandingBalance/currency/balanceAsOf/sourceType into read-only DTOs.
- All money in this new endpoint, including row values and totals, uses exact decimal strings with two fractional digits. Do not change existing endpoints' serialization. Browser consumers must never lose cents through JavaScript Number coercion.
- Return one total per currency present in either source list, ordered by currency; assetTotal = sum(planningValue), liabilityTotal = sum(outstandingBalance), netWorth = assetTotal - liabilityTotal. Missing side contributes exact zero. No all-currency sum or implied FX conversion.
- Return rows deterministically ordered by UUID within kind. Empty existing households have empty lists and totals; do not invent a base-currency zero group or pretend records are complete.
- Derive totals from exactly the copied returned rows inside a coherent read transaction (e.g. PostgreSQL repeatable-read). Do not query totals independently or promise that retrievedAt is a valuation date.
- Java BigDecimal domain arithmetic, no numeric overflow/truncation when sums exceed individual NUMERIC(19,2) precision. Reuse suitable existing pure calculation code read-only if practical; do not refactor siblings.

## User flow

A trusted private caller requests one household; the API returns its recorded position and source metadata, or established validation/not-found errors. No record or snapshot is written.

## Acceptance criteria

- [ ] Synthetic mixed-currency assets and liabilities return reconciled per-currency sums from planningValue, not estimatedValue, with no FX aggregate.
- [ ] Asset-only, liability-only, empty and negative-net-worth cases behave as specified. Zero-valued source rows remain included.
- [ ] A 99999999999999999.99 row and aggregate values above individual storage precision round-trip as exact two-decimal strings, with no scientific notation or numeric truncation.
- [ ] Source dates, liquidity and provenance remain intact, future-dated rows remain explicit, and household name/baseCurrency are metadata rather than calculation rules.
- [ ] Unknown households return established 404; malformed identifiers use established 400; PostgreSQL integration tests prove household isolation and absence of writes/snapshot creation.
- [ ] Returned totals are calculated from returned rows; document and test the transaction consistency boundary so concurrent writes cannot mix different database read views.
- [ ] Deterministic ordering and all arithmetic/serialization behavior have focused domain/API tests. Existing APIs remain unchanged.
- [ ] ./verify.sh passes and synthetic manual API evidence, exact contract/examples and implementation notes are recorded locally to this feature.

## Non-goals and safeguards

No UI, migrations, canonical writes, new valuation history, snapshot creation, freshness thresholds, goal/scenario integration or household seeding. Test with isolated disposable data. Read-only implementation does not approve household finances.

## Product decisions

Return a dedicated current read model rather than calling snapshot creation. Use decimal strings for the new browser-facing contract while leaving existing contracts stable. These are reversible, bounded implementation-facing choices grounded in D002 and the existing snapshot valuation convention.

## Delivery handoff

- Task: agent/tasks/021-current-financial-position.md
- Own backend/src/main/java/com/waypoint/position/**, matching backend/src/test/java/com/waypoint/position/**, and agent/product/current-financial-position/**.
- Existing household services/repositories/entities may be consumed read-only. No shared handler, build, migration or shared-prose edits; use feature-scoped error handling if needed.
- Independent of Tasks 012–020. Record api.md and implementation-log.md locally with tests, assumptions, limitations and system-evolution recommendations.
- After this and design Task 020 are accepted/merged, queue the frontend contract stored outside the queue at agent/product/financial-position/implementation-task-draft.md.

## Feature acceptance

- Acceptance status: PENDING
- Evidence: Contract framing only.
- Unmet criteria: Implementation and verification.
