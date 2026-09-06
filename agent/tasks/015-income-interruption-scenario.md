---
status: QUEUED
task_number: 015
feature_slug: income-interruption-scenario
branch: task/015-income-interruption-scenario
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 015: Income Interruption Scenario

## Goal

Determine the reserve needed to withstand a caller-defined temporary income interruption.

## Outcome

A documented backend-only operation: `POST /api/scenarios/income-interruption`.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/income-interruption-scenario/product-brief.md).
- Add domain behavior, typed HTTP contracts, focused tests, API examples and feature-local implementation evidence.
- Exercise synthetic primary and edge flows; run `./verify.sh` and record results.

## Constraints

- Exclusive ownership: `backend/src/main/java/com/waypoint/scenarios/incomeinterruption/**`, `backend/src/test/java/com/waypoint/scenarios/incomeinterruption/**`, and `agent/product/income-interruption-scenario/**`.
- Dependencies: Tasks 012–014 and all batch siblings are unnecessary. This is a bounded variable-income scenario, not a second constant-input projection. No imports from unfinished cash-flow work.
- No persistence, migrations, frontend, shared handlers/frameworks, build/configuration changes or shared-prose edits. Record implementation and system-evolution findings in the feature-local log for post-batch consolidation.
- Do not invent household data or financial policy. Calculation inputs remain assumptions; results cannot silently approve or persist a financial decision.
- Work in isolated branch/worktree `task/015-income-interruption-scenario`. Task lifecycle fields belong to the orchestrator after queueing; do not hand-edit them.

## Definition of Done

- Every linked acceptance criterion has recorded evidence; scope and ownership are respected.
- `./verify.sh` passes and valid/invalid API behavior is manually exercised on isolated synthetic data.
- Open the task PR with the task, brief and verification links. Independent Product Owner acceptance and green required `verify` are necessary before merge.
