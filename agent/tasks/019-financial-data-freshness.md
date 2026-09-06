---
status: QUEUED
task_number: 019
feature_slug: financial-data-freshness
branch: task/019-financial-data-freshness
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 019: Financial Data Freshness Review

## Goal

Identify asset valuations and liability balances needing review using an explicit caller-selected age threshold.

## Outcome

A documented backend-only operation: `GET /api/households/{householdId}/financial-data-freshness?reviewDate=YYYY-MM-DD&maxAgeDays=N`.

## Required deliverables

- Implement every criterion in the [READY product brief](../product/financial-data-freshness/product-brief.md).
- Add domain behavior, typed HTTP contracts, focused tests, API examples and feature-local implementation evidence.
- Exercise synthetic primary and edge flows; run `./verify.sh` and record results.

## Constraints

- Exclusive ownership: `backend/src/main/java/com/waypoint/review/freshness/**`, `backend/src/test/java/com/waypoint/review/freshness/**`, and `agent/product/financial-data-freshness/**`.
- Dependencies: Read existing AssetService and LiabilityService (and existing household validation) without edits. Do not depend on Task 012 assumptions or any new migration.
- No persistence, migrations, frontend, shared handlers/frameworks, build/configuration changes or shared-prose edits. Record implementation and system-evolution findings in the feature-local log for post-batch consolidation.
- Do not invent household data or financial policy. Calculation inputs remain assumptions; results cannot silently approve or persist a financial decision.
- Work in isolated branch/worktree `task/019-financial-data-freshness`. Task lifecycle fields belong to the orchestrator after queueing; do not hand-edit them.

## Definition of Done

- Every linked acceptance criterion has recorded evidence; scope and ownership are respected.
- `./verify.sh` passes and valid/invalid API behavior is manually exercised on isolated synthetic data.
- Open the task PR with the task, brief and verification links. Independent Product Owner acceptance and green required `verify` are necessary before merge.
