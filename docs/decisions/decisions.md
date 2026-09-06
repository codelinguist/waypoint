# Product and Architecture Decisions

## D001 — Structured financial state is canonical

**Status:** Accepted

Conversation history or LLM memory must not be the canonical source of household financial data.

**Reason:** Long-running planning requires explicit, auditable, correctable state.

---

## D002 — The LLM is not the calculation engine

**Status:** Accepted

Important financial calculations must be deterministic code with tests.

**Reason:** Financial arithmetic should be reproducible and auditable.

---

## D003 — Facts and assumptions are distinct domain concepts

**Status:** Accepted

A known mortgage balance and an estimated future salary must not be represented identically.

**Reason:** Plans require explicit uncertainty.

---

## D004 — Recommendation and decision are distinct

**Status:** Accepted

AI may recommend an action, but the application should only treat it as household policy after explicit approval.

**Reason:** Prevent silent AI authority over material financial decisions.

---

## D005 — Scenario state is non-destructive

**Status:** Accepted

Running a scenario must not modify canonical household state.

**Reason:** Users need to explore alternatives safely.

---

## D006 — Initial product is private and household-specific

**Status:** Accepted

Build first for Ralph and his wife.

Do not prematurely generalize for SaaS, multi-tenancy, regulatory investment advice, or broad internationalization.

**Reason:** Dogfooding should drive product discovery.

---

## D007 — Modular monolith first

**Status:** Accepted

Start with one backend application and one relational database.

**Reason:** Current complexity does not justify distributed architecture.

---

## D008 — Python is the initial backend language

**Status:** Superseded by D011 and D012

Use Python/FastAPI for the first backend.

**Reason:** The project is also intended to deepen practical AI engineering skills while preserving deterministic backend architecture.

---

## D009 — PostgreSQL is the initial database

**Status:** Accepted

Use PostgreSQL as canonical persistence.

**Reason:** Strong relational model, mature tooling, and future compatibility with vector extensions if they become useful.

---

## D010 — Preserve historical plans and snapshots

**Status:** Accepted

Financial plans should be versioned rather than overwritten.

**Reason:** The product should eventually answer questions such as:

> How are we doing compared with the plan we made in 2026?

---

## D011 — Java owns the core application and REST API

**Status:** Accepted

Use a Java/Spring Boot modular monolith for the public REST API, canonical
financial state, transactional workflows, and ordinary deterministic financial
calculations.

**Reason:** The core of the product is a long-lived, strongly typed financial
domain with explicit invariants, persistence, and auditable state transitions.
Java is a strong fit for that center of gravity. Keeping these responsibilities
in one deployable application also preserves D007.

**Tradeoff:** AI and numerical libraries are often more readily available in
Python. A Java core therefore needs an explicit integration boundary when a
future feature genuinely requires that ecosystem.

---

## D012 — Python is reserved for specialized analytical capabilities

**Status:** Accepted

Add a Python service only when a concrete forecasting, optimization,
machine-learning, NLP, or similar feature has a meaningful dependency on the
Python ecosystem.

The service must:

- accept and return versioned, structured contracts
- receive only the data required for the calculation
- never write canonical application tables
- return results with model/calculation version and relevant warnings
- remain replaceable from the perspective of the core application

Straightforward deterministic financial calculations remain in the Java
monolith unless evidence supports moving them.

**Reason:** This captures the benefits of Python where they are strongest
without paying the operational and consistency costs of polyglot services
before a real feature requires them.

---

## D013 — Docker Compose is the standard local development environment

**Status:** Accepted

Use Docker Compose as the primary way to run the application locally. The
initial topology contains the Spring Boot application and PostgreSQL, with
persistent database storage and readiness checks.

Running tests or the application directly on the host remains supported when
useful for a faster development loop, but a clean checkout must be runnable
with Docker as its only runtime prerequisite.

**Reason:** A containerized local environment gives the household project a
repeatable application/database setup and reduces machine-specific Java and
PostgreSQL configuration while keeping the modular-monolith boundary intact.

**Tradeoff:** Docker adds build time and container-specific configuration, so
the Compose setup should stay minimal and must not be treated as production
orchestration.

---

## D014 — CI is a required merge gate on `main`, driven by one canonical command

**Status:** Accepted

Every pull request targeting `main` runs the root-level `./verify.sh`
command — the complete Java 21 Maven suite, including the
PostgreSQL/Testcontainers integration tests — via a least-privilege GitHub
Actions workflow. `main` branch protection requires that check (`verify`)
before merge. Local agents run the same script before opening or updating a
PR, so there is exactly one definition of "green," not separate local and CI
notions of passing.

Because Claude Code and Codex currently authenticate to GitHub as the same
account, GitHub cannot provide an independent formal approving review; the
product brief's recorded findings and acceptance remain the durable approval
record, and are not represented as a GitHub-native approval.

**Reason:** PR #1 demonstrated that review quality alone is not a safety net
when the verification command is undocumented and ambiguous and no check is
required at the repository boundary — deterministic financial software needs
an objective, reproducible gate that neither implementation nor review prose
can override.

**Tradeoff:** A required check can deadlock merging if its definition breaks;
recovering from that is a deliberate, explicitly-decided administrator action,
not a routine bypass.


---

## D015 — Bring forward the first financial-position frontend slice

**Status:** Accepted

Following Ralph's explicit request on 2026-09-06, begin the read-only financial-position
slice of Phase 9 before completing the full Phase 8 scenario engine. Explore and
approve the design separately, add the missing coherent current-position read model,
and queue UI implementation only after both prerequisites are accepted and merged.

Use the existing asset planning-value convention for net worth, calculated in Java
per currency. The new read model returns source rows and totals together with exact
decimal-string monetary fields for browser precision; existing API contracts remain
unchanged. Retrieval time does not turn dated records into current confirmed values.

**Reason:** Existing financial records support a useful household experience now.
A narrow UI feedback loop can guide subsequent development without waiting for every
planning capability. Read-only delivery preserves household authority.

**Tradeoff:** The first page has no data-entry flow, requires an existing configured
household, and does not include broader cash-flow, goals or scenario views. Design
approval, independent acceptance and the required verification gate remain mandatory.
