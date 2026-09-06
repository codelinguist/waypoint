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
- [ ] Superseding atomically creates the replacement and links the prior version; the old record remains retrievable, history is ordered, and an invalid/cross-household/already-superseded request changes nothing. Verified in `PlanningAssumptionApiIntegrationTest.supersedeCreatesReplacementAndLinksPriorVersionAtomically`, `.rejectsSupersedingWithMismatchedNameAndChangesNothing`, `.rejectsSupersedingAnAlreadySupersededAssumptionAndChangesNothing`, and `.returnsNotFoundWhenSupersedingAcrossHouseholds` (404, since the prior lookup is household-scoped); service-level equivalents in `PlanningAssumptionServiceTest` additionally assert `planningAssumptionRepository.save` is never called and the prior's `supersededBy` stays null on rejection.
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
- Acceptance evidence: Independent PR #22 diff re-review at `59188166c4ad41549f0c0422830dfb1935e071e4`; see the latest Review findings dated 2026-09-06 below. Feature acceptance withheld.
- Unmet criteria: Criterion 5, atomic supersession with immutable history and no changes from an already-superseded request, is not enforced for overlapping requests (R1).
- Returned work: Resolve R1 and supply a PostgreSQL concurrency regression test plus a passing `./verify.sh` result.
- Follow-up opportunities: Typed assumption values, Plan versions, stale-assumption alerts, approval workflows, and stored scenario integration.
- Accepted or returned by Product Owner Agent: Codex — returned for correction.
- Accepted or returned at: 2026-09-06


## Review findings — 2026-09-06

Review of PR #22 against `main`, obtained independently with `gh pr diff 22`.
Reviewed head: `eb328ce0a97b14d998cbb0d08d8bcfe9f39db7e0`.
Context was limited to the four requested documents and the actual PR diff;
no implementation conversation or other repository context was consulted.
Backend-only: no visual-review file exists and no visual gate applies.

### R1 — Concurrent supersessions can overwrite history

- Classification: `BLOCKING`.
- Decision: `ACCEPTED` — correction required; unresolved.
- Evidence: `backend/src/main/java/com/waypoint/assumption/PlanningAssumptionService.java`,
  `supersedeAssumption` (lines 93–119), reads the prior through the ordinary
  `findByIdAndHousehold_Id`, checks its in-memory link, inserts a replacement,
  and assigns the link. The repository has no locking query, the entity has
  no version field, and the update has no compare-and-set guard. Migration
  `V6__create_planning_assumptions.sql` only makes the replacement ID unique;
  it does not prevent overwriting a prior row's existing link.
- Concrete failure: two overlapping requests read the same unsuperseded prior
  before either commits. Both pass validation and create different replacements.
  Their updates to the prior can both commit, with the later update replacing
  the first link. Both replacements remain unsuperseded and can appear in the
  active list, while the first successful supersession is no longer linked
  from its prior. A transaction around each request does not itself make the
  eligibility check exclusive. This is a code-path finding, not a claim that
  a concurrent runtime reproduction was performed during this review.
- Acceptance condition: enforce one successful supersession per prior across
  concurrent transactions. Add a PostgreSQL regression test with deliberately
  overlapping attempts against the same prior: exactly one succeeds, the loser
  receives a structured rejection and persists no replacement, history contains
  only the original and winning replacement, the original links to that winner,
  and the active query contains only the winner for an applicable date. Preserve
  the existing sequential rejection behavior and rerun `./verify.sh`.
- Basis: acceptance criterion 5 and PD-002 require atomic supersession and
  auditable immutable versions; this remedy enforces the existing contract.

### Acceptance assessment

- Criteria 1–4: supported by the entity/DTO/service/repository diff and synthetic
  integration tests for persistence/provenance, validation, isolation/order,
  and explicit-date active filtering. Bounded value and value-type validation
  are visible in DTO annotations, although their oversized cases are not
  separately exercised in the added tests.
- Criterion 5: unmet under concurrent requests, as detailed in R1. Existing
  sequential tests do not establish concurrent single-supersession behavior.
- Criteria 6–7: supported by the additive controller, immutable field mappings,
  absence of canonical aggregate writes, and the diff's exclusive paths plus
  exactly one V6 migration and the task lifecycle change.
