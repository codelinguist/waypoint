---
status: QUEUED
task_number: 017
feature_slug: debt-prepayment-comparison
branch: task/017-debt-prepayment-comparison
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 017: Debt Prepayment Comparison

## Goal

Compare an explicit immediate principal prepayment with continuing the same fixed monthly debt payment.

## Outcome

A documented backend-only operation: `POST /api/scenarios/debt-prepayment`.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/debt-prepayment-comparison/product-brief.md).
- Add domain behavior, typed HTTP contracts, focused tests, API examples and feature-local implementation evidence.
- Exercise synthetic primary and edge flows; run `./verify.sh` and record results.

## Constraints

- Exclusive ownership: `backend/src/main/java/com/waypoint/scenarios/debtprepayment/**`, `backend/src/test/java/com/waypoint/scenarios/debtprepayment/**`, and `agent/product/debt-prepayment-comparison/**`.
- Dependencies: Consume merged com.waypoint.planning.debtamortization classes read-only. No edits to existing calculator, no annual-rate conversion and no dependency on current workers or batch siblings.
- No persistence, migrations, frontend, shared handlers/frameworks, build/configuration changes or shared-prose edits. Record implementation and system-evolution findings in the feature-local log for post-batch consolidation.
- Do not invent household data or financial policy. Calculation inputs remain assumptions; results cannot silently approve or persist a financial decision.
- Work in isolated branch/worktree `task/017-debt-prepayment-comparison`. Task lifecycle fields belong to the orchestrator after queueing; do not hand-edit them.

## Definition of Done

- Every linked acceptance criterion has recorded evidence; scope and ownership are respected.
- `./verify.sh` passes and valid/invalid API behavior is manually exercised on isolated synthetic data.
- Open the task PR with the task, brief and verification links. Independent Product Owner acceptance and green required `verify` are necessary before merge.
