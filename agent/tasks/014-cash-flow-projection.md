---
status: MERGED
task_number: 014
feature_slug: cash-flow-projection
branch: task/014-cash-flow-projection
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-014-cash-flow-projection
session: 921b5290
pr: 21
claimed_at: 2026-09-05T18:05:53Z
fix_rounds: 0
conflict_rounds: 0
---

# Task 014: Constant Monthly Cash-Flow Projection

## Goal

Add the missing deterministic cash-flow projection primitive from Phase 7.

## Outcome

A trusted caller can inspect a dated, reconciled monthly cash path and identify the first negative and lowest balance from explicit temporary inputs.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/cash-flow-projection/product-brief.md).
- Add pure domain logic, typed HTTP validation, `POST /api/planning/cash-flow-projection`, focused tests, and feature-local API/evidence documentation.

## Constraints

- Exclusive code ownership: `backend/src/main/java/com/waypoint/planning/cashflow/**` and `backend/src/test/java/com/waypoint/planning/cashflow/**`.
- Exclusive prose ownership: `agent/product/cash-flow-projection/**`; lifecycle fields in this task file belong to the orchestrator after queueing.
- No migration, persistence, entity access, sibling import, shared handler/framework, build/configuration, README, roadmap, central-log, workflow, or template edits.
- Inputs are temporary assumptions. Do not infer household cash flow or present the projection as a forecast, recommendation, or approved decision.

## Definition of Done

- Every acceptance criterion has evidence and the diff respects exclusive ownership.
- `./verify.sh` passes and synthetic primary/edge flows are exercised.
- Open the task PR; Product Owner acceptance and green required `verify` are required before automatic merge.

