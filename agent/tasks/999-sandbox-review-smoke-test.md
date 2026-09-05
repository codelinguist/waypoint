---
status: IN_REVIEW
task_number: 999
feature_slug: sandbox-review-smoke-test
branch: test/sandbox-review-smoke
worktree:
session:
pr:
claimed_at: 2026-09-06T00:00:00Z
fix_rounds: 0
conflict_rounds: 0
---

# Sandbox review smoke test

## Goal

Verify that the sandboxed Codex review can inspect a real PR, update its product
brief, commit, and push without merging the disposable change.

## Definition of Done

- Codex reads the PR diff and this task contract.
- Codex records its review in the linked product brief.
- The review commit is pushed to this disposable branch.

