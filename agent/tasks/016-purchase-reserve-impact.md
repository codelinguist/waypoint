---
status: QUEUED
task_number: 016
feature_slug: purchase-reserve-impact
branch: task/016-purchase-reserve-impact
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 016: Purchase Impact on Cash Reserves

## Goal

See how a proposed cash purchase changes reserve coverage against an explicitly chosen reserve floor.

## Outcome

A documented backend-only operation: `POST /api/scenarios/purchase-reserve-impact`.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/purchase-reserve-impact/product-brief.md).
- Add domain behavior, typed HTTP contracts, focused tests, API examples and feature-local implementation evidence.
- Exercise synthetic primary and edge flows; run `./verify.sh` and record results.

## Constraints

- Exclusive ownership: `backend/src/main/java/com/waypoint/scenarios/purchasereserve/**`, `backend/src/test/java/com/waypoint/scenarios/purchasereserve/**`, and `agent/product/purchase-reserve-impact/**`.
- Dependencies: Consume merged com.waypoint.planning.runway classes read-only. Do not change their APIs or duplicate their runway arithmetic. No dependency on Tasks 012–014 or 015/017–019.
- No persistence, migrations, frontend, shared handlers/frameworks, build/configuration changes or shared-prose edits. Record implementation and system-evolution findings in the feature-local log for post-batch consolidation.
- Do not invent household data or financial policy. Calculation inputs remain assumptions; results cannot silently approve or persist a financial decision.
- Work in isolated branch/worktree `task/016-purchase-reserve-impact`. Task lifecycle fields belong to the orchestrator after queueing; do not hand-edit them.

## Definition of Done

- Every linked acceptance criterion has recorded evidence; scope and ownership are respected.
- `./verify.sh` passes and valid/invalid API behavior is manually exercised on isolated synthetic data.
- Open the task PR with the task, brief and verification links. Independent Product Owner acceptance and green required `verify` are necessary before merge.
