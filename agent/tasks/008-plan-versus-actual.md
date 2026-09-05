---
status: IN_REVIEW
task_number: 008
feature_slug: plan-versus-actual
branch: task/008-plan-versus-actual
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-008-plan-versus-actual
session: caa97e54
pr:
claimed_at: 2026-09-05T11:08:33Z
fix_rounds: 0
---

# Goal

Add a read-only plan-versus-actual analysis that compares explicit planned snapshot measures with one persisted financial snapshot.

# Outcome

A trusted private API caller can inspect signed actual-minus-plan variances for a selected snapshot without persisting a plan, changing the snapshot, or depending on Goals.

# Required deliverables

- Implement the backend-only feature described in [the product brief](../product/plan-versus-actual/product-brief.md).
- Add request/domain/response logic and focused unit and API/integration tests.
- Keep decimal arithmetic deterministic and outside transport concerns.
- Enforce household and snapshot ownership plus established structured validation behavior.
- Run `./verify.sh` and record implementation evidence in the product brief and implementation log.

# Constraints

- Do not add a migration, persistent planning entity, or stored analysis result.
- Do not modify or depend on Goals, target dates, progress, contributions, forecasts, scenarios, or AI behavior.
- Keep this task independent from Task 007: use analysis/snapshot-specific files only and do not edit goal entities, controllers, repositories, tests, or migrations.
- Do not infer plan values, label variances as favorable/unfavorable, or change canonical financial data.

# Definition of Done

- The product brief's acceptance criteria are satisfied.
- Arithmetic, sign/direction, validation, household isolation, and no-mutation behavior are covered by automated tests.
- `./verify.sh` passes.
- The implementation log and product brief contain verification evidence, deviations, and unresolved follow-up questions.
