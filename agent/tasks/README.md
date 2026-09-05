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
conflict_rounds: 0
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
- `IN_PROGRESS` — a Claude Code worker is implementing it: the first pass, an
  automatic fix round (`fix_rounds` counts which), or an automatic rebase
  round (`conflict_rounds` counts which — a separate budget, see below);
  `worktree`, `session`, and `branch` are filled in.
- `IN_REVIEW` — the worker pushed the branch and opened `pr`; waiting on the
  automated Codex review. A `BLOCKING` verdict sends the task back to
  `IN_PROGRESS` for another automatic fix round, up to `fix_rounds`' bound.
  The PR going stale against `main` (a sibling task merged and now conflicts)
  is a distinct failure with its own `conflict_rounds` bound, so a run of bad
  luck on one never eats the other's budget. Either path returns the task
  straight to `IN_PROGRESS` — it does not rest in a separate "returned"
  state. A review that fails for infrastructure reasons (Codex's own
  invocation crashed, produced no output, or returned an unparseable
  verdict) is `REVIEW_ERROR`, not `BLOCKING`: it consumes neither budget,
  dispatches no worker, and never authorizes a merge — the task simply gets
  reviewed again next run.
- `STALLED` — automatic rounds (of either kind) were exhausted, or something
  the orchestrator can't safely resolve on its own (e.g. a Flyway
  migration-version collision against `main`, which always needs a human
  rather than an automatic rebase). Needs a human or an interactive Claude
  Code / Codex session; read the product brief's review findings for why.
- `MERGED` — merged to `main` the moment Codex's review records acceptance
  and the required `verify` check is green; worktree removed. Terminal
  state; the file stays as a durable record alongside
  `agent/implementation-log.md`.

## Who writes what

- Codex (`frame` mode, or the user chatting with Codex about the product)
  creates new `QUEUED` files here.
- `agent/automation/orchestrator.sh` owns every other status transition,
  `worktree`, `session`, `pr`, `claimed_at`, `fix_rounds`, and
  `conflict_rounds`. Nothing else should edit those directly.
- A `STALLED` file can always be picked up by hand instead — read it, and
  the product brief's recorded findings, like the old
  `agent/current-task.md`, per `AGENTS.md`.
