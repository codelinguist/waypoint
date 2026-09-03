# Implementation Log

### 2026-09-03 — Task 004: Income and Recurring Obligations

**Changed**

- `task/004-income-obligations` was cut from `main` before Task 003 merged,
  so it was missing `verify.sh`, the required `verify` CI check, and the
  Task 003 documentation updates. Merged `origin/main` into the task branch
  before implementing; the only conflict was `agent/current-task.md` (stale
  Task 003 content vs. the already-defined Task 004 task), resolved in favor
  of the Task 004 content since Task 003 was already accepted and merged.
- Added `IncomeStream` and `Obligation` JPA entities
  (`backend/src/main/java/com/waypoint/household/`), both household-scoped
  via a `@ManyToOne` to `Household`, with server-assigned `id`,
  `sourceType = MANUAL_ENTRY`, and Hibernate-managed `createdAt`/`updatedAt`.
  `IncomeStream` carries `incomeType`, `amount` (rate or flat amount
  depending on `frequency`), `frequency`, `currency`,
  `compensationClassification` (`GROSS`/`NET`/`UNKNOWN`), `certainty`
  (`CONFIRMED`/`EXPECTED`/`VARIABLE`), `startDate`, and optional `endDate`.
  `Obligation` carries `obligationType`, `amount`, `frequency`, `currency`,
  `startDate`, and optional `endDate`. Added the supporting enums
  (`Frequency`, `IncomeType`, `IncomeCertainty`,
  `CompensationClassification`, `ObligationType`) and a shared
  `InvalidScheduleException` for the `endDate < startDate` rule.
- Added `IncomeStreamRepository`/`ObligationRepository`
  (household-scoped `findBy...OrderByCreatedAtAscIdAsc` and
  `findByIdAndHousehold_Id` queries, matching the `Asset`/`Liability`
  pattern), `IncomeStreamService`/`ObligationService` (household lookup,
  name/currency normalization, `endDate`-not-before-`startDate` validation,
  not-found semantics), and `IncomeStreamNotFoundException`/
  `ObligationNotFoundException`.
- Added `IncomeStreamController`/`ObligationController` under
  `/api/households/{householdId}/income-streams` and
  `/api/households/{householdId}/obligations` (`POST`, `GET /{id}`, `GET`
  list), plus `CreateIncomeStreamRequest`/`IncomeStreamResponse` and
  `CreateObligationRequest`/`ObligationResponse` DTOs with Bean Validation
  (`@NotBlank`/`@NotNull`/`@DecimalMin(0)`/`@Digits(17,2)`/3-letter currency
  `@Pattern`). Unlike `valuedAt`/`balanceAsOf` on `Asset`/`Liability`,
  `startDate` has no `@PastOrPresent` constraint, so future starts are
  accepted per the product brief (the household has jobs starting in
  October 2026).
- Wired `IncomeStreamNotFoundException`, `ObligationNotFoundException`, and
  `InvalidScheduleException` into `ApiExceptionHandler`
  (`INCOME_STREAM_NOT_FOUND` / `OBLIGATION_NOT_FOUND` 404,
  `VALIDATION_FAILED` 400).
- Added Flyway migration `V3__create_income_streams_and_obligations.sql`
  creating `income_streams` and `obligations` tables (both with a
  household-id index and a `CHECK (amount >= 0)` constraint, matching the
  `assets`/`liabilities` non-negativity-constraint pattern; `endDate`
  ordering is enforced in the service layer only, consistent with how
  `Asset.planningValue <= estimatedValue` is enforced today).
- Updated `README.md`: documented the new endpoints, enum values, and
  semantics (future-dated starts allowed; `certainty` always returned as
  submitted, never inferred; `amount` is schedule-relative, not annualized),
  and updated the Status section (Phase 1/2 and Task 003 are accepted; Phase
  3 is implemented and pending Product Owner acceptance).

**Tests**

