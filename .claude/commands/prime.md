---
description: Load the minimum authoritative context for the current Waypoint stage
---

# Prime: Load Waypoint Context

## Objective

Build enough working understanding for the current stage without loading the
whole product corpus by default.

## Process

### 1. Establish the stage and task

Read `AGENTS.md` and the applicable stage in
`agent/collaboration-workflow.md`. Load context only; do not start a stage merely
because the Jira issue is ready.

Read the assigned Jira issue description in full, plus comments from the
   current round (since its last status change) in full. For an issue with
   several closed Review/Revise rounds behind it, skim earlier comments for
   their outcome only — read one in full only if this session specifically
   needs the reasoning behind an earlier finding. If you weren't pointed at a
   specific issue, ask which one this session is for; pre-existing work may
   instead point at a legacy `agent/tasks/*.md` file.

### 2. Load relevant durable context

Scan the index at the top of `docs/decisions/decisions.md` and read in full only
decisions whose trigger matches the issue or affected code. Open product,
domain, and architecture documents only when the issue, selected decisions, or
a concrete implementation question depends on them. The Frame stage is the
exception and loads the full set specified by `AGENTS.md`.

If the issue names a feature slug and a design stage happened, also read
`agent/ui/<feature-slug>/design-brief.md`. (For pre-existing work still
tracked by a legacy task file, read its linked
`agent/product/<feature-slug>/product-brief.md` instead.)

### 3. Check recent history

!`git log -5 --oneline`

!`git status`

`agent/implementation-log.md` was retired and later removed outright (its
history remains in git history if ever needed) because a running log is
pure token-cost overhead — nothing to read there, and do not recreate it,
root or feature-local.

### 4. Orient to affected code

Inspect the files and contracts named by the issue, then follow direct call,
type, persistence, and test dependencies as needed. Inspect build configuration
only when the task changes dependencies or build behavior. Confirm
`./verify.sh` is present; it remains the canonical verification command.

## Output Report

Report back concisely — headers and short bullets, not prose paragraphs:

### Current task
- Jira issue key, feature slug, and one-line goal from the issue.
- Which stage this session is starting in: Implement, Revise, or Ship.

### Product context
- The problem and desired outcome this task serves, from the Jira issue and
  `docs/product/*`.

### Domain and architecture constraints
- Rules from `docs/domain/financial-model.md` and
  `docs/architecture/architecture.md` that bear directly on this task.

### Recent activity
- What the last few commits say changed, and anything they flagged as
  assumed or unresolved.

### Open questions
- Anything ambiguous enough to flag to the user before proceeding, per
  `agent/collaboration-workflow.md` — Claude Code has no direct channel to
  Codex, so the user decides whether it needs a trip back to Codex.
