---
description: Merge an accepted PR to main and close out its Jira issue
argument-hint: <issue-key or PR number>
---

# Ship: $ARGUMENTS

Read `AGENTS.md` and `agent/workflow/ship.md`; the latter is canonical. Resolve
the exact Jira issue and PR. Stop if acceptance is absent/stale or the current
head's required `verify` check is not green.

Complete the Ship stage with a squash merge. Clean up only a clean, unlocked
worktree and merged local branch; use the session's supported worktree exit
operation when it created that worktree. Report the merge, Jira status, and
cleanup result.
