# Task records (retired for new work)

New feature work is defined and tracked as a Jira issue that Codex creates
during the Frame stage (see `agent/collaboration-workflow.md`) and moves to
To Do; there is no repository brief or numbered task file to create alongside
it. This numbered-file format is retained only so the records below stay
readable as history; do not add new ones.

```yaml
status: QUEUED
task_number: 006
feature_slug: some-feature
branch: task/006-some-feature
pr:
```

QUEUED meant ready for user selection, IN_PROGRESS meant implementation was
underway, IN_REVIEW meant awaiting requested review, STALLED meant blocked,
and MERGED meant the requested merge was confirmed. Acceptance and findings
for these historical tasks live in their linked
`agent/product/<feature-slug>/product-brief.md`. Do not rewrite old records
as logs.
