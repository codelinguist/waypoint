# Product Brief: Planning Assumptions Registry

## Status

`READY`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-06
- Last updated at: 2026-09-06

## User input

- Problem as presented: Frame another batch of three tasks for parallel execution.
- Examples or evidence supplied: The roadmap's Phase 6 requires planning assumptions, effective dates, review dates, provenance, and supersession; this foundation is still absent while part of Phase 7 is already complete.
- Corrections and constraints supplied: Use the established orchestrator and isolated worktree workflow, with up to three tasks in parallel.
- Explicit preferences: Frame three implementation-ready tasks rather than continuing one task at a time.

## Product framing

- Underlying problem: Waypoint can store domain records and run temporary calculations, but it cannot durably record what the household currently assumes for planning, why, when it applies, or what replaced it.
- Primary user: Ralph and his wife through the trusted private API.
- Desired outcome: Record household-scoped planning assumptions as explicitly non-factual, provenance-bearing, immutable versions with review and supersession history.
- Success measure: A caller can create, retrieve, list, and supersede an assumption without overwriting history or changing any existing canonical financial record.
- Priority and rationale: This is the missing Phase 6 prerequisite for trustworthy stored plans and scenarios. A generic Fact table is deliberately excluded because existing assets, liabilities, income, obligations, snapshots, and goals already represent typed canonical facts.

## Knowledge classification

### Confirmed inputs

- Product principles require facts and assumptions to remain distinct and prohibit silently promoting inferred values to facts.
- Material values require timestamps and provenance.
- Existing typed household aggregates remain canonical for their respective facts.

### Product assumptions to validate

- A general-purpose assumption value can initially be represented as a required textual value plus a required unit/type label, rather than guessing one universal numeric schema.
- Manual entry is the only supported provenance source today, consistent with the current `SourceType` capability.

### Open questions

- Typed numeric/date assumption values, approval workflows, imports, and links from assumptions to future Plan versions remain follow-ups.

## Scope

### In scope

- Create a household-scoped `PlanningAssumption` with stable ID, required name, required textual value, required value type/unit label, optional notes, `effectiveFrom`, optional `effectiveUntil`, required `reviewDate`, provenance source, created timestamp, and optional `supersededBy` link.
- Treat records as immutable history. Superseding creates a new assumption and links the prior version to it atomically; ordinary update and delete operations do not exist.
- Validate nonblank bounded text, date ordering, and that a replacement belongs to the same household and logical assumption name.
- Retrieve one assumption and list a household's assumptions deterministically, with an explicit query option for active-only versus full history.
- Define active as not superseded and temporally applicable on a caller-supplied `asOf` date; do not depend on the server clock implicitly.
- Expose additive private REST endpoints and structured validation/not-found behavior, backed by one Flyway migration.
- Add unit and PostgreSQL integration tests for creation, household isolation, temporal filtering, supersession, immutability, provenance, and rollback on invalid replacement.

### Out of scope

- A generic Fact entity/table; changing existing asset, liability, income, obligation, snapshot, or goal schemas.
- Automatically deriving assumptions, converting them to facts, approval/decision workflows, Plan persistence, scenarios, recommendations, UI, imports, or LLM integration.
- Currency conversion or interpreting arbitrary textual values in financial calculations.

## User flow or behavior

1. A trusted caller explicitly creates an assumption and labels its value, timing, review date, notes, and provenance.
2. Waypoint returns it as an assumption, never as a confirmed fact.
3. The caller can query the active assumption as of an explicit date or inspect full version history.
4. When the belief changes, the caller supersedes it with a new record; the prior version remains auditable.

## Acceptance criteria

