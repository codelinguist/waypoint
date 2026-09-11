# Implement

Read the Jira issue in full when one exists, the approved design brief if any,
affected code/tests/contracts, and matching decisions selected through
`docs/decisions/index.md`. Open product, domain, or architecture material only
when the change depends on its rules. Preserve dirty unrelated work.

For Jira work, transition To Do to In Progress and use an isolated worktree on
`task/<issue-key>-<feature-slug>` (lowercase key). For an explicitly requested
repository change without an issue, use `codex/<feature-slug>`. Only one agent
edits a feature; concurrent edits require separate worktrees and ownership.
Resolve conflicts and check migration-version collisions against `main` before
handoff when applicable.

Implement the smallest complete increment. Use
`agent/templates/implementation-plan.md` only when complexity warrants it.
Keep financial rules deterministic and emphasize domain behavior in tests.
Run relevant focused checks and `./verify.sh`; exercise the primary flow and
capture wide/narrow evidence for UI work.

Record durable contracts and decisions in their canonical homes, not a task
log. Push and open/update a PR for every implementation task. For Jira work,
include the issue key in its title and branch, add evidence to the issue or
feature contract, and move the issue to Review. Implementation authorizes push
and PR creation, not merge or review. Report the PR and evidence; do not start
Review automatically.
