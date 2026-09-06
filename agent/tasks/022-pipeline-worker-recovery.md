---
status: IN_REVIEW
task_number: 022
feature_slug: pipeline-worker-recovery
branch: task/022-pipeline-worker-recovery
worktree: /private/tmp/waypoint-retry-recovery
session:
pr:
claimed_at: 2026-09-06T04:30:00Z
fix_rounds: 0
conflict_rounds: 0
---

# Task 022: Recover stalled pipeline workers

User-authorized interactive operational recovery, implemented in an isolated
worktree. See agent/product/pipeline-worker-recovery/product-brief.md.

Scope: automation ownership/retry/auth guards, worker prompt consistency,
regression tests, scheduler installation, and recovery of tasks 012/013.
Do not edit feature implementation code or household financial data.

Ship the pipeline patch through a PR with passing verify and independent
acceptance, then complete and record operational recovery.
