# Current Task

## Task 001 — Scaffold the Backend and Household Aggregate

### Goal

Create the initial backend application and implement the first domain aggregate:

- Household
- Person

### Stack

- Java with Spring Boot
- PostgreSQL
- Spring Data JPA with Hibernate
- Flyway
- JUnit 5
- Testcontainers where PostgreSQL integration behavior is under test
- Docker and Docker Compose for the local development environment

### Requirements

Implement:

1. Spring Boot application scaffold.
2. PostgreSQL configuration through environment variables.
3. JPA/Hibernate configuration.
4. Flyway migrations.
5. `Household` persistence model.
6. `Person` persistence model.
7. Corresponding request/response DTOs with Bean Validation.
8. REST endpoints to:
   - create a household
   - retrieve a household
   - add a person to a household
   - retrieve household members
9. Automated tests.
10. A production-shaped `Dockerfile` for the Spring Boot application.
11. A Docker Compose configuration that starts:
    - the Spring Boot application
    - PostgreSQL with a persistent named volume and health check
12. Container configuration that waits for PostgreSQL readiness and supplies
    application/database settings through environment variables without
    committing secrets.
13. Local development instructions in `README.md`, with
    `docker compose up --build` as the primary startup path.

### Domain expectations

Household should include at least:

- UUID id
- name
- base_currency
- created_at
- updated_at

Person should include at least:

- UUID id
- household_id
- name
- role
- created_at
- updated_at

Do not add unnecessary personal/sensitive information yet.

### Constraints

Do not implement:

- authentication
- AI features
- assets
- liabilities
- transactions
- event sourcing
- queues
- vector database
- external integrations
- Kubernetes
- production deployment infrastructure

Docker is required for local development, but the setup should remain small:
one application container and one PostgreSQL container. Do not introduce a
general-purpose container platform or production orchestration concerns.

### Definition of Done

- `docker compose up --build` starts the application and PostgreSQL from a
  clean checkout with only Docker as a prerequisite
- application becomes ready only after it can connect to PostgreSQL
- database migrations run successfully
- household can be created and retrieved
- household member can be added and retrieved
- tests pass
- PostgreSQL data survives a normal Compose stop/start cycle
- no secrets are committed in Docker, Compose, or application configuration
- README explains Docker-based setup, shutdown, data reset, logs, and test
  commands
- `agent/implementation-log.md` is updated
