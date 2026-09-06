# Pipeline worker recovery

Status: IMPLEMENTED

## Problem and scope

Tasks 012 and 013 stalled because cron workers could not access Claude login.
The scheduler repeatedly dispatched cached blocking verdicts, spent the retry
budgets without actual fixes, and could reset worktrees still owned by workers.
Ralph requested diagnosis and recovery. This is an operational backend workflow
repair; no UI or household financial data changes are authorized.

## Acceptance criteria

1. Missing Claude auth leaves queued work and retry budgets unchanged.
2. Existing workers, including blocked ones, protect their worktree against
   review/reset/merge and duplicate retry dispatch; roster failures fail closed.
3. A dispatched retry's unchanged reviewed head does not consume another round.
   A new worker commit can be reviewed; stalled tasks remain stopped.
4. Use the existing login through a macOS GUI-session LaunchAgent on the same
   five-minute cadence, preserving other scheduled jobs and copying no Claude
   credentials. Verify auth in that session before recovery.
5. Recover only the retry budgets proven spent on infrastructure failure for
   tasks 012/013, preserve their findings, and restart one worker per task.
6. Automation regression tests and ./verify.sh pass. Existing product acceptance
   and required verify gates remain mandatory for each recovered feature.

## Delivery evidence

- 80 automation assertions pass, including repeated-tick behavior.
- ./verify.sh: 392 tests, zero failures/errors/skips (2026-09-06).
- GUI LaunchAgent auth probe: loggedIn=true; all four cron fix workers recorded
  immediate Not logged in failures with no implementation work.
- Operational installation and recovered worker results will be recorded after
  the reviewed pipeline change ships.

## Acceptance review

Pending independent review of this branch.
