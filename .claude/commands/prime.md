---
description: Load Waypoint's product, domain, architecture, and current-task context at the start of a session
---

# Prime: Load Waypoint Context

## Objective

Build a working understanding of the household financial planning application
before touching any code or docs, so this conversation starts aligned with
the current task instead of re-deriving context from scratch.

## Process

### 1. Read the required docs, in order

Read each of these in full — this is the exact list `AGENTS.md` requires
before coding:

1. `docs/product/vision.md`
2. `docs/product/user-zero.md`
3. `docs/product/problems.md`
4. `docs/product/principles.md`
5. `docs/domain/financial-model.md`
6. `docs/architecture/architecture.md`
7. `docs/decisions/decisions.md`
8. `docs/product/roadmap.md`
9. Your assigned Jira issue (if you weren't pointed at a specific one, ask
   which issue this session is for; pre-existing work may instead point at a
   legacy `agent/tasks/*.md` file)

### 2. Read collaboration mechanics

Read `AGENTS.md` and `agent/collaboration-workflow.md` if this session
hasn't already — specifically the Plan/Implement/Validate phase definitions
and the user-initiated stage boundaries. Load context only; do not start a
stage merely because a task file is ready.

Read the assigned Jira issue's description and comments for the outcome,
scope, and acceptance criteria. If the issue names a feature slug and a
design stage happened, also read `agent/ui/<feature-slug>/design-brief.md`.
(For pre-existing work still tracked by a legacy task file, read its linked
`agent/product/<feature-slug>/product-brief.md` instead.)

### 3. Check recent history

!`git log -15 --oneline`

!`git status`

Read the tail of `agent/implementation-log.md` for the most recent entries —
what changed, what was assumed, what was left unresolved, and what the
previous pass recommended next.

### 4. Orient to the codebase

!`git ls-files backend/src/main/java | head -60`

Skim `backend/pom.xml` for the dependency set. Confirm `./verify.sh` is
present — it is the only verification command, local or CI.

## Output Report

Report back concisely — headers and short bullets, not prose paragraphs:

### Current task
- Jira issue key, feature slug, and one-line goal from the issue.
- Which Plan/Implement/Validate phase this session is starting in.

### Product context
- The problem and desired outcome this task serves, from the Jira issue and
  `docs/product/*`.

### Domain and architecture constraints
- Rules from `docs/domain/financial-model.md` and
  `docs/architecture/architecture.md` that bear directly on this task.

### Recent activity
- What the last few commits and the implementation log say changed, and
  anything they flagged as assumed or unresolved.

### Open questions
- Anything ambiguous enough to raise with the Product Owner Agent (Codex),
  per `agent/collaboration-workflow.md`, before proceeding.
