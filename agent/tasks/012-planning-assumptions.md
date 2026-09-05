---
status: IN_PROGRESS
task_number: 012
feature_slug: planning-assumptions
branch: task/012-planning-assumptions
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-012-planning-assumptions
session: dc5841c6
pr:
claimed_at: 2026-09-05T18:05:42Z
fix_rounds: 0
conflict_rounds: 0
---

# Task 012: Planning Assumptions Registry

## Goal

Add the missing Phase 6 foundation for explicit, provenance-bearing, versioned planning assumptions without creating a competing generic Fact datastore.

## Outcome

A trusted caller can create, retrieve, list, and supersede household assumptions while preserving immutable history and keeping them visibly distinct from canonical facts.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/planning-assumptions/product-brief.md).
- Add the assumption aggregate, migration, service/domain behavior, REST surface, and focused unit/PostgreSQL integration tests.
- Record commands, evidence, assumptions, and limitations in the feature-local implementation log.

## Constraints

- Exclusive code ownership: `backend/src/main/java/com/waypoint/assumption/**`, `backend/src/test/java/com/waypoint/assumption/**`, and `backend/src/main/resources/db/migration/V6__create_planning_assumptions.sql`.
- Exclusive prose ownership: `agent/product/planning-assumptions/**`; lifecycle fields in this task file belong to the orchestrator after queueing.
- Existing household types may be consumed read-only. Do not edit existing aggregates, shared exception handling, application/build configuration, README, roadmap, central implementation log, workflow docs, or sibling paths.
- Do not add a generic Fact table, infer household values, or promote assumptions to facts, decisions, or canonical financial state.

## Definition of Done

- Every acceptance criterion has recorded evidence and the diff respects exclusive ownership.
- `./verify.sh` passes and synthetic create/query/supersede/history flows are exercised.
- The branch opens a PR; Product Owner acceptance and green required `verify` are required before automatic merge.

