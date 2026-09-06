---
status: QUEUED
task_number: 021
feature_slug: current-financial-position
branch: task/021-current-financial-position
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 021: Current Financial Position API

## Goal and outcome

Deliver a read-only current-position response with exact server-calculated per-currency net worth and the records explaining it.

## Required deliverables

Implement every criterion and the exact monetary contract in the [product brief](../product/current-financial-position/product-brief.md). Add domain/API/integration tests, api.md examples and feature-local implementation-log.md. Exercise synthetic API flows and run ./verify.sh.

## Constraints

- Own backend/src/main/java/com/waypoint/position/**, matching tests and agent/product/current-financial-position/** only. Read existing household types without edits.
- No migration, canonical write, snapshot creation, frontend, shared handler/build/configuration or shared-prose edits.
- Independent of all other queued/in-progress tasks. Use its isolated task branch/worktree; orchestrator owns lifecycle state.

## Definition of Done

All acceptance criteria have evidence; exact decimal-string transport and coherent totals are verified, ./verify.sh passes, and the task PR receives Product Owner acceptance plus green required verify. Hand off the documented API to the UI task; do not implement that UI here.
