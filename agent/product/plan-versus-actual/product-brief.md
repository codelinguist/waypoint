# Product Brief: Plan-versus-Actual Snapshot Analysis

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-05
- Last updated at: 2026-09-05

## User input

- Problem as presented: Task 006 identified plan-versus-actual analysis as a follow-up opportunity; the vision asks whether the household is ahead or behind the plan it made.
- Examples or evidence supplied: The product vision asks what changed and whether current choices delay long-term plans; the roadmap includes plan-versus-actual analysis in conversational planning.
- Corrections and constraints supplied: Financial arithmetic must be deterministic, and the LLM is not the authoritative datastore.
- Explicit preferences: None supplied beyond the repository principles and roadmap.

## Product framing

- Underlying problem: Snapshot comparison explains change between two actual states, but it does not show how one actual snapshot differs from a household's stated expected values.
- Primary user: Ralph and his wife conducting a focused financial review.
- Desired outcome: A caller can submit explicit planned monetary measures and compare them with one persisted snapshot, receiving signed actual-minus-plan variances with clear direction.
- Success measure: A read-only analysis makes ahead/behind interpretation possible without creating a plan record, changing snapshots, or depending on Goals.
- Priority and rationale: Second-highest next increment. It directly extends the accepted snapshot read model, is useful immediately for manual review, and is smaller and safer than introducing the later planning engine.

## Knowledge classification

### Confirmed inputs

- The roadmap calls for plan-versus-actual analysis and the Task 006 brief leaves its semantics open.
- Financial snapshots already expose deterministic per-currency asset, liability, and net-worth measures.
- Product principles require explicit separation of facts, assumptions, goals, recommendations, and decisions, and prohibit silent mutation.

### Product assumptions to validate

- For this first increment, a plan is an explicit analysis input rather than a persisted planning entity.
- The analysis compares one selected snapshot's actual totals with caller-supplied planned totals for the same currencies and measures.
- Variance is `actual - planned`; the response states the direction explicitly and does not label every positive variance as good or bad.
- Missing planned measures are invalid rather than silently treated as zero; zero is valid when explicitly supplied.

### Open questions

- Whether future plans should be persisted with effective dates, provenance, versioning, or approval state.
- Which measures should be planned beyond the existing snapshot asset, liability, and net-worth totals.
- Whether the household wants a positive/negative variance interpreted as favorable for any particular measure.

## Scope

### In scope

- A documented read-only backend analysis scoped to one household and one existing snapshot.
- A request containing explicit per-currency planned asset, liability, and net-worth amounts for the measures already supported by snapshots.
- Deterministic decimal actual-minus-planned variances, source snapshot metadata, and explicit plan/actual/variance values.
- Validation for household and snapshot ownership, duplicate currencies, complete measures, non-negative planned totals, and internally consistent planned net worth (`assets - liabilities`).
- Unit and API/integration tests for arithmetic, direction, validation, household isolation, and no persistence or mutation.

### Out of scope

- Persisting plans, assumptions, budgets, targets, or analysis results.
- Creating or editing goals; target dates, progress, contributions, forecasts, and scenario modeling.
- Inferring planned values from conversation, historical snapshots, goals, or household facts.
- Judging whether a variance is financially good or bad, making recommendations, or changing canonical data.
- Percentage variance, charts, exports, frontend, authentication, imports, and external synchronization.

## User flow or behavior

1. A trusted private API caller supplies a household, snapshot identifier, and explicit planned totals.
2. The system validates household ownership, snapshot ownership, currency uniqueness, and planned-total consistency.
3. The system reads the snapshot's actual totals and computes actual-minus-plan per currency and measure.
4. The system returns the source snapshot metadata and signed variances without persisting the plan or changing the snapshot.

## Acceptance criteria

- [ ] The backend exposes a documented read-only household-scoped operation comparing explicit plan inputs with one existing financial snapshot.
- [ ] A valid response returns the source snapshot identifier/date and, for every supplied currency, planned values, actual values, signed actual-minus-plan variances, and unambiguous direction.
- [ ] Arithmetic uses deterministic decimal domain logic and covers currencies with zero actual values where explicitly planned.
- [ ] The operation rejects unknown households, missing or cross-household snapshots, duplicate/missing measures, negative planned totals, and inconsistent planned net worth with established structured errors.
- [ ] The operation performs no persistence and does not mutate the source snapshot; repeated identical requests produce the same result.
- [ ] Automated unit and API/integration tests cover arithmetic, sign/direction, validation, household isolation, and no mutation.
- [ ] The implementation remains backend-only, adds no migration or persistent planning entity, does not depend on Goals, and passes `./verify.sh`.

