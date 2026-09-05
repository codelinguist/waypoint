# Automated pipeline

`orchestrator.sh` is the machine that makes `agent/tasks/` self-driving. See
`agent/collaboration-workflow.md` -> "Automated pipeline" for why this exists
and what it changes about the workflow's safety posture (bypassed
permissions/sandbox, automatic merge — both deliberate, both scoped to this
pipeline only).

## What it does, once per run

1. **Review + merge** any open `task/*` PR: runs an unattended Codex review
   (`review-prompt.md`) against PRs with new commits, merges automatically
   once Codex records acceptance and the required `verify` check is green,
   or dispatches a bounded automatic fix round (`worker-prompt.md`, with a
   fix-round note, budget tracked in the task file's `fix_rounds`) when
   Codex found `BLOCKING` findings. A sibling task merging first and leaving
   the PR merge-conflicted is a separate failure with its own bounded
   rebase-and-resolve round (`conflict_rounds`) — a review-finding fix round
   and a conflict-resolution round never share the same budget. If the
   review itself fails to produce a usable outcome (`codex exec` crashes,
   prints nothing, or returns an unparseable verdict line), that is
   `REVIEW_ERROR`, not `BLOCKING`: it is logged, consumes no budget,
   dispatches no worker, and never authorizes a merge — the PR is simply
   reviewed again next run.
2. **Dispatch** newly `QUEUED` tasks from `agent/tasks/`, up to
   `WAYPOINT_MAX_PARALLEL` (default 3) concurrently `IN_PROGRESS`. Each
   claimed task gets its own git worktree and an unattended
   `claude --bg "<prompt>"` background session (the prompt is passed
   positionally, not via `-p` — see the note in `dispatch_worker()`).

It is not itself a loop — it does one pass and exits. Cron provides the
repetition.

## Where things live

Everything the orchestrator touches is kept outside your interactive
checkout (`$REPO_ROOT` — wherever you normally run `claude` from), in a
sibling directory:

```text
../waypoint-orchestrator/
  control/            # the orchestrator's own clone, always synced to origin/main
  worktrees/
    task-006-.../      # one git worktree per in-flight task
```

`agent/automation/state/` (gitignored) holds a single-instance lock
(`.orchestrator.lock`, a plain `mkdir` lock — safe if a run overlaps a slow
previous one, which just exits immediately) and one `pr-<number>.json` per
open PR (`last_reviewed_sha` + `verdict`), so an unchanged PR isn't
re-reviewed every tick.

`agent/automation/logs/orchestrator.log` (gitignored) has one line per
significant action — claims, dispatches, review verdicts, merges, stalls.
Tail it to see what the pipeline has been doing:

```
tail -f agent/automation/logs/orchestrator.log
```

## Installing the cron job

```
crontab -e
```

Add (every 5 minutes; adjust to taste — a shorter interval just means more
skipped runs while the lock is held, not more throughput):

```
*/5 * * * * /Users/rj/Documents/projects/waypoint/agent/automation/orchestrator.sh >> /Users/rj/Documents/projects/waypoint/agent/automation/logs/cron.log 2>&1
```

This only runs while this machine is on and this user is logged in (cron on
macOS is machine-local, not a service that survives a reboot into a
different session by default). That's an accepted limitation of the "local
cron" choice, not a bug — see `agent/collaboration-workflow.md` -> "Automated
pipeline".

## Turning it off

- Temporarily: `crontab -e` and comment out or delete the line.
- Mid-flight: `claude agents` lists background worker sessions; `claude stop
  <id>` stops one without losing its conversation. `codex` invocations inside
  a review pass are short-lived and finish on their own.
- A task stuck at `STALLED` needs a human: read its file in `agent/tasks/`,
  the review findings in its product brief, and either fix it by hand in its
  worktree (`../waypoint-orchestrator/worktrees/task-<slug>/`) or delete the
  worktree and requeue.

## Tests

`agent/automation/tests/orchestrator_test.sh` is a lightweight local
regression suite for the pure logic in `orchestrator.sh` — frontmatter
read/write (including status transitions and the two independent retry
counters), prompt rendering (asserting the rendered `{{TASK_FILE}}` path is
never doubled), review-verdict classification (`ACCEPTED` / `BLOCKING` /
`REVIEW_ERROR`, including that a failed or malformed Codex invocation never
becomes `BLOCKING`), the `verify`-check state filter, and Flyway
migration-collision detection against disposable local git fixtures. It does
not call `gh`, `codex`, or `claude`, and is not part of `./verify.sh` (which
is reserved for the Java Maven suite per D014) — run it directly:

```
agent/automation/tests/orchestrator_test.sh
```

## First run

This pipeline has real side effects — it commits, pushes, opens PRs, and
merges to `main` unattended. Run it once by hand and read
`agent/automation/logs/orchestrator.log` before adding the cron entry,
especially to confirm the `claude --bg` session-id parsing in
`dispatch_worker()` actually matches what your installed `claude` version
prints — that's the one piece this script can't fully verify without a real
queued task to dispatch.
