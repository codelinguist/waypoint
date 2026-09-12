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

Agents start with `AGENTS.md`, then activate only the current stage file and
the context it names. Do not treat this README as a mandatory full-document
reading list. Humans seeking a full product orientation can continue through
`docs/product/`, `docs/domain/financial-model.md`,
`docs/architecture/architecture.md`, `docs/decisions/index.md`, and the
roadmap.

## Agent collaboration workflow

Every feature uses a planning-to-implementation handoff between Codex (Product
Owner Agent) and Claude Code (implementer), shipped through a branch and pull
request rather than a direct commit to `main`. The workflow, approval gates,
branching mechanics, and reusable artifacts are documented in
[`agent/collaboration-workflow.md`](agent/collaboration-workflow.md).

Frame, design approval, review, and accept are Codex's stages and happen
directly between the user and Codex in Codex's own session; Claude Code is
not invoked for them. Claude Code owns Implement, Revise, and (on request)
Ship. The short version is:

1. The user presents a problem, context, correction, or feedback directly to
   Codex.
2. Codex, acting as the Product Owner Agent, defines the outcome, priority,
   scope, and acceptance criteria, creates a Jira issue, and moves it to To Do.
3. Claude Code explores UI directions and writes a design brief (UI tasks only).
4. Codex selects a direction directly with the user, asking only when a
   material preference cannot be inferred safely (UI tasks only).
5. Claude Code implements on a task branch, verifies it in the real
   application, and pushes the branch and opens a PR.
6. Codex reviews the PR's diff and evidence directly with the user, without
   editing the implementation, and records findings as comments on the Jira
   issue.
7. Claude Code applies accepted changes and re-verifies.
8. Codex accepts the completed feature against evidence, recording acceptance
   as a Jira comment and authorizing the merge.

The user initiates each meaningful stage with a short command or request.
Agents complete the work within that stage, then report the result. There is
no automatic task pickup, review/fix loop, or merge. See the
[command-driven workflow](agent/collaboration-workflow.md). CI and independent
acceptance remain required; shipping requires an explicit user request.

The design-brief and visual-review stages (steps 3–4 and the review evidence
in step 6) apply only when the Jira issue includes UI work.

## Backend

The `backend/` directory contains the Spring Boot modular monolith described
in `docs/architecture/architecture.md`. It currently implements:

- `Household` and `Person` (Phase 1 of `docs/product/roadmap.md`): create and
  retrieve a household, and add and retrieve its members.
- `Asset` and `Liability` (Phase 2): record and retrieve a household's owned
  and owed balance-sheet items with explicit values, dates, currencies, and
  liquidity/type classification. Create and read only — no update, delete, or
  aggregation yet.
- `IncomeStream` and `Obligation` (Phase 3): record and retrieve a household's
  recurring cash inflows and outflows with explicit amount/rate, frequency,
  currency, dates, and — for income — gross/net/unknown and
  confirmed/expected/variable classification. Create and read only; no
  update, delete, aggregation, FX conversion, or cash-flow totals yet.
- `FinancialSnapshot` (Phase 4): create an immutable, point-in-time capture
  of a household's known asset/liability balance-sheet state as of a
  caller-supplied date, with copied line items and deterministic
  per-currency asset/liability/net-worth totals. Create and read only; no
  update, delete, FX conversion, or income/obligation snapshots yet.

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

Assets and liabilities are recorded and read under a household, and accept
only explicit, caller-supplied values. Assets are create/read-only; a
liability's outstanding balance can additionally be replaced through an
auditable balance-history resource (below):

```bash
curl -X POST http://localhost:8080/api/households/{householdId}/assets \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Emergency Fund",
        "assetType": "CASH",
        "estimatedValue": "1000.00",
        "planningValue": "1000.00",
        "currency": "PHP",
        "valuedAt": "2026-09-01",
        "liquidity": "LIQUID"
      }'

curl http://localhost:8080/api/households/{householdId}/assets
curl http://localhost:8080/api/households/{householdId}/assets/{assetId}

curl -X POST http://localhost:8080/api/households/{householdId}/liabilities \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Credit Card",
        "liabilityType": "CREDIT_CARD",
        "outstandingBalance": "500.00",
        "currency": "PHP",
        "balanceAsOf": "2026-09-01"
      }'

curl http://localhost:8080/api/households/{householdId}/liabilities
curl http://localhost:8080/api/households/{householdId}/liabilities/{liabilityId}
```

