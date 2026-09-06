# Automated pipeline

`orchestrator.sh` is the machine that makes `agent/tasks/` self-driving. See
`agent/collaboration-workflow.md` -> "Automated pipeline" for why this exists
and what it changes about the workflow's safety posture (an unsandboxed Claude
worker, a sandboxed but network-enabled Codex reviewer, and automatic merge —
all deliberate and scoped to this pipeline only).

## What it does, once per run

1. **Review + merge** any open `task/*` PR: runs an unattended Codex review
   (`review-prompt.md`) against PRs with new commits under
   `-s workspace-write -c sandbox_workspace_write.network_access=true`, merges automatically
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
previous one, which just exits immediately), one `pr-<number>.json` per
open PR (`last_reviewed_sha` + `verdict`), so an unchanged PR isn't
re-reviewed every tick, and `gh-token` (mode 600) — a cached copy of `gh
auth token`'s output, refreshed whenever that command succeeds, and read
back whenever it doesn't (a cron-launched process runs in a separate macOS
security session from an interactive login shell, without that session's
Keychain access — see `ensure_gh_token()` and the 2026-09-05 "Cron's `gh`
auth hits the same Keychain-session gap git did" implementation-log entry).
Never committed; if it's ever missing and `gh auth token` fails under cron,
the run stops before any Git or GitHub operation. Run the script once
interactively after authenticating `gh` to reseed it.

### Optional repository-scoped GitHub token

For a smaller credential blast radius, create a fine-grained personal access
token in GitHub's web UI and limit repository access to this repository. Grant
only Metadata read (mandatory), Contents read/write, Pull requests read/write,
and Checks read. Store it at:

```text
agent/automation/state/gh-token.scoped
```

Then restrict its permissions:

```sh
chmod 600 agent/automation/state/gh-token.scoped
```

The override is authoritative whenever it exists. A symlink, empty file,
permissions other than `600` or `400`, or a token that fails an authenticated
`gh api /user` request
aborts the run; the orchestrator deliberately does not fall back to its broader
account token or cache. Rotate it by replacing its contents and restoring mode
`600`. Delete it to return to the normal `gh auth token`/cache path. The state
directory is gitignored, but the token remains plaintext on this host.

The Codex sandbox restricts filesystem/process writes to its workspace, but
network access is enabled without a host allowlist and the reviewer receives
`GH_TOKEN`. The scoped token mitigates that credential exposure; it does not
remove it.

`agent/automation/logs/orchestrator.log` (gitignored) has one line per
significant action — claims, dispatches, review verdicts, merges, stalls.
Tail it to see what the pipeline has been doing:

```
tail -f agent/automation/logs/orchestrator.log
```

## Scheduling on macOS

Run every five minutes as a user LaunchAgent in the `gui/<uid>` domain,
with `LimitLoadToSessionType` set to `Aqua`, `StartInterval` set to `300`,
and `ProgramArguments` pointing to the absolute `orchestrator.sh` path.
Use the repository root as `WorkingDirectory` and redirect stdout/stderr to
`agent/automation/logs/scheduler.log`. Install the plist under
`~/Library/LaunchAgents/com.waypoint.orchestrator.plist`, then load it with
`launchctl bootstrap gui/$(id -u) <plist>`.

Do not schedule this worker pipeline through cron. Claude's macOS Keychain
login is available in the logged-in GUI session but was unavailable to cron
workers, which exited immediately with `Not logged in`. A LaunchAgent uses
the existing login without copying an OAuth token into a plaintext cache.
Remove only the old Waypoint crontab entry when switching; preserve other jobs.
The existing single-instance lock still protects overlapping invocations.

The machine must be awake and the user logged in. A failed Claude auth
preflight leaves queued tasks and retry budgets unchanged. Active or blocked
workers retain exclusive ownership of their worktrees. After a fix or rebase
is dispatched, an unchanged PR head waits without consuming another round,
even if its worker has exited. A new worker commit permits another review;
`STALLED` tasks require explicit recovery and are not repeatedly dispatched.

To recover a login-blocked task, first verify auth from the scheduler's session,
stop its failed workers, inspect the worktree for unpushed work, and repair the
infrastructure issue. Under the orchestrator lock, use its frontmatter helpers
to return the task to `IN_REVIEW`. Restore only retry budget proven to have
been spent on infrastructure failures, record the evidence in the implementation
log, and let the next scheduled pass dispatch one fresh worker. Do not delete
review verdicts, bypass acceptance/CI, or reset a worktree owned by a worker.

## Turning it off

- Temporarily: `launchctl bootout gui/$(id -u)/com.waypoint.orchestrator`.
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
