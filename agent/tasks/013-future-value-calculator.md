---
status: QUEUED
task_number: 013
feature_slug: future-value-calculator
branch: task/013-future-value-calculator
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 013: Future-Value Calculator

## Goal

Add the missing deterministic compound-growth primitive from Phase 7.

## Outcome

A trusted caller can project explicit principal and equal monthly contributions under an explicit nominal annual return assumption with auditable monthly reconciliation.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/future-value-calculator/product-brief.md).
- Add pure domain logic, typed HTTP validation, `POST /api/planning/future-value`, focused tests, and feature-local API/evidence documentation.

## Constraints

- Exclusive code ownership: `backend/src/main/java/com/waypoint/planning/futurevalue/**` and `backend/src/test/java/com/waypoint/planning/futurevalue/**`.
- Exclusive prose ownership: `agent/product/future-value-calculator/**`; lifecycle fields in this task file belong to the orchestrator after queueing.
- No migration, persistence, entity access, sibling import, shared handler/framework, build/configuration, README, roadmap, central-log, workflow, or template edits.
- Inputs are temporary assumptions. Do not infer returns or present results as forecasts, guarantees, recommendations, or approved decisions.

## Definition of Done

- Every acceptance criterion has evidence and the diff respects exclusive ownership.
- `./verify.sh` passes and the synthetic primary flow plus edge cases are exercised.
- Open the task PR; Product Owner acceptance and green required `verify` are required before automatic merge.