`assetType` is one of `CASH`, `BANK_ACCOUNT`, `PROPERTY`, `INVESTMENT`,
`BUSINESS_OWNERSHIP`, `OTHER`. `liabilityType` is one of `CREDIT_CARD`,
`MORTGAGE`, `PERSONAL_LOAN`, `BUSINESS_LOAN`, `OTHER`. `liquidity` is one of
`LIQUID`, `RESTRICTED`, `ILLIQUID`. `planningValue` must not exceed
`estimatedValue`; all monetary values must be non-negative; `valuedAt` and
`balanceAsOf` must not be in the future. Every created record is stamped
`sourceType: "MANUAL_ENTRY"` by the server. Every `Liability` also carries a
`revision` (starts at `0`), used below for conditional balance updates.

A liability's outstanding balance is replaced — never edited in place —
through its balance-history resource, which requires the liability's current
`revision` and appends an immutable audit row for every accepted change:

```bash
curl -X POST http://localhost:8080/api/households/{householdId}/liabilities/{liabilityId}/balances \
  -H 'Content-Type: application/json' \
  -d '{
        "outstandingBalance": "300.00",
        "balanceAsOf": "2026-09-10",
        "reason": "Paid down with September bonus",
        "expectedRevision": 0
      }'

curl http://localhost:8080/api/households/{householdId}/liabilities/{liabilityId}/balances
```

The POST replaces the liability's current `outstandingBalance`/`balanceAsOf`
(re-asserting `MANUAL_ENTRY` provenance) and appends a history row recording
the before/after balance, before/after source date, before/after source
type, currency, `reason`, the resulting `revision`, and server `recordedAt`
— atomically, in one transaction. `expectedRevision` must match the
liability's current `revision`; a stale value (including a replay of an
already-applied revision) is rejected with a structured `409
LIABILITY_REVISION_CONFLICT` and appends nothing. Two concurrent submissions
starting from the same revision resolve to exactly one success and one such
409, with exactly one audit row appended — never a lost update. The GET
returns history in deterministic revision order (oldest first) and is empty
until the first balance replacement; a liability's identity, household,
name, type, and currency are never affected by a balance replacement. There
is no edit or delete API for history rows. `outstandingBalance` accepts zero
and increases as well as decreases; `reason` must be non-blank.

Income streams and recurring obligations are recorded and read the same way.
Income streams may start in the future (e.g. a job that has not begun yet),
unlike `valuedAt`/`balanceAsOf` above:

```bash
curl -X POST http://localhost:8080/api/households/{householdId}/income-streams \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "New Job Salary",
        "incomeType": "SALARY",
        "amount": "50000.00",
        "frequency": "MONTHLY",
        "currency": "PHP",
        "compensationClassification": "GROSS",
        "certainty": "EXPECTED",
        "startDate": "2026-10-01"
      }'

curl http://localhost:8080/api/households/{householdId}/income-streams
curl http://localhost:8080/api/households/{householdId}/income-streams/{incomeStreamId}

curl -X POST http://localhost:8080/api/households/{householdId}/obligations \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Mortgage",
        "obligationType": "MORTGAGE",
        "amount": "22000.00",
        "frequency": "MONTHLY",
        "currency": "PHP",
        "startDate": "2026-09-01"
      }'

curl http://localhost:8080/api/households/{householdId}/obligations
curl http://localhost:8080/api/households/{householdId}/obligations/{obligationId}
```

`incomeType` is one of `SALARY`, `HOURLY_CONTRACT`, `BUSINESS_DISTRIBUTION`,
`OTHER`. `obligationType` is one of `HOUSEHOLD_BASELINE`, `MORTGAGE`,
`LOAN_PAYMENT`, `INSURANCE`, `TUITION`, `TRAVEL_SINKING_FUND`,
`DISCRETIONARY`, `OTHER`. `frequency` (shared by both) is one of `HOURLY`,
`WEEKLY`, `BIWEEKLY`, `MONTHLY`, `ANNUAL`. `compensationClassification`
(income only) is one of `GROSS`, `NET`, `UNKNOWN`. `certainty` (income only)
is one of `CONFIRMED`, `EXPECTED`, `VARIABLE` and is always returned exactly
as submitted — it is never inferred from `incomeType` or `amount`, so
`EXPECTED`/`VARIABLE` income is never presented as a confirmed fact.
`amount` represents either a flat amount or a rate depending on `frequency`
(e.g. an `HOURLY` amount is an hourly rate); no annualization, FX conversion,
or cash-flow total is calculated. `amount` must be non-negative; `endDate`,
if present, must not precede `startDate`; `startDate` may be in the future.
Every created record is stamped `sourceType: "MANUAL_ENTRY"` by the server.

Financial snapshots capture the household's current assets and liabilities
as of a requested date. Only `asOfDate` is supplied by the caller — the
server selects eligible records, copies their values, and computes totals:

