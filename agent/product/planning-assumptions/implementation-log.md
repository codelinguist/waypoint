# Implementation Log — Planning Assumptions Registry

Feature-local log for Task 012, per this task's exclusive prose ownership
(`agent/product/planning-assumptions/**`). The shared
`agent/implementation-log.md` is intentionally not touched here; consolidation
follows the parallel batch per the product brief's delivery handoff.

### 2026-09-06 — Add the planning assumption aggregate, API, and tests

**Changed**

- New `com.waypoint.assumption` package: `PlanningAssumption` entity (immutable
  except for the one-time `supersededBy` link set during supersession),
  `PlanningAssumptionRepository`, `PlanningAssumptionService`, and the
  `PlanningAssumptionNotFoundException` / `InvalidPlanningAssumptionException`
  domain exceptions.
- `web/PlanningAssumptionController` exposes
  `/api/households/{householdId}/assumptions`: `POST` (create), `GET /{id}`
  (retrieve), `GET` (list, with `activeOnly`/`asOf` query params), and
  `POST /{id}/supersede` (atomic supersession).
- `web/PlanningAssumptionExceptionHandler` is a `@RestControllerAdvice`
  scoped to `PlanningAssumptionController` only (`assignableTypes`), mirroring
  the existing `GoalContributionExceptionHandler` pattern, so the shared
  `com.waypoint.web.ApiExceptionHandler` is reused read-only and never edited
  — required by this task's exclusive-ownership constraint.
- `web/dto/PlanningAssumptionRequest` and `PlanningAssumptionResponse`.
- `db/migration/V6__create_planning_assumptions.sql`: one new table with a
  self-referencing `superseded_by_id` FK, a `UNIQUE` constraint on it (a given
  version can be pointed to by at most one prior version), and a check
  constraint enforcing `effective_until >= effective_from`.

**Tests**

- `PlanningAssumptionTest` (domain unit): `isActiveAsOf` boundary behavior
  (before/on/after `effectiveFrom`/`effectiveUntil`, open-ended window,
  superseded-always-inactive) and `linkSupersededBy` immutability guard.
- `PlanningAssumptionServiceTest` (Mockito unit): household-not-found on
  create/get/list/supersede, field trimming, blank-notes-to-null
  normalization, date-ordering validation, active-list filtering, and the
  three supersession rejection paths (already-superseded, name mismatch,
  invalid date ordering) each verified to leave the repository `save` method
  uncalled and the prior version's `supersededBy` unset.
- `PlanningAssumptionApiIntegrationTest` (`@SpringBootTest` +
  `Testcontainers`, mirroring `FinancialGoalApiIntegrationTest`): create/get,
  bean-validation 400s (blank/oversized fields, missing dates, bad date
  ordering, persists nothing), household-not-found and cross-household 404s,
  deterministic list ordering, household isolation, active-as-of filtering
  (excludes not-yet-effective, out-of-window, and superseded versions;
  includes open-ended versions), the full supersession flow (old record
  remains retrievable and marked `superseded`, history shows both versions),
  the three supersession rejection paths returning 400/404 and changing
  nothing, and a check that creating an assumption does not mutate an
  unrelated asset in the same household.
- Local `./verify.sh` (root, full Maven suite incl. Testcontainers): **PASS**
  — `Tests run: 385, Failures: 0, Errors: 0, Skipped: 0` (42 of those new to
  this task: 8 domain, 12 service, 22 integration).
- Manually exercised the primary flow through the same MockMvc-driven
  integration test rather than a separately run local server — this is a
  backend-only, unattended-worker task with no interactive environment to run
  a live server against; the integration test hits the real REST boundary,
  Flyway migration, and PostgreSQL (via Testcontainers), so it is the
  equivalent evidence for this task's shape.

**Decisions**

- Reused the household-scoped `SourceType` enum (`MANUAL_ENTRY` only)
  read-only from `com.waypoint.household`, per PD-001 — no new source-type
  concept introduced, consistent with "manual entry is the only supported
  provenance source today."
- Implemented "active as of" as a domain method on the entity
  (`isActiveAsOf(LocalDate)`) rather than a database query predicate, then
  filtered in the service after a single deterministic-order fetch. This
  keeps the temporal-applicability rule unit-testable without a database,
  consistent with `AGENTS.md`'s "financial calculations must be callable
  without an LLM" / deterministic-and-testable principle, and household
  assumption lists are small enough that in-memory filtering has no
  meaningful cost.
- The `PlanningAssumptionRequest` DTO is shared by both the create and
  supersede endpoints since their required fields are identical; introducing
  a second, structurally identical DTO type for supersession would be a
  premature distinction with no behavioral difference.
- Supersession validates that the replacement's `name` equals the superseded
  version's `name` (the "logical assumption name" from the product brief) and
  that the target is not already superseded, both before creating any new
  row, so a rejected supersession changes nothing. Cross-household
  supersession attempts are naturally rejected as 404 (not 400) because the
  prior version is looked up scoped by `householdId`, matching the existing
  `findByIdAndHousehold_Id` isolation pattern used elsewhere in the codebase.

**Assumptions**

- Blank (all-whitespace) `notes` are normalized to `null` on create and
  supersede, mirroring how other aggregates trim free-text input; this was
  not specified explicitly in the product brief's confirmed inputs.
- `GET .../assumptions?activeOnly=true` requires an explicit `asOf` query
  parameter and returns `400 VALIDATION_FAILED` if it is missing, rather than
  defaulting to the server's current date — required by the acceptance
  criterion that active filtering "does not consult the system date," but the
  exact 400-vs-405-vs-omitted-filter behavior for a missing `asOf` was left to
  this implementation to choose.
- `GET .../assumptions` without `activeOnly` returns full version history
  (including superseded versions) in the same deterministic
  name/effectiveFrom/createdAt/id order used for the active-only list; an
  `asOf` value supplied without `activeOnly=true` is silently ignored rather
  than rejected.
- Response includes both a `superseded` boolean and a nullable
  `supersededById`, to keep supersession state an explicit text/boolean field
  per the brief's accessibility note ("supersession must be explicit"),
  rather than requiring the caller to infer it from a nullable field alone.

**Open questions**

- None beyond what the product brief already lists under "Open questions"
  (typed numeric/date assumption values, approval workflows, imports, and
  links from assumptions to future Plan versions).

**Recommended next task**

- A follow-up to consume `PlanningAssumption` values from a typed contract
  (e.g. a numeric-rate assumption feeding a deterministic calculator) once a
  concrete calculator needs one — this task deliberately leaves the `value`
  field as free text plus a `valueType` label, per PD-001, rather than
  guessing a numeric schema.
