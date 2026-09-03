# Current Task

## Task 004 — Income and Recurring Obligations

### Goal

Add canonical household records for recurring income streams and recurring
obligations so later snapshots and deterministic planning can work from dated,
frequency-aware cash-flow commitments.

Detailed requirements and acceptance criteria:

- `agent/product/income-obligations/product-brief.md`

### Outcome

A trusted caller can create and retrieve household-scoped income streams and
recurring obligations with explicit amounts, frequencies, currencies, dates,
and uncertainty/classification. The API preserves schedule semantics without
calculating totals or silently converting estimates into facts.

### Required deliverables

1. Add Flyway migrations and explicit domain entities for income streams and
   recurring obligations, linked to households.
2. Add validated create/get/list APIs with structured errors, exact decimal
   handling, household isolation, deterministic ordering, and server-assigned
   `MANUAL_ENTRY` provenance.
3. Represent income frequency, compensation classification, and certainty
   explicitly; allow valid future starts and reject invalid date ranges.
4. Add focused unit and PostgreSQL/Testcontainers integration tests covering
   the product brief's success, validation, provenance, isolation, ordering,
   and persistence criteria.
5. Update README and `agent/implementation-log.md` with representative API,
   verification, assumptions, limitations, and recommended follow-up evidence.
6. Preserve the existing modular-monolith architecture and run the canonical
   root command `./verify.sh` before handoff.

### Constraints

- Do not seed Ralph's documented income or obligations.
- Do not add aggregation, FX conversion, taxes, annualization, forecasting,
  runway, goals, snapshots, update/delete/history behavior, UI, AI, or
  external integrations.
- Preserve Java 21, PostgreSQL, Flyway, JPA, and the existing API/error style.
- Use exact decimal monetary storage and validation compatible with
  `NUMERIC(19,2)`.
- Keep all records household-scoped; cross-household access must not disclose
  existence.
- Preserve explicit uncertainty: `EXPECTED` or `VARIABLE` income is not a
  confirmed fact merely because it was submitted through the API.
- Use branch `task/004-income-obligations` and the established PR workflow.

### Definition of Done

- every acceptance criterion in the linked product brief is satisfied
- the full pre-existing and new test suite passes via `./verify.sh`
- the PR reports matching local and GitHub `verify` results
- no application behavior outside this increment is changed
- implementation evidence is recorded for Product Owner review
- Claude pushes the branch and opens the PR; Codex reviews and accepts only
  after the required check is green
