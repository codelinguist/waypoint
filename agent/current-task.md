# Current Task

## Task 005 — Financial Position Snapshots

### Goal

Add immutable, household-scoped financial position snapshots that capture
eligible asset and liability line items as of a requested date and calculate
deterministic net-worth totals separately within each original currency.

Detailed requirements and acceptance criteria:

- `agent/product/financial-snapshots/product-brief.md`

### Outcome

A trusted caller can create and retrieve an auditable snapshot of the
household's known balance-sheet state without mutating current records or
inventing cross-currency totals.

### Required deliverables

1. Add Flyway migrations and explicit snapshot header/line-item entities linked
   to households and source asset/liability records by retained UUID metadata.
2. Add validated create/get/list APIs with structured errors, exact decimal
   handling, household isolation, deterministic ordering, and server-assigned
   `MANUAL_ENTRY` provenance.
3. Filter asset `valuedAt` and liability `balanceAsOf` by `asOfDate`, include
   boundary dates, and preserve copied values so snapshots cannot change when
   source records change.
4. Add deterministic per-currency asset, liability, and net-worth totals with
   no FX conversion or cross-currency aggregate.
5. Add focused unit and PostgreSQL/Testcontainers integration tests covering
   the product brief's success, boundaries, empty state, exact decimals,
   multi-currency behavior, isolation, ordering, duplicate dates,
   immutability, provenance, and persistence criteria.
6. Update README and `agent/implementation-log.md` with representative API,
   verification, assumptions, limitations, and recommended follow-up evidence.
7. Preserve the existing modular-monolith architecture and run the canonical
   root command `./verify.sh` before handoff.

### Constraints

- Do not seed Ralph's documented financial data.
- Do not add income/obligation snapshots, cash-flow totals, FX conversion,
  annualization, taxes, forecasting, runway, goals, update/delete behavior,
  UI, AI, or external integrations.
- Preserve Java 21, PostgreSQL, Flyway, JPA, and the existing API/error style.
- Use exact decimal monetary storage compatible with `NUMERIC(19,2)`.
- Keep all records household-scoped; cross-household access must not disclose
  existence.
- Make the historical limitation explicit: `asOfDate` is a filter over
  currently stored source records, not a reconstruction of unavailable value
  history.
- Use branch `task/005-financial-snapshots` and the established PR workflow.

### Definition of Done

- every acceptance criterion in the linked product brief is satisfied
- the full pre-existing and new test suite passes via `./verify.sh`
- the PR reports matching local and GitHub `verify` results
- no application behavior outside this increment is changed
- implementation evidence is recorded for Product Owner review
- Claude pushes the branch and opens the PR; Codex reviews and accepts only
  after the required check is green
