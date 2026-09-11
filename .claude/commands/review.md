---
description: Independently review a PR against its Jira issue and record ACCEPT/RETURNED
argument-hint: <issue-key or PR number>
---

# Review: $ARGUMENTS

Read `AGENTS.md` and `agent/workflow/review.md`; that stage file is canonical
for scope, evidence, and the acceptance record. Resolve the exact Jira issue
and PR; read the current PR diff/revision, the current-round findings, and
available evidence. Stop if the issue or PR can't be resolved rather than
guessing.

If Claude Code implemented this revision itself, delegate the actual review
read (diff vs. acceptance criteria, evidence) to a fresh subagent with no
memory of the implementation, per D020 — do not review from this session in
that case. If a different agent implemented it, review directly.

Record BLOCKING, RECOMMENDED, or OPTIONAL findings and acceptance conditions
as a comment on the Jira issue. ACCEPT only when every criterion has
evidence — record the exact revision and move the issue to Acceptance.
Otherwise record RETURNED and move it to In Progress.

Report the verdict and stop. Do not start Revise.