- `IncomeStreamServiceTest` / `ObligationServiceTest` (Mockito unit tests):
  name/currency normalization, household-not-found on create/get/list,
  `endDate`-before-`startDate` rejection, household-scoped retrieval and
  isolation, and creation-order listing.
- `IncomeObligationApiIntegrationTest` (`@SpringBootTest` +
  `@AutoConfigureMockMvc` + Testcontainers `PostgreSQLContainer`, real
  Flyway migration run): create/retrieve for both resources; future
  `startDate` accepted; zero amount accepted and negative rejected; blank
  name, malformed currency, and unrecognized `incomeType`/`obligationType`/
  `frequency`/`certainty`/`compensationClassification` rejected; `endDate`
  before `startDate` rejected and equal-to-`startDate` accepted; exact
  `NUMERIC(19,2)` decimal preservation; excessive fractional scale and
  precision-overflow rejection; an unsupported client-submitted `sourceType`
  field rejected; unknown-household 404 on create/list; cross-household
  retrieval returns 404 without disclosing the record; new households return
  empty lists; creation-order listing with permitted duplicate names; and
  cross-household isolation. 37 tests, all passing.
- Full suite: `./verify.sh` — 113 tests (32 asset/liability +
  37 income/obligation + 44 household/person/misc.), 0 failures, Flyway
  migrating a clean database through V1 -> V2 -> V3 automatically.
- Exercised the primary flow manually against `docker compose up --build`
  (Docker Desktop started for this session): created a household, an
  `EXPECTED`/`GROSS` monthly salary income stream with an October 2026
  start date, a `VARIABLE`/`UNKNOWN` hourly freelance income stream, and a
  monthly mortgage obligation; listed both collections back in creation
  order; confirmed a negative-amount obligation and an `endDate` before
  `startDate` both return `VALIDATION_FAILED` 400; confirmed an unknown
  household returns 404; confirmed the created records survived a
  `docker compose down` / `up` cycle (named-volume persistence), then tore
  the stack down.

**Decisions**

- Kept `IncomeStream`/`Obligation` in the existing `com.waypoint.household`
  package/module rather than introducing a separate `cash_flow` module,
  matching how `Asset`/`Liability` were placed in Task 002 and preserving
  the current single-module boundary until a concrete feature needs the
  split described in `docs/architecture/architecture.md`.
- Used one shared `Frequency` enum (`HOURLY`, `WEEKLY`, `BIWEEKLY`,
  `MONTHLY`, `ANNUAL`) for both `IncomeStream` and `Obligation`, since the
  product brief defines frequency once and both resources need the same
  vocabulary.
- Named the income amount/rate field `amount` (not `rate`) on both
  entities: per PD-001 it is stored and returned exactly as submitted, with
  `frequency` giving it meaning (e.g. an `HOURLY` amount is an hourly rate);
  no normalization, annualization, or total is calculated in this task.
- Did not add a database-level `CHECK` for `endDate >= startDate`, matching
  the existing precedent that `Asset.planningValue <= estimatedValue` is
  enforced only in `AssetService`, not in the V2 migration; enforced it in
  `IncomeStreamService`/`ObligationService` via `InvalidScheduleException`
  instead.
- Followed the product brief's PD-001-PD-003: preserved caller-stated
  schedule semantics without aggregation/annualization; made income
  `certainty` and `compensationClassification` caller-supplied and returned
  verbatim (never inferred from `incomeType` or `amount`); implemented
  create/read only, with no update/delete/history endpoints.

**Assumptions**

- `certainty` and `compensationClassification` apply to `IncomeStream` only,
  per the product brief's field list; `Obligation` has no comparable
  certainty concept in this task.
- A caller-supplied three-letter currency code (normalized to uppercase),
  matching the `Asset`/`Liability` precedent, is sufficient for this
  increment; no per-currency FX or catalogue validation is applied.
- No household data was seeded; the manual verification evidence above used
  a disposable "Task 004 Smoke Test" household in the local Docker Compose
  Postgres volume, alongside pre-existing smoke-test households left by
  prior tasks in that same shared local volume.

