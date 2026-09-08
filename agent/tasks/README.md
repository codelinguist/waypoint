# Task records

One file per numbered task records its scoped execution contract and links to
agent/product/<feature-slug>/product-brief.md. Existing records are retained;
they are not an executable queue. For Jira work, use the issue and linked brief
without creating a duplicate numbered task.

The user selects the task and stage. The agent updates status as part of that
requested stage; no status change dispatches work or triggers review or merge.

```yaml
status: QUEUED
task_number: 006
feature_slug: some-feature
branch: task/006-some-feature
pr:
```

Follow the metadata with the goal, deliverables, constraints, and brief link.
QUEUED means ready for user selection, IN_PROGRESS means implementation is
underway, IN_REVIEW means awaiting requested review, STALLED means blocked,
and MERGED means the requested merge was confirmed. Acceptance and findings
live in the brief. Legacy worker/session/timing/retry fields are historical
metadata only and need not be maintained. Do not rewrite old records as logs.
