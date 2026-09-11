# Ship

Run only for an explicit request to merge/deliver a specific issue or PR. Read
the current acceptance record, accepted revision, PR head, required check
status, and any discrepancy evidence.

The Jira issue must be in Acceptance with an ACCEPTED record for the current PR
head, and the required `verify` check must be green. A later commit invalidates
acceptance. Never bypass a missing, pending, or failed check.

Squash-merge the intended PR, move the issue to Done, and record completion.
Then remove its worktree and merged local branch only if they are unlocked and
clean; a session that created the worktree may use its supported exit/remove
operation. Never force cleanup of dirty or locked work. Report the merge,
issue status, and cleanup result.
