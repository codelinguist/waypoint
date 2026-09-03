# Implementation Log

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
