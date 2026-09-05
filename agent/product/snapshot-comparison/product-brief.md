# Product Brief: Financial Snapshot Comparison

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-05
- Last updated at: 2026-09-05

## User input

- Problem as presented: Task 005 identified snapshot comparison as a candidate next step after delivering persisted financial position snapshots.
- Examples or evidence supplied: The roadmap lists historical comparison in Phase 4; the vision asks what changed financially over time.
- Corrections and constraints supplied: This is a personal household financial planning application. The repository and persisted snapshots remain authoritative; no inferred values become facts.
- Explicit preferences: None required for this increment.

## Product framing

- Underlying problem: A household can record point-in-time financial positions but cannot yet inspect the deterministic change between two recorded dates. Without that comparison, historical snapshots are an archive rather than a planning aid.
- Primary user: Ralph and his wife reviewing household financial progress.
- Desired outcome: A user can request a comparison of two snapshots belonging to the same household and receive clear, deterministic changes in the snapshot’s supported financial measures.
- Success measure: The API returns a correct, auditable comparison for valid snapshot pairs and rejects invalid or cross-household requests without changing stored data.
- Priority and rationale: High. It is the smallest valuable completion of Phase 4, directly activates the Task 005 foundation, and creates useful history before the product adds goals or plan-versus-actual concepts.

## Knowledge classification

### Confirmed inputs

- The roadmap places historical comparison in Phase 4.
- Task 005 delivered financial position snapshots and recorded snapshot comparison as a candidate next step.
- Snapshot comparison is read-only and does not require a household financial preference.

### Product assumptions to validate

- A comparison between two existing snapshots is sufficient for the first increment; trend charts, period-series analysis, and plan-versus-actual semantics can follow later.
- The existing snapshot measures and their stored dates are the comparison source of truth.
- Users need directional deltas (later snapshot minus earlier snapshot) and the two source snapshot identities/dates, not a new persisted comparison record.

### Open questions

- Whether later comparisons should add richer measures, percentage changes, or trend views.
- Whether a future plan-versus-actual feature should reuse this response shape.

## Scope

### In scope

- A read-only household-scoped endpoint/service that compares two persisted financial snapshots.
- Validation that both snapshots exist, belong to the requested household, and represent distinct comparison points.
- Deterministic deltas for every financial measure already exposed by the snapshot model, with the later snapshot treated as the end point and the earlier snapshot as the start point.
- A response that identifies both source snapshots and their dates, and makes the comparison direction explicit.
- Automated unit/domain and API tests covering valid, invalid, missing, and cross-household comparisons.

### Out of scope

- Creating, editing, deleting, or recalculating snapshots.
- Persisting comparison results.
- Goals, target dates, progress, budgets, forecasts, or plan-versus-actual calculations.
- Percentage-change calculations, charts, exports, pagination, or a frontend.
- Authentication, authorization, or external account synchronization.
- Any recommendation or automatic change to canonical household financial data.

## User flow or behavior

1. A trusted private API caller supplies a household and two snapshot identifiers, explicitly identifying the earlier and later snapshot.
2. The system validates the household and both snapshot references.
3. The system calculates each supported measure as later value minus earlier value using deterministic application logic.
4. The system returns the source metadata and signed deltas without writing a comparison or changing either snapshot.
5. Invalid, missing, same-snapshot, and cross-household requests return the repository’s established structured error behavior.

## Acceptance criteria

- [ ] The product exposes a documented read-only comparison operation scoped to one household and two snapshot identifiers.
- [ ] A valid comparison returns both source snapshot dates/identifiers and deterministic later-minus-earlier deltas for every supported snapshot financial measure.
- [ ] The response makes the comparison direction unambiguous, including for decreases and zero changes.
- [ ] The operation rejects a missing household, missing snapshot, snapshot from another household, and identical snapshot identifiers with the established validation/not-found semantics.
- [ ] The operation performs no persistence and does not mutate either source snapshot.
- [ ] Automated tests prove arithmetic correctness, ordering independence only where explicitly represented by the request, error behavior, household isolation, and no mutation.
- [ ] The implementation remains backend-only, keeps calculation logic outside transport concerns, and passes the repository’s canonical verification command.

## Risks and safeguards

- Financial-data or household-approval boundary: This is read-only reporting over already persisted data; it must not infer, revise, or approve financial facts and must not create recommendations.
- Privacy or sensitive-data considerations: Preserve household scoping and do not expose snapshots through another household’s request; avoid adding unnecessary data to the response.
- Accessibility considerations: No UI is in scope. Any future presentation must preserve signed changes and source dates in text.
- Failure or misuse risks: Ambiguous date ordering or cross-household identifiers could produce misleading results; require explicit earlier/later inputs and validate ownership before calculation.

## Product decisions

### PD-001 — Explicit earlier and later snapshot inputs

- Decision: The comparison request names the earlier and later snapshots explicitly; the service does not infer ordering from dates or silently swap them.
- Evidence: The core user question is what changed, and a signed delta is only meaningful with a declared direction.
- Alternatives considered: Automatically sort by snapshot date; accept an unordered pair.
- Rationale: Explicit direction prevents surprising results, handles equal or irregular dates deliberately, and keeps the API auditable.
- User input required: `NO` — this is a reversible API convention grounded in comparison semantics.

### PD-002 — No persisted comparison aggregate

- Decision: Return the comparison on demand and persist neither the request nor its result.
- Evidence: The roadmap calls for historical comparison, while the product principles favor simple, auditable designs and canonical snapshots as the source of truth.
- Alternatives considered: Store comparison records; introduce a history/event model.
- Rationale: On-demand deterministic calculation is the smallest complete increment and avoids stale derived records.
- User input required: `NO` — no household preference or material financial decision is involved.

## Delivery handoff

- Current task: `agent/tasks/006-snapshot-comparison.md`
- Design brief, if applicable: Not applicable; backend-only task.
- Implementation owner: Claude Code
- Review evidence: Pending implementation.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence:
- Unmet criteria:
- Returned work:
- Follow-up opportunities: Goals domain and plan-versus-actual analysis remain candidates after this Phase 4 completion.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:
