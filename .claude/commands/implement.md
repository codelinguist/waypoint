---
description: Pick up a Jira issue and implement it in an isolated worktree
argument-hint: <issue-key>
---

# Implement: $ARGUMENTS

Read `AGENTS.md` and `agent/workflow/implement.md`; that stage file is canonical
for scope, context, verification, and delivery. This command requires a Jira
issue key. Fetch its description and current-round comments through Jira; stop
if unavailable rather than inventing requirements.

Require To Do status, move it to In Progress, and create the deterministic
`.claude/worktrees/<issue-key>` worktree from `main` on
`task/<issue-key>-<feature-slug>` (all lowercase). If already in a worktree or
the issue is not To Do, report the state and ask before changing course.

Complete the Implement stage. Leave the worktree for a possible Revise stage
and report the PR, evidence, and final Jira status. Do not start Review.
