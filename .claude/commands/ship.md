---
description: Merge an accepted PR to main and close out its Jira issue
argument-hint: <issue-key or PR number>
---

# Ship: $ARGUMENTS

Run the Ship stage described in `agent/collaboration-workflow.md`. This
merges to `main` — only run it because the user asked for this specific
action right now, not because acceptance and a green check happen to line
up. Never bypass a missing, pending, or failed required check.

If `$ARGUMENTS` isn't an issue key or PR number, ask which one instead of
guessing.

## Steps

1. **Resolve the PR.** If given an issue key, find its PR — check the Jira
   issue's Development panel via the Atlassian MCP tools, or
   `gh pr list --head task/<issue-key>-*`. If given a PR number, use it
   directly.
2. **Confirm acceptance.** Read the Jira issue: it must be in Acceptance
   with an ACCEPTED comment from Codex. If it isn't, stop and tell the user
   rather than merging unaccepted work.
3. **Confirm acceptance isn't stale.** Check whether any commit landed on
   the PR after the ACCEPTED comment's recorded revision. If so, per
   collaboration-workflow.md acceptance no longer applies to the current
   head — stop and tell the user it needs another Review pass, don't merge.
4. **Confirm the required check.** The `verify` check must be green on the
   PR's current head commit (`gh pr checks <number>`). Pending or failed
   blocks the merge — report it and stop.
5. **Merge.** Squash-merge the PR (`gh pr merge <number> --squash`), matching
   this repo's existing merge history.
6. **Close out the issue.** Transition the Jira issue from Acceptance to
   Done.
7. **Clean up the worktree, if any.** Find a worktree for this issue's branch
   with `git worktree list` (look for `task/<issue-key>-...`). If this
   session entered that worktree itself earlier on via EnterWorktree (e.g.
   during Implement or Revise), use `ExitWorktree` with `action: "remove"`
   and `discard_changes: true` — it unlocks and removes its own worktree and
   branch in one step, which plain `git worktree remove` cannot do while the
   lock it holds is in place. `discard_changes` is safe here because the
   branch's commits are already captured in the squash merge you just made.
   Otherwise this command is running in a different session than the one
   that created the worktree, so use `git worktree remove <path>` directly
   (ExitWorktree only acts on worktrees the current session created), then
   delete the now-merged local branch with `git branch -d <branch>`. Only
   remove it that way if it isn't locked and has no uncommitted changes — if
   it's locked by another session, dirty, or doesn't exist, skip this step
   and say so; don't force it.
8. **Report.** Tell the user the merge commit, the issue's new status, and
   whether a worktree was cleaned up.