```bash
curl -X POST http://localhost:8080/api/households/{householdId}/financial-snapshots \
  -H 'Content-Type: application/json' \
  -d '{"asOfDate": "2026-09-01"}'

curl http://localhost:8080/api/households/{householdId}/financial-snapshots
curl http://localhost:8080/api/households/{householdId}/financial-snapshots/{snapshotId}
```

An asset is eligible when its `valuedAt` is on or before `asOfDate`; a
liability is eligible when its `balanceAsOf` is on or before `asOfDate`
(later-dated records are excluded). Each line item copies the source
record's identity (`sourceAssetId`/`sourceLiabilityId`), name, type,
currency, source date, and exact value (`planningValue` for assets,
`outstandingBalance` for liabilities) at capture time — a snapshot is a
point-in-time copy, so a later liability balance replacement (or any other
source-record change) cannot alter an existing snapshot; a new snapshot
reflects the current values through the same read path as any other query.
`totalsByCurrency` sums asset and liability line items
separately within each currency and derives net worth
(`assetTotal - liabilityTotal`) for that currency only; currencies are never
combined, and a currency present on only one side shows a zero total for the
other. `asOfDate` cannot be in the future. `capturedAt` is the actual
generation time and is distinct from the caller-supplied `asOfDate` — a
snapshot does not claim to reconstruct historical values that were never
stored, only to filter currently stored records by date. Snapshots are
create/read-only (no update or delete), and duplicate `asOfDate` values
across snapshots are permitted since each capture is a distinct observation.
Every created snapshot is stamped `sourceType: "MANUAL_ENTRY"` by the server.

Financial goals record a household's monetary targets and their progress:

```bash
curl -X POST http://localhost:8080/api/households/{householdId}/goals \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "Retirement",
        "targetAmount": "1000000.00",
        "currency": "PHP",
        "targetDate": "2046-01-01",
        "priority": 1,
        "currentAmount": "50000.00"
      }'

curl http://localhost:8080/api/households/{householdId}/goals
curl http://localhost:8080/api/households/{householdId}/goals/{goalId}
```

`targetAmount` must be greater than 0; `currentAmount` is caller-supplied and
must not be negative — the system never infers it from assets, liabilities,
or snapshots. `targetDate` must not be in the past (today or later).
`priority` is a positive integer where lower numbers are higher priority;
list results are ordered by ascending priority. Every response also returns
computed `remainingAmount` (`targetAmount - currentAmount`, which can go
negative for an overachieved goal) and `progressPercentage`
(`currentAmount / targetAmount * 100`, bounded to `[0, 100]`) — both are
derived on every read from the stored amounts, never stored themselves, so
they can never drift out of sync. Goals are create/read-only in this
increment (no update, delete, or contribution history).

The read-only current financial position — a household's recorded asset and
liability rows plus per-currency totals in one coherent read — is available
at `GET /api/households/{householdId}/financial-position`; see
`agent/product/current-financial-position/api.md` for the full contract
(exact decimal-string monetary transport, no FX aggregation). This is what
the frontend below consumes.

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
| `FRONTEND_PORT` | Host port mapped to the frontend | `5173`   |
| `HOUSEHOLD_ID` | Household the frontend displays (see "Frontend" below) | unset |

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

Run the canonical repository verification command from the repository root
(requires a JDK on PATH and a running Docker daemon; Docker is used by
Testcontainers, not to run Maven itself):

```bash
./verify.sh
```

This is exactly what the required `verify` GitHub Actions check runs on every
pull request targeting `main` — there is one definition of "green," not
separate local and CI notions of passing. Equivalently, from `backend/`:
`./mvnw test`.

Tests include unit coverage of the household/person, asset/liability,
income-stream/obligation, and financial-snapshot services, and
Testcontainers-backed integration tests that run the application against a
real, ephemeral PostgreSQL container (Flyway migrations included).

## Frontend

