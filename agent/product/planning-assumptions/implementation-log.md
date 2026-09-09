# Implementation Log: Planning Assumptions Registry

Jira issue: WAP-7
Branch: `task/wap-7-planning-assumptions`

## What changed

Added the `com.waypoint.assumption` module (new package, no edits to existing
aggregates or shared infrastructure):

- `PlanningAssumption` entity — household-scoped, immutable except for a
  one-time `supersededBy` link set atomically when a replacement is created.
  Provenance always defaults to `SourceType.MANUAL_ENTRY` internally (not
  accepted from the request body), matching the existing `Asset` convention.
- `PlanningAssumptionRepository` — lookup by id+household, full-history
  listing ordered by name/createdAt/id, and an active-as-of JPQL query
  (`supersededBy IS NULL AND effectiveFrom <= :asOf AND (effectiveUntil IS
  NULL OR effectiveUntil >= :asOf)`).
- `PlanningAssumptionService` — create, get, list (`activeOnly` +
  caller-supplied `asOf`, never the server clock), and supersede. Supersede
  validates the target isn't already superseded, that the replacement's name
  matches the prior version's logical name, and effective-date ordering,
  before creating the replacement and linking the prior version — all inside
  one `@Transactional` method so an invalid/cross-household/already-
  superseded attempt persists nothing.
- REST surface under `/api/households/{householdId}/assumptions`: `POST`
  (create), `GET /{id}` (get), `GET` with `activeOnly`/`asOf` query params
  (list), `POST /{id}/supersede` (supersede). No update or delete mapping
  exists.
- `AssumptionExceptionHandler` — a module-local `@RestControllerAdvice` for
  this package's own exception types (`PlanningAssumptionNotFoundException`,
  `AssumptionAlreadySupersededException`, `InvalidPlanningAssumptionException`),
  kept separate from `com.waypoint.web.ApiExceptionHandler` since that file is
  outside this task's ownership. Spring dispatches to whichever advice
  declares a handler for the thrown exception type, so both coexist without
  edits to the shared file. `HouseholdNotFoundException` is still handled by
  the existing shared advice.
- Migration `V6__create_planning_assumptions.sql` — new `planning_assumptions`
  table with a `superseded_by_id` self-reference and a check constraint
  enforcing `effective_until >= effective_from` at the database level as a
  second line of defense behind the service-level check.

## Tests added

- `PlanningAssumptionServiceTest` (15 tests, mocked repositories): household
  existence checks, field trimming, blank-notes-to-null, effective-window
  validation, active-only listing requiring an explicit `asOf`, and the full
  supersede validation chain (already-superseded, name mismatch, invalid
  dates, successful atomic link).
- `PlanningAssumptionApiIntegrationTest` (25 tests, Testcontainers PostgreSQL
  + MockMvc): create/get/list happy paths, validation 400s (blank/oversized
  name/value/valueType, missing dates, bad effective window), household
  isolation and 404s, deterministic list ordering, active-as-of filtering
  (excludes superseded and out-of-window, includes open-ended, ignores the
  system clock), the full supersede flow (prior stays retrievable with a
  `supersededBy` link, invalid/cross-household/already-superseded attempts
  change nothing), and that `PUT`/`DELETE` on an assumption return 405.

## Commands run

```
cd backend && ./mvnw --batch-mode -q compile
./verify.sh
```

`./verify.sh` result: `Tests run: 467, Failures: 0, Errors: 0` — `BUILD SUCCESS`.

## Assumptions introduced

- The supersede request body carries the full replacement field set
  (including `name`) rather than inheriting the name from the prior version,
  so the "same logical assumption name" acceptance criterion has something
  concrete to validate against and reject on mismatch.
- Deterministic list ordering is `name ASC, createdAt ASC, id ASC` (no
  priority-like field exists on this aggregate, unlike `FinancialGoal`).
- Field bounds (`name` 255, `value` 1000, `valueType` 100, `notes` 2000) are
  chosen to be generous for free-text planning values without an existing
  convention to match; not specified by the product brief.
- "Already superseded" and "replacement name mismatch" are both reported as
  `VALIDATION_FAILED`/400-class errors (via `ASSUMPTION_ALREADY_SUPERSEDED`
  and `VALIDATION_FAILED` codes respectively), consistent with the existing
  codebase's binary 400/404 error vocabulary — there is no 409 Conflict
  precedent elsewhere in the API.

## Limitations / follow-ups

- Per the product brief's explicit scope, `value`/`valueType` remain
  free text; no typed numeric/date assumption schema, approval workflow, or
  link to a future `Plan` version exists yet.
- The supersede endpoint returns only the new replacement; a caller must
  separately `GET` or list to see the prior version's `supersededBy` link.

## Recommended next task

Typed assumption values (numeric/date with unit) and wiring assumptions into
a future `Plan` version, per the product brief's open questions.
