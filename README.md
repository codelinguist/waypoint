# Family Financial AI

A personal AI-powered financial planning system designed initially for Ralph and his wife.

The product is intended to become a long-lived financial operating system for a household: it maintains structured financial state, goals, assumptions, decisions, and historical snapshots, while an AI layer helps interpret changes, run scenarios, explain tradeoffs, and support planning.

## Core idea

The LLM is **not** the source of truth.

Canonical financial state lives in structured application data. The AI can interpret, explain, recommend, propose changes, and invoke deterministic tools, but it must not silently invent or overwrite financial facts.

## Initial users

- Ralph
- Ralph's wife

They are both founders/users of the first version. The application should be built for their real financial planning needs before being generalized into a broader product.

## Start here

Read these documents in order:

1. `AGENTS.md`
2. `docs/product/vision.md`
3. `docs/product/user-zero.md`
4. `docs/product/problems.md`
5. `docs/product/principles.md`
6. `docs/domain/financial-model.md`
7. `docs/architecture/architecture.md`
8. `docs/decisions/decisions.md`
9. `docs/product/roadmap.md`
10. `agent/current-task.md`

## Agent collaboration workflow

UI features use a planning-to-implementation handoff between Codex (Product
Owner Agent) and Claude Code (implementer). The workflow, approval gates, and
reusable artifacts are documented in
[`agent/collaboration-workflow.md`](agent/collaboration-workflow.md).

The short version is:

1. The user presents a problem, context, correction, or feedback.
2. Codex, acting as the Product Owner Agent in its own session, defines the
   outcome, priority, scope, and acceptance criteria in a durable product
   brief, then points `agent/current-task.md` at it.
3. Claude Code explores UI directions and writes a design brief.
4. Codex selects a direction based on the product brief and asks the user
   only when a material preference cannot be inferred safely.
5. Claude Code implements the approved design and verifies it in the real
   application.
6. Codex reviews the rendered evidence, without editing the implementation,
   and triages findings.
7. Claude Code applies accepted changes and re-verifies.
8. Codex accepts the completed feature against evidence.

This workflow applies only when `agent/current-task.md` includes UI work. The
current backend task does not require a speculative frontend or design pass.

## Backend

The `backend/` directory contains the Spring Boot modular monolith described
in `docs/architecture/architecture.md`. It currently implements the
`Household` and `Person` aggregates (Phase 1 of `docs/product/roadmap.md`):
create and retrieve a household, and add and retrieve its members.

### Prerequisites

- Docker and Docker Compose (the only required prerequisite for local
  development).
- Java 21 and Maven are only needed if you want to run the application or
  tests directly on the host instead of in Docker. A Maven wrapper
  (`backend/mvnw`) is included so a system-wide Maven install is not required.

### Run with Docker Compose (primary path)

From the repository root:

```bash
cp .env.example .env   # first time only; edit values if you want non-default credentials
docker compose up --build
```

This builds the Spring Boot application image, starts PostgreSQL and the
application, and runs Flyway migrations automatically on application startup.
The application container waits for PostgreSQL to report healthy
(`depends_on: condition: service_healthy`, backed by `pg_isready`) before it
starts, so the API only becomes reachable once it can connect to the
database.

Once running, the API is available at `http://localhost:8080` (configurable
via `SERVER_PORT` in `.env`). For example:

```bash
curl -X POST http://localhost:8080/api/households \
  -H 'Content-Type: application/json' \
  -d '{"name": "Example Household", "baseCurrency": "PHP"}'
```

### Stopping and resetting

```bash
docker compose down          # stop containers, keep data
docker compose down -v       # stop containers and delete the Postgres volume (full reset)
docker compose up --build    # start again; data persists across stop/start unless -v was used
```

PostgreSQL data is stored in the named volume `waypoint-postgres-data`, so it
survives a normal `docker compose down` / `docker compose up` cycle.

### Logs

```bash
docker compose logs -f app        # application logs
docker compose logs -f postgres   # database logs
```

### Environment variables

No secrets are committed. `docker-compose.yml` and
`backend/src/main/resources/application.yml` read all database and server
settings from environment variables, with local-friendly defaults. Copy
`.env.example` to `.env` and adjust as needed; `.env` is gitignored.

| Variable      | Purpose                          | Default    |
|---------------|-----------------------------------|-----------|
| `DB_NAME`     | PostgreSQL database name          | `waypoint` |
| `DB_USER`     | PostgreSQL user                   | `waypoint` |
| `DB_PASSWORD` | PostgreSQL password               | `waypoint` |
| `DB_PORT`     | Host port mapped to PostgreSQL    | `5432`     |
| `SERVER_PORT` | Host port mapped to the API       | `8080`     |

### Running on the host (optional, faster inner loop)

PostgreSQL must still run somewhere reachable (for example via
`docker compose up postgres`). Then, from `backend/`:

```bash
./mvnw spring-boot:run
```

Connection settings default to `localhost:5432` and can be overridden with
the same `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and
`SERVER_PORT` environment variables used by Compose.

### Tests

```bash
cd backend
./mvnw test
```

Tests include unit coverage of the household/person services and
Testcontainers-backed integration tests that run the application against a
real, ephemeral PostgreSQL container (Flyway migrations included). Docker
must be running locally for the Testcontainers-backed tests to execute.

## Status

Phase 1 of the roadmap (Household and Person foundation) is implemented. See
`agent/implementation-log.md` for the current state and next recommended
task.
