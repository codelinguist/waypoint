# Task queue

This directory replaces the old single `agent/current-task.md` file. Instead
of one active task at a time, it holds a small backlog: one file per task,
`agent/tasks/<NNN>-<feature-slug>.md`, each an independent execution contract
that `agent/automation/orchestrator.sh` claims and dispatches — usually to a
Claude Code worker running unattended in its own git worktree — without a
human driving each step.

Up to **3** tasks may be `IN_PROGRESS` at once (see
`agent/collaboration-workflow.md` -> "Automated pipeline" for why that bound
exists). Anything beyond that limit waits in `QUEUED`.

## File format

```markdown
---
status: QUEUED
task_number: 006
feature_slug: some-feature
branch: task/006-some-feature
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
---

<the same body content agent/current-task.md used to hold: Goal, Outcome,
Required deliverables, Constraints, Definition of Done, linking to
agent/product/<feature_slug>/product-brief.md for full acceptance criteria>
```

## Status lifecycle

This file only tracks orchestrator-owned lifecycle state. Codex's own
`ACCEPTED`/findings verdict lives in the product brief
(`agent/product/<feature_slug>/product-brief.md`), not here — the
orchestrator reads that verdict from its own run state, not from this file.

- `QUEUED` — written by Codex (`frame` mode), not yet claimed.
- `IN_PROGRESS` — a Claude Code worker is implementing it, either the first
  pass or an automatic fix round (`fix_rounds` counts which); `worktree`,
  `session`, and `branch` are filled in.
- `IN_REVIEW` — the worker pushed the branch and opened `pr`; waiting on the
  automated Codex review. A `BLOCKING` verdict here sends the task straight
  back to `IN_PROGRESS` for another fix round, up to `fix_rounds`' bound —
  it does not rest in a separate "returned" state.
- `STALLED` — fix rounds were exhausted, or something the orchestrator can't
  safely resolve on its own (e.g. a Flyway migration-version collision
  against `main`). Needs a human or an interactive Claude Code / Codex
  session; read the product brief's review findings for why.
- `MERGED` — merged to `main` the moment Codex's review records acceptance
  and the required `verify` check is green; worktree removed. Terminal
  state; the file stays as a durable record alongside
  `agent/implementation-log.md`.

## Who writes what

- Codex (`frame` mode, or the user chatting with Codex about the product)
  creates new `QUEUED` files here.
- `agent/automation/orchestrator.sh` owns every other status transition,
  `worktree`, `session`, `pr`, `claimed_at`, and `fix_rounds` field. Nothing
  else should edit those directly.
- A `STALLED` file can always be picked up by hand instead — read it, and
  the product brief's recorded findings, like the old
  `agent/current-task.md`, per `AGENTS.md`.