- [x] Creating a valid assumption persists every supplied field, `MANUAL_ENTRY` provenance, timestamps, household ownership, and no supersession link. Verified in `PlanningAssumptionApiIntegrationTest.createsAndRetrievesAssumption` (asserts name/value/valueType/notes/effectiveFrom/effectiveUntil/reviewDate/sourceType/createdAt/superseded=false/supersededById absent) and `PlanningAssumptionServiceTest.createsAssumptionWithTrimmedFieldsAndBlankNotesNormalizedToNull`.
- [x] Blank/oversized name, value, or value-type labels; missing dates; and `effectiveUntil < effectiveFrom` return structured HTTP 400 and persist nothing. Verified in `PlanningAssumptionApiIntegrationTest.rejectsBlankName`, `.rejectsBlankValue`, `.rejectsBlankValueType`, `.rejectsOversizedName`, `.rejectsMissingEffectiveFrom`, `.rejectsMissingReviewDate`, and `.rejectsEffectiveUntilBeforeEffectiveFromAndPersistsNothing` (asserts the list stays empty afterward). Domain-level date-ordering rejection additionally verified in `PlanningAssumptionServiceTest.rejectsEffectiveUntilBeforeEffectiveFrom`.
- [x] Get/list operations enforce household isolation and established 404 behavior; list order is deterministic. Verified in `PlanningAssumptionApiIntegrationTest.returnsNotFoundWhenGettingAssumptionForUnknownHousehold`, `.returnsNotFoundWhenAssumptionBelongsToAnotherHousehold`, `.keepsAssumptionsIsolatedBetweenHouseholds`, and `.listsAssumptionsOrderedDeterministicallyByName` (name/effectiveFrom/createdAt/id ascending, matching the existing household-scoped repository pattern).
- [x] Active-as-of filtering uses the explicit date, excludes superseded and out-of-window versions, includes open-ended versions, and does not consult the system date. `PlanningAssumption.isActiveAsOf(LocalDate)` takes the date as an explicit parameter and never reads the system clock; verified in `PlanningAssumptionTest` (boundary cases: before/on effectiveFrom, on/after effectiveUntil, open-ended, superseded-always-inactive), `PlanningAssumptionServiceTest.listActiveAssumptionsFiltersOutSupersededAndOutOfWindowVersions`, and `PlanningAssumptionApiIntegrationTest.activeOnlyFilterExcludesOutOfWindowAndIncludesOpenEndedVersions` / `.activeOnlyFilterExcludesSupersededVersions`. `GET .../assumptions?activeOnly=true` without `asOf` returns 400 (`.activeOnlyRequiresAsOfParameter`) rather than defaulting to today.
- [x] Superseding atomically creates the replacement and links the prior version; the old record remains retrievable, history is ordered, and an invalid/cross-household/already-superseded request changes nothing. Verified in `PlanningAssumptionApiIntegrationTest.supersedeCreatesReplacementAndLinksPriorVersionAtomically`, `.rejectsSupersedingWithMismatchedNameAndChangesNothing`, `.rejectsSupersedingAnAlreadySupersededAssumptionAndChangesNothing`, and `.returnsNotFoundWhenSupersedingAcrossHouseholds` (404, since the prior lookup is household-scoped); service-level equivalents in `PlanningAssumptionServiceTest` additionally assert `planningAssumptionRepository.save` is never called and the prior's `supersededBy` stays null on rejection.
- [x] There is no ordinary update/delete API and no path that changes existing canonical financial aggregates or labels an assumption as fact. `PlanningAssumptionController` exposes only `POST`, `GET`, and `POST .../supersede`; `PlanningAssumption` has no setters besides the package-private, once-only `linkSupersededBy`; no existing aggregate, repository, or service is referenced for writes. Confirmed by inspection and by `.creatingAssumptionDoesNotMutateOtherHouseholdRecords`.
- [x] The implementation adds exactly one new migration and remains within Task 012's exclusive code/test/product paths except for that migration and its own task lifecycle file. Confirmed via `git status`/`git diff --stat` against the pre-task baseline: only `backend/src/main/java/com/waypoint/assumption/**`, `backend/src/test/java/com/waypoint/assumption/**`, `backend/src/main/resources/db/migration/V6__create_planning_assumptions.sql`, and this feature's `agent/product/planning-assumptions/**` files changed; `ApiExceptionHandler` and sibling packages untouched (a scoped `PlanningAssumptionExceptionHandler` with `@RestControllerAdvice(assignableTypes = ...)` is used instead, mirroring `GoalContributionExceptionHandler`).
- [x] `./verify.sh` passes; synthetic API flows demonstrate create, active query, supersede, and history without using real household data. `./verify.sh` passed locally: `Tests run: 385, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS` (42 tests new to this task). All API flows exercised via `PlanningAssumptionApiIntegrationTest` against synthetic households created within each test; no real household data used. GitHub `verify` check pending on the opened PR.

## Risks and safeguards

- Financial-data or household-approval boundary: Creating or superseding an assumption records a planning belief, not a confirmed fact, recommendation, decision, or authorization to change canonical financial data.
- Privacy or sensitive-data considerations: Private API only; examples and evidence use synthetic values and avoid request-body logging.
- Accessibility considerations: Backend-only; type, provenance, active state, and supersession must be explicit text fields.
- Failure or misuse risks: A generic textual value must not be consumed by deterministic calculators without a separately defined typed contract.

## Product decisions

### PD-001 — Add assumptions without a generic Fact registry

- Decision: Persist `PlanningAssumption` as its own aggregate; continue treating existing typed domain records as canonical facts.
- Evidence: The domain model distinguishes concepts, while the application already has typed fact-bearing aggregates. A generic Fact table would duplicate and weaken those schemas.
- Alternatives considered: One generic Fact/Assumption table; modifying every existing aggregate in this task.
- Rationale: Closes the Phase 6 assumption gap without creating a competing financial datastore.
- User input required: `NO` — this is reversible domain scoping grounded in existing principles.

### PD-002 — Preserve versions through explicit supersession

- Decision: No in-place edits; supersession creates a linked replacement transactionally.
- Evidence: The roadmap and principles require historical planning state and supersession.
- Alternatives considered: Mutable records with an audit timestamp; event sourcing.
- Rationale: Provides an auditable history without premature infrastructure.
- User input required: `NO`.

### PD-003 — Isolate the parallel batch

- Decision: Task 012 exclusively owns `com.waypoint.assumption/**`, matching tests, `agent/product/planning-assumptions/**`, migration `V6__create_planning_assumptions.sql`, and its lifecycle file. It may reuse household types read-only but must not edit shared handlers, existing aggregates, build files, README, roadmap, central implementation log, or workflow documents.
- Evidence: Earlier parallel batches exposed shared-file and migration conflicts.
- Alternatives considered: Shared household-package edits and concurrent central documentation updates.
- Rationale: Only this task has a migration; siblings are stateless and own separate packages.
- User input required: `NO`.

## Delivery handoff

- Current task: `agent/tasks/012-planning-assumptions.md`
- Design brief, if applicable: Not applicable; backend-only.
- Implementation owner: Claude Code in isolated branch/worktree `task/012-planning-assumptions`.
- Review evidence: Pending PR review. Record implementation details in `agent/product/planning-assumptions/implementation-log.md`; shared-document consolidation follows the batch.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence:
- Unmet criteria: Pending implementation.
- Returned work:
- Follow-up opportunities: Typed assumption values, Plan versions, stale-assumption alerts, approval workflows, and stored scenario integration.
- Accepted or returned by Product Owner Agent:
- Accepted or returned at:

