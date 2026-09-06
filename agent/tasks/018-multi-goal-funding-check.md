---
status: QUEUED
task_number: 018
feature_slug: multi-goal-funding-check
branch: task/018-multi-goal-funding-check
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 018: Multiple Goal Funding Check

## Goal

See whether several explicitly modeled saving goals fit one monthly saving budget without choosing household priorities.

## Outcome

A documented backend-only operation: `POST /api/planning/multi-goal-funding-check`.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/multi-goal-funding-check/product-brief.md).
- Add domain behavior, typed HTTP contracts, focused tests, API examples and feature-local implementation evidence.
- Exercise synthetic primary and edge flows; run `./verify.sh` and record results.

## Constraints

- Exclusive ownership: `backend/src/main/java/com/waypoint/planning/multigoalfunding/**`, `backend/src/test/java/com/waypoint/planning/multigoalfunding/**`, and `agent/product/multi-goal-funding-check/**`.
- Dependencies: Consume merged com.waypoint.planning.goalcontribution classes read-only; no canonical Goals lookup or dependency on Tasks 012–017/019.
- No persistence, migrations, frontend, shared handlers/frameworks, build/configuration changes or shared-prose edits. Record implementation and system-evolution findings in the feature-local log for post-batch consolidation.
- Do not invent household data or financial policy. Calculation inputs remain assumptions; results cannot silently approve or persist a financial decision.
- Work in isolated branch/worktree `task/018-multi-goal-funding-check`. Task lifecycle fields belong to the orchestrator after queueing; do not hand-edit them.

## Definition of Done

- Every linked acceptance criterion has recorded evidence; scope and ownership are respected.
- `./verify.sh` passes and valid/invalid API behavior is manually exercised on isolated synthetic data.
- Open the task PR with the task, brief and verification links. Independent Product Owner acceptance and green required `verify` are necessary before merge.
