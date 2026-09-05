---
status: IN_REVIEW
task_number: 006
feature_slug: snapshot-comparison
branch: task/006-snapshot-comparison
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-006-snapshot-comparison
session: c866f364
pr:
claimed_at: 2026-09-05T10:05:57Z
fix_rounds: 0
---

# Goal

Make persisted financial snapshots useful for historical review by adding a deterministic, read-only comparison of two snapshots from the same household.

# Outcome

A trusted private API caller can identify an earlier and later snapshot and receive signed later-minus-earlier deltas for all supported snapshot measures, with clear source metadata and established error behavior for invalid requests.

# Required deliverables

- Implement the backend-only comparison operation described in [the product brief](../product/snapshot-comparison/product-brief.md).
- Keep comparison arithmetic deterministic and separate from transport concerns.
- Enforce household ownership and explicit earlier/later direction.
- Add automated tests for arithmetic, validation/not-found behavior, household isolation, and no mutation.
- Run `./verify.sh` and record implementation evidence in the product brief and implementation log.

# Constraints

- Do not add a frontend, goals, forecasts, percentage changes, charts, exports, or plan-versus-actual semantics.
- Do not persist comparison results or modify canonical snapshots.
- Follow the existing API’s structured validation and not-found behavior.
- Do not introduce assumptions about household finances or require household approval for this read-only feature.

# Definition of Done

- The product brief’s acceptance criteria are satisfied.
- The comparison is read-only, household-scoped, directionally explicit, and deterministic.
- Automated tests cover valid and invalid paths, including cross-household isolation and identical snapshots.
- `./verify.sh` passes.
- The implementation log and product brief contain the verification result, deviations, and unresolved follow-up questions.
