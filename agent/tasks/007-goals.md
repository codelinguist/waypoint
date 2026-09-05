---
status: IN_REVIEW
task_number: 007
feature_slug: goals
branch: task/007-goals
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-007-goals
session:
pr:
claimed_at: 2026-09-05T11:08:28Z
fix_rounds: 0
---

# Goal

Add the smallest durable household Goals domain: monetary goals with target amount, target date, priority, explicit current amount, and deterministic progress.

# Outcome

A trusted private API caller can create and retrieve household-scoped goals and see auditable progress without changing other canonical financial records.

# Required deliverables

- Implement the backend-only feature described in [the product brief](../product/goals/product-brief.md).
- Add the goal schema/entity, domain/service logic, API surface, and focused tests.
- Keep progress arithmetic deterministic and outside transport concerns.
- Enforce household ownership and established validation/not-found behavior.
- Run `./verify.sh` and record implementation evidence in the product brief and implementation log.

# Constraints

- Do not modify assets, liabilities, income, obligations, snapshots, or plan-versus-actual behavior.
- Do not add goal updates/deletion, contributions, forecasts, scenarios, frontend, AI behavior, or inferred progress.
- Keep this task independent from Task 008: use only goal-specific entities/files and a goal-specific migration; do not edit Task 008 files or add shared planning entities.
- Do not invent household financial values or silently alter canonical data.

# Definition of Done

- The product brief's acceptance criteria are satisfied.
- Monetary goal validation, persistence, household isolation, retrieval, and progress calculations are covered by automated tests.
- `./verify.sh` passes.
- The implementation log and product brief contain verification evidence, deviations, and unresolved follow-up questions.
