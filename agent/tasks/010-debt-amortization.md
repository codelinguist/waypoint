---
status: IN_PROGRESS
task_number: 010
feature_slug: debt-amortization
branch: task/010-debt-amortization
worktree: /Users/rj/Documents/projects/waypoint-orchestrator/worktrees/task-010-debt-amortization
session: 843f72a0
pr:
claimed_at: 2026-09-05T13:43:59Z
fix_rounds: 1
conflict_rounds: 0
---

# Task 010: Fixed-Payment Debt Amortization

## Goal

See the payoff duration and total interest implied by an explicit balance, constant monthly interest rate, and fixed monthly payment.

## Outcome

A documented, stateless backend calculation with deterministic decimal results from explicit temporary inputs, independent of Tasks 009–011 siblings.

## Required deliverables

- Implement the READY [product brief](../product/debt-amortization/product-brief.md), including every acceptance criterion.
- Add pure domain calculation, typed validation, POST `/api/planning/debt-amortization`, dedicated tests and feature-local `api.md` examples.
- Record verification, manual API evidence and the full implementation record in this feature's brief or linked feature-local implementation log.

## Constraints

- Exclusive code ownership: `backend/src/main/java/com/waypoint/planning/debtamortization/**` and `backend/src/test/java/com/waypoint/planning/debtamortization/**`.
- Exclusive prose/evidence ownership: `agent/product/debt-amortization/**`. Follow the brief's explicit shared-prose exception: defer central log, README and decision-log updates to a post-batch consolidation; do not edit shared files.
- No migrations, persistence, entity access/change, sibling imports, shared framework, dependencies/build changes, shared exception-handler edits or frontend.
- Inputs are caller-supplied assumptions for this calculation only. Do not infer household facts, financial priorities, or approvals. Stop for a material missing household choice.
- Start implementation in a fresh conversation and isolated branch/worktree `task/010-debt-amortization`. The frontmatter slug and branch are deliberately blank per the user's queue-writing instruction; the filename and this body identify them unambiguously. Lifecycle fields belong to the orchestrator after queueing.

## Definition of Done

- Every linked acceptance criterion has evidence; the diff stays inside the ownership boundaries and works without sibling outputs.
- `./verify.sh` passes, the primary API flow is manually exercised using synthetic data and isolated test infrastructure, and results/limitations are recorded locally to this feature.
- Open a task PR with evidence and links under the collaboration workflow. Product Owner acceptance and a green required verify check are required before merge.