**Open questions**

- Same as the product brief: an hourly-rate expected-hours companion field,
  required-vs-discretionary obligation flagging beyond `obligationType`,
  and day- vs. month-level date precision remain deferred to a concrete
  downstream planning need.
- Aggregation, FX conversion, taxes, and net-cash-flow semantics remain
  undefined until the deterministic planning-engine phase.

**Recommended next task**

- Phase 4 of the roadmap: financial snapshots (point-in-time net worth and
  cash-flow summary), which can now read from `Asset`, `Liability`,
  `IncomeStream`, and `Obligation`.

### 2026-09-03 — Task 003: Automate Delivery Gates (PR #2)

**Changed**

- Added `verify.sh` at the repository root: the single canonical verification
  command. It `cd`s into `backend/` (no location ambiguity for the caller)
  and runs `./mvnw --batch-mode test` — the complete Java 21 Maven suite,
  including the PostgreSQL/Testcontainers integration tests. Requires only a
  JDK on PATH (the Maven wrapper provisions Maven) and a running Docker
  daemon (used by Testcontainers, not to run Maven itself); exits nonzero on
  any test failure.
- Added `.github/workflows/verify.yml`: a least-privilege (`permissions:
  contents: read`) GitHub Actions workflow that runs on `pull_request`
  events targeting `main`, checks out the PR, sets up Temurin JDK 21, and
  runs `./verify.sh` unmodified — the same command local agents run. The job
  is named `verify`, which is also the exact reported status-check context.
- Revised `.github/pull_request_template.md` to require: the local
  `./verify.sh` result, the required CI `verify` check result and run link,
  the manually exercised primary flow, task/product-brief links, UI evidence
  (when applicable), and deviations/known limitations.
