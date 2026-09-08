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
7. **Report.** Tell the user the merge commit, and the issue's new status.
