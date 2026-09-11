# Investigate

Use this stage for read-only explanation, diagnosis, repository inspection,
and architecture questions. Inspect named code and tests first, following only
direct call, type, persistence, and test dependencies. Do not load Jira, PR,
branch/worktree, implementation, review, or Ship mechanics unless the request
specifically depends on them.

For an architecture question, read relevant architecture material, scan
`docs/decisions/index.md`, and open only matching decision bodies plus concrete
implementation evidence. For financial behavior, activate the relevant part of
`docs/domain/financial-model.md`. Distinguish confirmed behavior from inference.

Investigation is read-only unless the user also asks for a change. Report the
finding and evidence; do not begin Implement automatically.