- Updated `AGENTS.md` ("Agent collaboration") and
  `agent/collaboration-workflow.md` ("Branching and pull requests", and
  step 4's checklist) so both documents consistently state: Codex may
  commit and push completed Product Owner review findings/acceptance
  without asking each time; Codex may merge only after its acceptance
  commit is on the PR and the required `verify` check is green; neither
  agent may merge past, or routinely bypass, a failed or missing required
  check; and same-account Codex review is not an independent formal GitHub
  approval — the product brief's recorded acceptance is the durable record.
- Added decision `D014` to `docs/decisions/decisions.md` documenting the
  required-CI-gate/one-canonical-command decision and its rationale.
- Updated `README.md`: the Tests section now documents `./verify.sh` as the
  canonical command (with `./mvnw test` from `backend/` noted as
  equivalent), and a new "Continuous integration" section documents the
  required `verify` check and points to this log for the settings
  read-back.
- Opened PR #2 (`task/003-delivery-gates` -> `main`) and configured `main`
  branch protection to require the `verify` status check.

**Tests / verification evidence**

- Local: `./verify.sh` from the repository root — `Tests run: 62, Failures:
  0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Confirmed identical to running
  `./mvnw -B test` directly from `backend/` (no behavior change from the
  command wrapper).
- Fail-fast behavior verified deliberately: temporarily injected a failing
  JUnit test (`Assertions.fail("canary")`) into `HouseholdServiceTest`, ran
  `./verify.sh`, confirmed exit code `1` and a Maven `BUILD FAILURE` /
  surefire failure report, then reverted the file (`git checkout --`) before
  committing anything.
- CI: PR #2's `verify` job on GitHub-hosted `ubuntu-latest`
  (run `https://github.com/codelinguist/waypoint/actions/runs/33758865382`)
  reported `Tests run: 62, Failures: 0, Errors: 0, Skipped: 0` and
  `BUILD SUCCESS` — the same count and result as the local run, satisfying
  the "reported test count agrees with local execution" criterion.
- Branch protection read-back (`gh api
  repos/codelinguist/waypoint/branches/main/protection`, credentials never
  logged):
  ```json
  {
    "required_status_checks": {
      "strict": false,
      "contexts": ["verify"],
      "checks": [{"context": "verify", "app_id": 15368}]
    },
    "enforce_admins": {"enabled": false},
    "required_linear_history": {"enabled": false},
    "allow_force_pushes": {"enabled": false},
    "allow_deletions": {"enabled": false}
  }
  ```
  No `required_pull_request_reviews` is configured (confirmed absent from
  the read-back), so `main` does not require an approving review that a
  single shared GitHub identity (`codelinguist`, used by both Claude Code
  and Codex) could not validly provide. `enforce_admins: false` preserves a
  deliberate administrator recovery path if the CI definition itself breaks,
  per the product brief's PD-002/PD-003 and the acceptance criteria.
- Confirmed the required check is live on PR #2 itself:
  `gh pr view 2 --json mergeable,mergeStateStatus,statusCheckRollup`
  reported `mergeable: MERGEABLE`, `mergeStateStatus: CLEAN`, and a single
  `verify` check with `conclusion: SUCCESS` — i.e., GitHub is actually
  evaluating the required check against this PR, not merely accepting the
  protection configuration.
- Directly observed GitHub blocking merge while the check runs, not just
  after: pushing the implementation-log update triggered a second `verify`
  run; while it was `IN_PROGRESS`, `gh pr view 2` reported
  `mergeStateStatus: BLOCKED`; once that run completed
  (`conclusion: SUCCESS`), the same query reported `mergeStateStatus: CLEAN`
  again — confirming the "GitHub prevents merge while the check is missing,
  running, or failing" behavior empirically, not just via configuration
  read-back.
- Full pre-existing suite (Task 001 + Task 002 tests) is included in the 62
  and remains green; no application-domain, API, schema, or UI code was
  touched by this task.

**Decisions**

- Wrapped the verification command as a thin shell script that `cd`s into
  `backend/` and calls the existing Maven wrapper directly on the runner's
  JDK, rather than re-running Maven inside a nested `maven:3.9-eclipse-
  temurin-21` container (as an earlier local Task 002 session had done to
  work around a sandbox that lacked a host JDK). Running Maven natively lets
  Testcontainers talk to the Docker daemon directly with zero extra
  networking configuration (no `TESTCONTAINERS_HOST_OVERRIDE` /
  `TESTCONTAINERS_RYUK_DISABLED` needed), works identically on this host and
  on GitHub-hosted `ubuntu-latest` runners via `actions/setup-java`, and
  avoids Docker-Desktop-vs-Linux `host.docker.internal` networking
  differences entirely. See `D014`.
- Named the workflow `Verify` and its single job `verify`; GitHub reports
  the status-check context as exactly `verify`, which is what branch
  protection now requires — confirmed empirically from the first live run
  rather than assumed.
- Required only the `verify` status check on `main`, deliberately omitting a
  required approving review, since Claude Code and Codex share one GitHub
  identity and an unsatisfiable requirement would deadlock every PR. Recorded
  this explicitly (`AGENTS.md`, `agent/collaboration-workflow.md`, PR
  template) rather than silently relying on the same-account review as if it
  were independent.
- Left `enforce_admins: false` so a broken CI definition has a real recovery
  path, per the acceptance criteria; this is intentionally not a routine
  bypass and is documented as a deliberate exception in `D014` and the
  collaboration workflow.

**Assumptions**

- GitHub Actions and branch protection on this repository's current plan
  support one required status check without cost; confirmed directly rather
  than assumed, since this session has admin access to `codelinguist/
  waypoint`.
- A JDK is available on every environment expected to run `./verify.sh`
  directly (this host, and GitHub Actions via `actions/setup-java`). If a
  future contributor's environment has Docker but no JDK, `verify.sh` would
  need a container-based fallback; not built now since it is not the
  documented constraint (Docker, not Java, is the project's stated only
  local-development prerequisite for *running the application*; running
  tests directly has always been the documented "optional, faster inner
  loop" path per `README.md`).

**Open questions**

- Per the product brief: separate GitHub identities/Apps for genuinely
  independent reviewer identity, and additional security/UI/deployment/
  risk-tier-specific checks, remain deliberate follow-up work, not part of
  this baseline gate.

**Recommended next task**

- Return to the Product Owner Agent (Codex) for independent review of PR #2
  against `agent/product/delivery-gates/product-brief.md`'s acceptance
  criteria, then resume the product-domain roadmap (e.g., financial
  snapshots or the facts/assumptions provenance model) once Task 003 is
  accepted and merged.

### 2026-09-03 — Task 002 review findings addressed (PR #1)

**Changed**

- F-001 (BLOCKING): Added `@Digits(integer = 17, fraction = 2)` to
  `estimatedValue`/`planningValue` (`CreateAssetRequest`) and
  `outstandingBalance` (`CreateLiabilityRequest`), matching `NUMERIC(19,2)`.
  Excess fractional scale and precision overflow are now rejected with a
  structured 400 before persistence instead of being silently rounded or
  crashing with a 500.
- F-002 (BLOCKING): Set
  `spring.jackson.deserialization.fail-on-unknown-properties: true` in
  `application.yml`. A client can no longer submit `sourceType` (or any other
  unrecognized field) on a create request; it is now rejected with a
  structured 400 (`MALFORMED_REQUEST`) instead of being silently ignored.
  Applied globally rather than per-DTO so it also covers future request types.
- F-003 (BLOCKING): Added the missing liability-side integration tests that
  already existed for assets (blank name, malformed currency, unrecognized
  enum, future date, populated-list ordering with duplicate names), plus new
  exact-decimal round-trip, excessive-scale, precision-overflow, and
  unsupported-provenance tests for both assets and liabilities.
- F-004 (BLOCKING): The PR's documented test command mounted `$(pwd)` at
  `/workspace` without noting it must be run from `backend/` (`pom.xml` lives
  there, not at the repo root). Corrected the command in the PR description
  and this log to mount `backend/` explicitly.

**Tests**

- 62 tests passing under Java 21 (`docker run --rm -v "$(pwd)/backend":/workspace -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e TESTCONTAINERS_RYUK_DISABLED=true -w /workspace maven:3.9-eclipse-temurin-21 mvn -B test`, run from the repository root), up from 49 — the 13 new tests cover the four findings above.
- Live-verified against a rebuilt `docker compose up --build`: unsupported
  `sourceType` now returns 400 `MALFORMED_REQUEST`; excess fractional scale
  and precision overflow both now return 400 `VALIDATION_FAILED` (the latter
  previously crashed with a raw 500); a valid exact-decimal value
  (`1234.56`) still round-trips correctly.

**Decisions**

- Fixed unsupported-field rejection globally via Jackson configuration rather
  than per-DTO, since the underlying concern (clients cannot claim
  server-owned fields) applies to every request type, not only this task's.

**Open questions**

- Same as the initial Task 002 entry below; unchanged by this fix round.

**Recommended next task**

- Return to the Product Owner Agent for re-review of PR #1 against the four
  `ACCEPTED` findings above.

### 2026-09-03 — Task 002: Record Household Assets and Liabilities

**Changed**

- Added Flyway `V2__create_assets_and_liabilities.sql`: `assets` and
  `liabilities` tables, FKs to `households`, scoped indexes, and DB-level
  CHECK constraints for non-negative values and `planning_value <=
  estimated_value`.
- Added `Asset`/`Liability` JPA entities and `AssetType`/`LiabilityType`/
  `Liquidity`/`SourceType` enums under `com.waypoint.household`, mirroring the
  Task 001 package layout.
- Added `AssetRepository`/`LiabilityRepository` with household-scoped and
  household+id-scoped lookups; `AssetService`/`LiabilityService` implementing
  create/get/list, household existence checks, and the planning-value-vs-
  estimated-value business rule (`InvalidAssetValueException`).
- Added `AssetController`/`LiabilityController` under
  `/api/households/{householdId}/assets` and `.../liabilities`, validated
  request/response DTOs, and three new `ApiExceptionHandler` mappings
  (`ASSET_NOT_FOUND`, `LIABILITY_NOT_FOUND`, and the reused
  `VALIDATION_FAILED` code for the cross-field business rule).
- Documented representative requests and enum values in `README.md`, and
  extended its Backend/Status sections to cover Phase 2.

**Tests**

- 49 tests passing under Java 21 (`docker run maven:3.9-eclipse-temurin-21`,
  matching the project's own Dockerfile base image): all prior Task 001 tests
  plus `AssetServiceTest` (7), `LiabilityServiceTest` (6), and
  `AssetLiabilityApiIntegrationTest` (19).
- `AssetLiabilityApiIntegrationTest` runs against a real, ephemeral
  Testcontainers PostgreSQL instance and confirmed Flyway applies V1 then V2
  cleanly on an empty schema.
- `docker compose up --build` against the existing Task 001 named volume
  confirmed a genuine upgrade path: Flyway logged "Current version of schema
  public: 1" then "Migrating schema public to version 2", both services
  reported healthy.
- Live smoke tests against the running container covered: create/get/list for
  both assets and liabilities; zero-value acceptance; negative-value
  rejection; planning-value-exceeds-estimated-value rejection; future-date
  rejection; unknown-household 404 on create; and cross-household 404 without
  disclosing the record's existence.

**Decisions**

- Kept `Asset`/`Liability` in the existing flat `com.waypoint.household`
  package rather than introducing a new sub-package, consistent with Task
  001's structure and the brief's smallest-increment scope.
- Validated non-negative values and the not-in-the-future date constraint
  declaratively via Bean Validation (`@DecimalMin`, `@PastOrPresent`) to match
  the existing DTO style; validated the planning-value-vs-estimated-value
  cross-field rule in the service layer (not a class-level Bean Validation
  constraint) since it is a genuine domain invariant, not a request-shape
  check, and needs to be independently unit-testable per `AGENTS.md`.
- Added DB-level CHECK constraints mirroring the two non-negative rules and
  the planning/estimated relationship, as defense-in-depth for financial
  data integrity; did not add a currency-format CHECK constraint, matching
  Task 001's precedent of leaving that to application-level validation.
- Reused the `VALIDATION_FAILED` error code for the planning-value business
  rule rather than minting a new code, since the brief treats it as one
  family of input-validation failures alongside the Bean Validation checks.

**Assumptions**

- "Upgrades a Task 001 database" is satisfied by both the Testcontainers
  empty-schema run (V1 then V2 in sequence) and the Docker Compose run against
  the already-migrated Task 001 volume; no separate V1-only intermediate
  environment was constructed, since Flyway's version-tracking behavior here
  is standard and not specific to this migration.
- Docker-in-Docker networking on this host's Docker Desktop required
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` and
  `TESTCONTAINERS_RYUK_DISABLED=true` when running Testcontainers-backed
  tests from inside the `maven:3.9-eclipse-temurin-21` container; this is a
  local test-runner detail, not a project dependency change.

**Open questions**

- Value-history semantics (how an asset/liability value changes over time)
  remain undecided before any update endpoint is introduced, per PD-004.
- Person-level ownership of individual assets/liabilities is still deferred.
- Exchange-rate sources and cross-currency aggregation remain undefined.

**Recommended next task**

- Independent Product Owner acceptance of Task 002 against
  `agent/product/assets-liabilities/product-brief.md`, then frame the next
  vertical increment (e.g., financial snapshots or the facts/assumptions
  provenance model) per the roadmap.

### 2026-09-03 — Task 001 product acceptance

**Changed**

- Product Owner Agent accepted Task 001 against the linked Household Foundation
  product brief and recorded the evidence there.
- No implementation changes were required during acceptance.

**Tests**

- Independently ran the complete suite under the declared Java 21 Docker
  environment: 17 tests passed with 0 failures and 0 errors.
- Built and started the application with Docker Compose from an empty database;
  PostgreSQL and the application both became healthy, and Flyway applied V1.
- Exercised household create/retrieve, person add/list, validation, and
  not-found responses through the live HTTP API.
- Verified a created household remained retrievable after `docker compose down`
  and `docker compose up`, confirming named-volume persistence.

**Decisions**

- Accepted Task 001 with no returned work or new architectural decision.

**Assumptions**

- The failed host-side test attempt under an installed Java 26 runtime is not a
  product failure: the project declares Java 21, and the same suite passed in
  the Java 21 Docker environment. Docker remains the canonical local path under
  D013.

**Open questions**

- Person-role vocabulary and member lifecycle remain intentionally deferred.
- Authentication remains required before any non-private deployment.

**Recommended next task**

- Frame Phase 2 as a small vertical increment for household assets and
  liabilities before overwriting `agent/current-task.md`.

### 2026-09-03 — Task 001: Scaffold the backend and Household aggregate

**Changed**

- Added a Spring Boot 3.5 / Java 21 backend at `backend/`, built with Maven
  (`backend/pom.xml`, `backend/mvnw`).
- Implemented the `Household` and `Person` JPA entities
  (`backend/src/main/java/com/waypoint/household/`), with UUID primary keys,
  Hibernate-managed `createdAt`/`updatedAt` timestamps, and a `Person` →
  `Household` many-to-one association.
- Added `HouseholdRepository` and `PersonRepository` (Spring Data JPA), a
  `HouseholdService` and `PersonService` for creation/lookup, and a
  `HouseholdNotFoundException` mapped to HTTP 404.
- Added Bean Validation request DTOs (`CreateHouseholdRequest`,
  `CreatePersonRequest`) and response DTOs (`HouseholdResponse`,
  `PersonResponse`), plus `HouseholdController` and `PersonController`
  exposing:
  - `POST /api/households`, `GET /api/households/{id}`
  - `POST /api/households/{id}/people`, `GET /api/households/{id}/people`
- Added a `@RestControllerAdvice` (`ApiExceptionHandler`) returning a
  structured `ErrorResponse` for validation failures, malformed
  requests/path variables, and unknown households.
- Added Flyway migration `V1__create_household_and_person.sql` creating
  `households` and `people` tables with a foreign key and an index on
  `people.household_id`.
- Added `backend/src/main/resources/application.yml`, sourcing all
  datasource/server settings from environment variables
  (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
  `SERVER_PORT`) with local-friendly defaults, and enabled the Actuator
  `health` endpoint.
- Added a multi-stage `backend/Dockerfile` (Maven build stage, Eclipse
  Temurin 21 JRE Alpine runtime stage, non-root user, container
  `HEALTHCHECK` against `/actuator/health`).
- Added root `docker-compose.yml` with `postgres` (named volume
  `waypoint-postgres-data`, `pg_isready` healthcheck) and `app`
  (`depends_on: condition: service_healthy`), plus `.env.example` and a
  `.gitignore` covering `backend/target/`, `.env`, and editor/OS files.
- Updated `README.md` with Docker-based setup, shutdown/reset, logs, host
  run, and test instructions.

**Tests**

- `HouseholdServiceTest` / `PersonServiceTest` (Mockito-based unit tests):
  name/currency/role normalization, and not-found behavior for unknown
  households.
- `HouseholdApiIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`
  + Testcontainers `PostgreSQLContainer`, real Flyway migration run):
  household create/retrieve, blank-name and malformed-currency validation
  (no persisted record), unknown-household 404 on get/add/list, empty
  member list for a new household, creation-order member listing,
  household-scoped member isolation, and permitted duplicate person names.
  17 tests total, all passing (`./mvnw test`).
- Verified `docker compose up --build` end-to-end from a clean state:
  app waits for Postgres `service_healthy` before starting, Flyway runs
  migrations automatically, the app container's own `HEALTHCHECK` reports
  `healthy`, household/person creation and retrieval work via `curl`,
  validation and not-found paths return 400/404, and a household created
  before `docker compose down` (no `-v`) was still retrievable after
  `docker compose up` again, confirming volume persistence.

**Decisions**

- Fixed a schema/entity mismatch found during testing: the initial
  migration declared `base_currency` as `CHAR(3)`, but Hibernate schema
  validation (`ddl-auto: validate`) expects `VARCHAR(3)` for a
  `@Column(length = 3)` String field; the migration now uses `VARCHAR(3)`.
- Used `GenerationType.UUID` (Hibernate 6) for entity IDs rather than a
  database-side UUID default, keeping ID generation in application code.
- Placed `Person` in the `household` package/module rather than a separate
  top-level module, matching the architecture doc's single "household"
  conceptual module for this increment.
- Followed the product brief's PD-001–PD-005: no seeded household/person
  data, minimal `Person` fields (name, role, household, id, timestamps),
  blank-field rejection, uppercase-normalized currency, not-found semantics
  for unknown households, member ordering by creation time with ID as a
  stable tie-breaker, and permitted duplicate names.
- Chose Spring Boot 3.5.16 (latest 3.5.x at implementation time) over the
  newly available Spring Boot 4.x line, to avoid taking on an unreviewed
  major-version migration in the foundational scaffold.

**Assumptions**

- A caller-supplied three-letter currency code (normalized to uppercase) is
  sufficient validation for this increment; no currency catalogue check is
  applied yet, per the product brief.
- No authentication/authorization exists yet; the API is intended for
  trusted local/private use only, per Task 001 constraints.
- `role` remains free text with no fixed vocabulary or authorization
  meaning.

**Open questions**

- Same as the product brief: person-role vocabulary, and
  update/delete/archival/membership-transfer behavior are undefined and
  deferred until a concrete downstream feature needs them.
- Authentication must be designed before any non-private deployment.

**Recommended next task**

- Phase 2 of the roadmap: `Asset` and `Liability` aggregates (balances,
  valuation dates, liquidity classification), building on the `Household`
  foundation established here.

---

### 2026-09-03 — Cross-agent collaboration workflow

**Changed**

- Added a repository-native Claude Code-to-Codex UI workflow.
- Added durable design-brief and visual-review templates.
- Added shared-agent handoff rules and Claude Code repository instructions.
- Added an explicit Product Owner role for problem framing, prioritization,
  design decisions, review triage, and final feature acceptance.
- Reassigned product ownership from Ralph to a dedicated Product Owner Agent and
  added its role charter and reusable product-brief template.

**Tests**

- Documentation structure and references reviewed manually; no application tests
  apply because application code has not been scaffolded.

**Decisions**

- Claude Code defaults to UI exploration and visual review; Codex defaults to
  implementation, integration, and final verification.
- Ralph and his wife are users and household authorities, not default product
  managers.
- The Product Owner Agent owns routine product decisions and evidence-based
  feature acceptance in a session separate from implementation.
- Product acceptance is distinct from household approval of material financial
  decisions or canonical-data changes.
- Agent identity does not override task scope or verified output quality.

**Assumptions**

- UI artifacts will be introduced only when a current task includes UI work.
- Claude Code will be installed and authenticated separately before its first
  live workflow stage.

**Open questions**

- No design system or screenshot harness is selected because the frontend is not
  yet in scope.

**Recommended next task**

- Continue Task 001: scaffold the Java backend and Household aggregate.

## Template for future entries

### YYYY-MM-DD — Task XXX

**Changed**

- ...

**Tests**

- ...

**Decisions**

- ...

**Assumptions**

- ...

**Open questions**

- ...

**Recommended next task**

- ...