The `frontend/` directory is a private, read-only React/TypeScript app for
one already-configured household, with multiple sections reachable through
a minimal in-page nav (no router; see `App.tsx`'s `AppNav`). Notably:

- **Financial position** (Phase 9 early slice, WAP-15) follows the approved
  design in `agent/ui/financial-position/design-brief.md` (currency-first
  summary cards, expandable holdings) and consumes the financial-position
  endpoint described above.
- **Income & obligations** (WAP-21) lists every recorded income stream and
  recurring obligation for the household (see
  `agent/product/income-obligations/api.md`), including income's
  `certainty` classification — anything other than `CONFIRMED` is visually
  flagged, consistent with AGENTS.md's facts-vs-assumptions rule. It reuses
  the financial-position page's table/badge/loading/error conventions rather
  than introducing a new visual language.

Record creation/editing, household creation/selection, authentication, and
FX conversion are explicitly out of scope for every section.

### Configuring the displayed household

The frontend never creates or selects a household itself — an operator
configures an existing one outside the ordinary product flow, via the
`HOUSEHOLD_ID` environment variable (a household's `id`, returned when you
create it — see the `curl -X POST .../api/households` example above). Leave
it unset to see the app's own "no household is configured" state instead of
a broken page; an ID that doesn't match any household shows a distinct
"household not found" state naming the configured ID.

### Run with Docker Compose

The `frontend` service builds alongside `postgres` and `app` from the same
root `docker compose up --build`:

```bash
cp .env.example .env               # first time only
echo 'HOUSEHOLD_ID=<an existing household id>' >> .env
docker compose up --build
```

The frontend is then served at `http://localhost:5173` (configurable via
`FRONTEND_PORT`). It is a small nginx container serving the built static
app and same-origin-proxying `/api/**` to the backend over the private
Docker network — the browser only ever talks to one origin, so the backend
needs no CORS configuration and is not given one.

### Running on the host (optional, faster inner loop)

The backend must still run somewhere reachable (for example via
`docker compose up postgres app`). Then, from `frontend/`:

```bash
npm ci
npm run dev
```

`npm run dev` proxies `/api` to `http://localhost:8080` by default
(override with `VITE_BACKEND_URL`). Set the household id for local dev by
editing the committed `frontend/public/config.js` directly — remember to
revert it before committing, since it defaults to an empty household id
(the missing-configuration state). In the built Docker image this file is
regenerated at container startup from `HOUSEHOLD_ID` instead (see
`frontend/docker/docker-entrypoint.sh`).

### Frontend tests

`./verify.sh` (and the required CI `verify` check) runs the frontend's
clean install, type check, test suite (Vitest + React Testing Library —
money-precision, multi-currency, empty/error/refresh-failure states, and
request-race safety), and production build, alongside the full backend
suite. Node's version is pinned in `frontend/.nvmrc` /
`frontend/package.json#engines`; `frontend/package-lock.json` is committed.

### Frontend E2E tests

`frontend/e2e/` holds a real-Chromium Playwright suite (`npm run test:e2e`
from `frontend/`, or `npx playwright test`), covering the same states
through an actual browser rather than jsdom, plus keyboard-focus and
200%-zoom-reflow-simulation checks. `frontend/e2e/evidence.spec.ts` captures
the wide/narrow screenshots under
`agent/ui/financial-position/evidence/implementation/`. This suite runs
against mocked API routes and is not part of the required `verify` CI gate
(no browser/Docker provisioning added there for it) — run it locally when
touching the frontend UI.

`frontend/e2e/real-backend-smoke.sh` is the one real-backend flow required
by WAP-15's acceptance criteria: it builds and starts an **isolated,
disposable** copy of the full stack (`docker-compose.test.yml`, an anonymous
Postgres volume and separate host ports driven all the way through both
compose files — never the shared `waypoint-postgres-data` volume or the
ordinary dev ports, so it can run alongside an already-running ordinary
stack), seeds one synthetic household with mixed-currency assets/liabilities
through the real REST API, then starts the frontend configured for that
exact household (the ordinary `docker-entrypoint.sh` runtime-config
contract) and exercises it two ways: a `curl` check of the raw JSON through
the real nginx-to-Spring proxy, and a real-Chromium Playwright check
(`frontend/e2e/real-backend-smoke.spec.ts`, run under
`frontend/playwright.smoke.config.ts`) that renders the actual React
dashboard against that same real path and drives a refresh through it. Tears
the isolated stack down (`down -v`) afterward. Run it from the repository
root:

```bash
./frontend/e2e/real-backend-smoke.sh
```

Requires `docker`, `curl`, `jq`, and the frontend's npm dependencies and
Playwright browsers installed (`npm ci && npx playwright install chromium`
in `frontend/`).

## Continuous integration

Every pull request targeting `main` runs `./verify.sh` in GitHub Actions
(`.github/workflows/verify.yml`) as the required `verify` status check.
`main` branch protection requires that check to pass before merge (the
settings read-back evidence lives in git history, not a checked-in log).
See `agent/workflow/implement.md`, `agent/workflow/review.md`, and
`agent/workflow/ship.md` for the stage-specific branch, review, and merge
gates.

## Status

Phase 1 (Household and Person foundation), Phase 2 (Asset and Liability
records), and Phase 3 (Income and Recurring Obligations) are implemented and
accepted. Task 003 (automated delivery gates: `verify.sh`, the required CI
check, and branch protection) is implemented and accepted. Phase 4
(Financial Position Snapshots) is implemented and pending Product Owner
acceptance. Phase 5 (Household Financial Goals) is implemented and pending
Product Owner acceptance. See `docs/product/roadmap.md` and the project's
Jira board for current state; what to work on next is a user decision, not
something to infer from prior history.