## Risks and safeguards

- Financial-data or household-approval boundary: Plan values are caller-supplied analysis inputs and are never silently promoted to facts, assumptions, goals, or decisions.
- Privacy or sensitive-data considerations: Enforce household ownership before reading the snapshot and return only the requested analysis.
- Accessibility considerations: No UI is in scope; future presentation must show plan, actual, signed variance, and direction as text.
- Failure or misuse risks: Variance sign alone can be misleading; return all three values and avoid normative labels such as good, bad, ahead, or behind unless a future metric-specific rule is approved.

## Product decisions

### PD-001 — Analyze explicit plans without persistence

- Decision: Accept the plan in the read-only request and persist neither the plan nor the result.
- Evidence: The roadmap's durable planning engine and facts/assumptions work are later phases; Task 006 established a useful read-only snapshot surface.
- Alternatives considered: Add a persisted Plan aggregate; infer a plan from goals; compare two snapshots as a proxy for plan-versus-actual.
- Rationale: Delivers the smallest useful analysis while avoiding premature plan semantics and keeps this task independent from Goals.
- User input required: `NO` — callers explicitly provide the values; no undiscoverable household preference is needed.

### PD-002 — Use actual-minus-plan and neutral language

- Decision: Define variance as actual minus plan and return signed values with explicit direction, without deciding whether a variance is favorable.
- Evidence: Snapshot comparison already uses signed later-minus-earlier deltas, and favorability depends on the metric and household priorities.
- Alternatives considered: Plan-minus-actual; generic ahead/behind labels; metric-specific favorable rules.
- Rationale: Consistent arithmetic is auditable; neutral output avoids guessing household preferences.
- User input required: `NO` — direction is a mathematical convention, not a material financial preference.

## Delivery handoff

- Current task: `agent/tasks/008-plan-versus-actual.md`
- Design brief, if applicable: Not applicable; backend-only task.
- Implementation owner: Claude Code
- Review evidence: Implemented in new
  `PlannedCurrencyTotals`/`VarianceDirection`/`PlanVersusActualVariance`/
  `CurrencyPlanVersusActual`/`PlanVersusActualAnalysis`/`InvalidPlanException`/
  `PlanVersusActualService` (domain), `PlanVersusActualController`
  (`POST /api/households/{householdId}/financial-snapshots/{snapshotId}/plan-comparison`),
  and matching request/response DTOs — additive files, reusing
  `FinancialSnapshotService` for household/snapshot ownership and actual
  totals rather than editing Task 006's snapshot service/controller. See
  `agent/implementation-log.md` (2026-09-05, Task 008) for full detail.
  `./verify.sh`: 175 tests, 0 failures (21 new). Primary flow exercised
  manually end to end (plan above/below/on actuals, absent-currency
  zero-actual handling, inconsistent-net-worth rejection, no snapshot
  mutation) against a throwaway Postgres instance, not the shared
  `waypoint-postgres-data` Compose volume — see the implementation log's
  "Open questions" for a recurring risk this surfaced with that volume.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: PR 10 was reviewed against this brief and
  `agent/collaboration-workflow.md`. The review found no unresolved findings.
  `./verify.sh` passes locally with 175 tests and 0 failures; the required
  GitHub `verify` check is green. The implementation provides the documented
  read-only household/snapshot endpoint, deterministic decimal
  actual-minus-plan values and directions, structured validation and
  household isolation, no persistence or snapshot mutation, focused unit and
  integration coverage, and no migration, planning entity, or Goals
  dependency.
- Unmet criteria: None.
- Returned work: None.
- Follow-up opportunities: Persisted/versioned plans, facts and assumptions, plan-vs-actual over cash flow, goal-aware interpretation, and favorable-variance rules only after household preferences are supplied.
- Accepted or returned by Product Owner Agent: Accepted by Codex.
- Accepted or returned at: 2026-09-05.

## Review findings

### 2026-09-05 — PR 10 review

- Review scope: `gh pr diff 10`, the approved acceptance criteria above, and
  the review criteria in `agent/collaboration-workflow.md`.
- Findings: None. No `BLOCKING`, `RECOMMENDED`, or `OPTIONAL` finding had
  visible evidence and a concrete acceptance condition sufficient to warrant
  recording a defect.
- Finding disposition: Not applicable; there were no findings to mark
  `ACCEPTED`, `REJECTED`, or `DEFERRED`.
- Verification: Local `./verify.sh` completed successfully with 175 tests and
  0 failures. GitHub required `verify` check passed (run
  `33963009680`, job `101298082488`).
