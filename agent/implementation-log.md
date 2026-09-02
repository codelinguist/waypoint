# Implementation Log

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
