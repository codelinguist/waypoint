---
status: QUEUED
task_number: 020
feature_slug: financial-position-design
branch: task/020-financial-position-design
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 020: Explore and Approve Financial Position Design

## Goal and outcome

Produce an approved, evidence-backed design for the first financial-position experience, ready for implementation in a fresh conversation.

## Required deliverables

Implement the [design-only product brief](../product/financial-position-design/product-brief.md) against the [parent scope](../product/financial-position/product-brief.md): two or three distinct directions, wide/narrow evidence, concrete states/accessibility, and a recorded Codex Product Owner selection.

## Constraints

- Own only agent/product/financial-position-design/** and agent/ui/financial-position/**. Use synthetic evidence; no production code or shared-file edits.
- Start design DRAFT, invoke Product Owner approval through checked-in artifacts, and obtain APPROVED before handoff.
- Independent of Tasks 012–019 and Task 021 implementation. Use the specified API contract for design; do not wait for its code.
- Follow the isolated branch/worktree and PR workflow. Lifecycle state belongs to the orchestrator.

## Definition of Done

Every design-brief acceptance criterion has evidence; ./verify.sh passes and required CI verify is green. Record feature-local implementation log and independent design-task acceptance. Do not begin production UI coding in this conversation.
