# Revise

Before editing, read the Jira issue and current-round review findings and
re-enter the existing `.claude/worktrees/<issue-key>` worktree on the same task
branch. Ensure the issue is In Progress. Read affected code/contracts/tests and use
`docs/decisions/index.md` to activate only relevant deeper context.

Resolve accepted findings without expanding scope. Material product or design
changes require an updated issue and new Codex approval. Run affected checks
and `./verify.sh`, update evidence, push, and move the issue back to Review.
Report completion; another Review begins only when the user requests it.