- Criterion 8: the brief and diff report local `./verify.sh` passing with 385
  tests. Independently confirmed that the reviewed head's required GitHub
  `verify` check completed successfully:
  https://github.com/codelinguist/waypoint/actions/runs/33983714424/job/101353365162.
  Synthetic create/query/supersede/history integration flows are present in the
  diff. Tests were not rerun locally in this review.
- Feature acceptance: `PENDING`; returned for R1. Green CI does not resolve the
  uncovered correctness defect. No merge authorization is recorded.
- No additional recommended or optional findings.
- System evolution: a feature-local concurrency regression test is required.
  No shared rule/template/doc change is needed for this fix: the existing
  atomicity and immutable-history requirements already cover the defect.


## Review findings — 2026-09-06 (current-head re-review)

Independently obtained the complete PR #22 diff against `main` using
`gh pr diff 22`. Reviewed head: `59188166c4ad41549f0c0422830dfb1935e071e4`.
Read only the four requested context documents and the actual PR diff;
no other conversation history was used. The existing R1 record is part of
this brief, not external review context. No visual-review file exists;
this is backend-only and visual gates do not apply.

### R1 — Concurrent supersessions can overwrite history (still unresolved)

- Classification: `BLOCKING`.
- Decision: `ACCEPTED` — correction required; still unresolved at this head.
- Visible evidence: `PlanningAssumptionService.supersedeAssumption`, lines
  93–119, still reads through ordinary `findByIdAndHousehold_Id`, checks the
  in-memory supersession link, saves a replacement, and assigns the prior's
  link. `PlanningAssumptionRepository` has no locking query;
  `PlanningAssumption` has no optimistic version field or conditional update.
  V6's unique constraint on `superseded_by_id` prevents sharing a replacement
  between priors, but does not prevent replacing an already-written link.
- Failure: overlapping transactions can both read an unsuperseded prior,
  insert distinct replacements, and commit updates to the same prior. The
  last update wins, losing the earlier link while both replacements remain
  active. The in-memory once-only guard and per-request transaction do not
  serialize this eligibility check. This conclusion follows from the diff;
  a concurrent runtime reproduction was not performed in this review.
- Test evidence: `supersedeCreatesReplacementAndLinksPriorVersionAtomically`
  and `rejectsSupersedingAnAlreadySupersededAssumptionAndChangesNothing` in
  `PlanningAssumptionApiIntegrationTest` exercise sequential requests only.
  The domain and mocked service tests likewise do not overlap transactions.
- Concrete acceptance condition: enforce exactly one successful supersession
  of a prior across deliberately overlapping PostgreSQL transactions. A
  regression test must show one success, a structured rejection for the loser,
  no losing replacement persisted, exactly two history records, the original
  linked to the winner, and only the winner active on an applicable explicit
  date. Retain sequential validation/isolation behavior and pass `./verify.sh`.
- Basis: criterion 5 and PD-002 already require atomic supersession and
  immutable, auditable history. No new product requirement is introduced.

### Acceptance assessment at the reviewed head

- Criteria 1–2: supported by immutable field mappings, manual provenance,
  creation timestamp, bounded/not-blank DTO constraints, required dates,
  date-order validation, and the creation/validation PostgreSQL API tests.
- Criteria 3–4: supported by household-scoped repository lookups and stable
  name/effectiveFrom/createdAt/id ordering, explicit `asOf` validation, and
  temporal-boundary, household-isolation, and active-filter tests.
- Criterion 5: **unmet** for overlapping supersessions, as R1 details.
- Criteria 6–7: supported by the additive create/get/list/supersede surface,
  no update/delete route or canonical aggregate writes, exclusive feature
  paths, and exactly one V6 migration plus the task lifecycle change.
- Criterion 8: the brief and implementation-log diff report local
  `./verify.sh` passing (385 tests, zero failures/errors/skips). Independently
  verified the reviewed head's required GitHub `verify` check completed with
  `SUCCESS`: https://github.com/codelinguist/waypoint/actions/runs/33984650534/job/101355838610.
  Synthetic PostgreSQL create/query/supersede/history tests are present in
  the diff. Tests were not rerun locally during this review.
- Feature acceptance remains `PENDING`; returned to Claude Code for R1.
  Green CI does not resolve the missing concurrency protection. This review
  does not authorize automatic merge.
- No additional `RECOMMENDED` or `OPTIONAL` findings.
- System evolution: the requested PostgreSQL regression test addresses this
  defect class. Existing atomicity and immutable-history rules suffice;
  no shared rule, template, or documentation edit is proposed.
