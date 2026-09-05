# Implementation Log

### 2026-09-05 — Orchestrator safety audit: review-error handling, retry-budget separation, isolation-claim correction, and tests

**Changed**

Addressed a six-item audit Codex (product-owner role, reviewing the
orchestrator infrastructure itself rather than a feature) handed the user
directly, outside the normal task-queue flow. Treated as infra/tooling work
like the earlier `fix/orchestrator-*` branches, not a queued feature — no
product brief, implemented and PR'd directly.

1. **Duplicated task path.** `agent/automation/review-prompt.md` referenced
   `agent/tasks/{{TASK_FILE}}`, but `orchestrator.sh` already renders
   `{{TASK_FILE}}` as `agent/tasks/<filename>` — the rendered prompt told
   Codex to read `agent/tasks/agent/tasks/<filename>`, a path that never
   exists. Fixed by dropping the hardcoded prefix; `{{TASK_FILE}}` already
   carries it (matching how `worker-prompt.md` already used it correctly).
2. **Review-infrastructure failures were miscoded as `BLOCKING`.** A crashed
   or empty `codex exec` invocation, or a response missing the
   `REVIEW_VERDICT:` line, previously fell into the same `else` branch as a
   genuine `BLOCKING` review finding — burning a `fix_rounds` attempt and
   dispatching a worker to "fix" findings that were never actually
   recorded, on every single infrastructure hiccup. Added a
   `REVIEW_ERROR` outcome: `determine_review_verdict()` (a new pure
   function, extracted from `run_review()` so it's unit-testable) returns
   it whenever `codex exec` exits nonzero, produces no output, or returns
   unparseable content; `review_and_merge_phase()` logs it, skips writing
   the per-PR state file entirely (so the next tick's `head_sha != last_sha`
   check naturally retries the review from scratch), and `continue`s before
   the `ACCEPTED`/`BLOCKING` case statement — no fix round, no `fix_rounds`
   increment, no merge.
3. **Isolation-claim correction.** `agent/collaboration-workflow.md` ->
   "Automated pipeline" claimed bypassed sessions were "confined to one
   task's worktree/branch." A git worktree only isolates which branch a
   session works on — it does not restrict filesystem, network, process, or
   credential access. Rewrote the paragraph to say so plainly, name the
   concrete instance (the GitHub token `ensure_authenticated_remote()`
   embeds in the control clone's `.git/config`), and note that a real
   sandbox/container or a restricted OS account with narrowly scoped
   credentials is the actual next hardening step, not yet done.
4. **Stale invocation docs.** `agent/automation/README.md` said the worker
   session is `claude -p`; the actual, and only working, invocation is
   `claude --bg "<prompt>"` (positional prompt — `-p`/`--print` conflicts
   with `--bg` on the installed CLI). Fixed there and in `worker-prompt.md`'s
   own header comment, which had the same stale claim.
5. **Shared retry budget for two unrelated failure classes.** `fix_rounds`
   was being incremented both by `handle_blocking()` (a genuine Codex
   `BLOCKING` product-review finding) and `handle_conflict()` (a PR gone
   merge-conflicted because a sibling task merged first) — so either kind of
   trouble could exhaust the budget the other needed. Split into two
   independent frontmatter fields and counters: `fix_rounds` /
   `MAX_FIX_ROUNDS` stay `handle_blocking()`-only; a new `conflict_rounds` /
   `MAX_CONFLICT_ROUNDS` (`WAYPOINT_MAX_CONFLICT_ROUNDS`, default 2, same as
   before) is now `handle_conflict()`-only. Added `conflict_rounds: 0` to
   the task-file template (`agent/tasks/README.md`) and to the three
   currently `IN_PROGRESS` files (009, 010, 011) so the field exists before
   any conflict round is ever dispatched against them.
6. **`set_frontmatter_field()` couldn't add a field, only rewrite one.**
   Discovered while wiring up (5): the existing `sed` implementation
   silently no-ops when the field has no line yet in a file's frontmatter —
   which would have made `conflict_rounds` permanently invisible to
   `frontmatter_field()` for any task file authored before this field
   existed (an older `STALLED` file picked up by hand, or a future `QUEUED`
   file Codex writes from a stale cached template), and the `(( conflict_rounds
   >= MAX_CONFLICT_ROUNDS ))` check would then never fire. Hardened it to
   insert the field just above the closing `---` when no existing line
   matches, instead of doing nothing.
7. **Added `agent/automation/tests/orchestrator_test.sh`.** A local,
   dependency-light regression suite (no `gh`/`codex`/`claude` calls) that
   sources `orchestrator.sh` (guarded so its `main "$@"` no longer fires on
   `source`, only on direct execution) and exercises: frontmatter
   read/write including status transitions and the field-insertion fix
   above; that `fix_rounds` and `conflict_rounds` are stored and updated
   independently (plus a source-shape check that `handle_blocking`/
   `handle_conflict` never reference each other's field); rendered
   worker/review prompt paths, explicitly asserting no doubled
   `agent/tasks/agent/tasks/...`; `determine_review_verdict()` against
   valid `ACCEPTED`/`BLOCKING`, a missing message file, unparseable
   content, and a nonzero `codex exec` exit status (each must be
   `REVIEW_ERROR`, never `BLOCKING`); a source-shape check that
   `review_and_merge_phase()`'s `REVIEW_ERROR` branch `continue`s before
   ever writing the PR state file; the exact `jq` filter `try_merge` uses
   for the `verify` check against canned `MISSING`/`PENDING`/`SUCCESS`
   JSON; and Flyway migration-collision detection against disposable local
   git fixtures (a clean new version, two branches independently claiming
   the same version, and a single branch claiming one version twice).
   Deliberately does not attempt a full mock of `gh`/`codex`/`claude` end
   to end (dispatch_worker, the real `codex exec` call, `gh pr
   checks`/`gh pr merge`) — noted explicitly in the file's header as a
   scope boundary, not an oversight. Not part of `./verify.sh` (reserved
   for the Java Maven suite per D014); run directly.

**Tests**

- `agent/automation/tests/orchestrator_test.sh`: 35 passed, 0 failed.
- `bash -n agent/automation/orchestrator.sh` and `bash -n
  agent/automation/tests/orchestrator_test.sh`.
- No `./verify.sh` run: no Java application code touched.
- Confirmed no drift against `origin/main` before and after editing the
  live `IN_PROGRESS` task files 009-011 (cron runs every 5 minutes and
  writes to these same files), to avoid clobbering an in-flight
  orchestrator write.

**Decisions**

- Treated this as direct infra/tooling work (branch `fix/orchestrator-
  review-safety`, no product brief, no `agent/tasks/` entry), matching how
  the prior `fix/orchestrator-first-run-bugs`,
  `fix/orchestrator-conflict-self-healing`, and
  `fix/orchestrator-cron-git-auth` branches were handled — the task-queue
  Plan/Implement/Validate cycle is for household-facing features, not for
  fixing the machine that runs it.
- Gave `MAX_CONFLICT_ROUNDS` its own env var (`WAYPOINT_MAX_CONFLICT_ROUNDS`)
  rather than reusing `WAYPOINT_MAX_FIX_ROUNDS` for both budgets, even
  though they currently default to the same value (2) — the whole point of
  item 5 was that these are conceptually different failure classes, so
  their bounds should be independently tunable even if nobody has needed to
  tune them differently yet.
- Scoped the isolation-language fix to `agent/collaboration-workflow.md` only
  (per the feedback), not `AGENTS.md`'s separate mention of worktree
  isolation there — that passage is about the concurrency mechanism
  (parallel tasks not clobbering each other's code), not a security-boundary
  claim, so it wasn't inaccurate in the same way.

**Assumptions**

- Assumed the audit's "priority order" (fix 1-2 before the next unattended
  run) meant "implement all six before this lands," not "ship 1-2 alone
  first" — cron runs every 5 minutes regardless, so there was no practical
  way to land only a subset before the next tick anyway; all six shipped
  together on one branch.

**Open questions**

- Real sandboxing/containment for unattended sessions (item 3's "next
  hardening step") is still not implemented — this pass only corrected the
  documentation to stop overstating what worktree isolation provides. Left
  as a follow-up, not attempted here, since it's a meaningfully larger
  change (a container or restricted OS account, credential scoping) than a
  documentation fix.
- The new test suite cannot exercise `dispatch_worker`, `run_review`'s
  actual `codex exec` call, or `try_merge`'s `gh pr checks`/`gh pr merge`
  end-to-end without a real or thoroughly mocked GitHub/Codex/Claude
  surface. If a bug is ever suspected specifically in that boundary (not
  the decision logic around it, which is covered), it will need a live or
  much heavier test run to catch.

**Recommended next task**

- Let Tasks 009-011 continue through review and merge, now covered by
  `REVIEW_ERROR` handling and the split retry budgets, and watch
  `agent/automation/logs/orchestrator.log` for the first real
  `REVIEW_ERROR` or `conflict_rounds` occurrence to confirm the new paths
  behave as designed under a live run, not just the local test suite.

### 2026-09-05 — Cron's git auth, a self-authored commit bug, and Tasks 009-011

**Changed**

- Confirmed cron actually dispatches and reviews/merges unattended, end to
  end, for the first time -- but getting there required fixing two more
  real problems on top of the earlier Full Disk Access and `PATH` fixes:
  1. **Cron-specific git authentication failure.** Once PATH and Full Disk
     Access were both fixed, cron's very first git operation
     (`sync_control_clone`'s fetch) started failing with `fatal: could not
     read Username for 'https://github.com': Device not configured` --
     repeating on every tick. `cron` runs in a different macOS security
     session than an interactive login shell, and the `osxkeychain` git
     credential helper depends on session identity for some keychain
     items, not just environment variables; git fell through to an
     interactive credential prompt with no TTY to prompt on. Confirmed
     this wasn't a `PATH`/env issue by reproducing a stripped-down
     environment (`env -i PATH=... HOME=...`) run by hand, which still
     succeeded -- only a real cron-launched process actually failed.
     Fixed by adding `ensure_authenticated_remote()`, which embeds `gh
     auth token`'s current token directly into the control clone's remote
     URL (a plain value in a local, uncommitted `.git/config`, not looked
     up via the keychain at request time), re-applied every run so a
     rotated token is always picked up. Confirmed with the user before
     applying, since it's a real credential-storage tradeoff (plaintext
     local file vs. keychain ACLs), not a silent default.
  2. **A self-authored bug, not a Codex or orchestrator one.** Tasks
     009-011's task files still had blank `feature_slug` on `main` after I
     believed I'd already fixed them (previous entry's commit 96b6947
     claimed to). The real cause: Codex's own authoring script had already
     run `git add` on the blank-`feature_slug` files; I then edited the
     *working tree* files with the fix but ran `git commit` without
     re-staging, so the commit captured the stale staged (blank) content,
     not my edit. `git status --short` even showed the tell (`AM`, staged
     Added + unstaged Modified) and I didn't catch it. Cron then spent 15+
     minutes (three ticks) repeatedly trying and failing to claim task 009
     with a malformed branch name (`task/009-`, slug missing), logging the
     identical `push_control_main` conflict every time -- because each
     fresh run resets the control clone cleanly to `main`, reads the same
     still-broken content, and reproduces the same failure. Fixed by
     actually re-staging and verifying the diff before committing this
     time.
- Framed and queued Tasks 009 (emergency-fund runway), 010 (debt
  amortization), and 011 (equal monthly goal contributions) via Codex --
  three Phase 7 calculations with exclusive package ownership
  (`backend/.../planning/<pkg>/**`) and no shared migrations, entities, or
  prose files, deliberately deferring README/implementation-log updates to
  a post-batch consolidation to avoid the Task 007/008 collision class.
  Confirmed all three dispatched into separate worktrees by an actual
  unattended cron tick (not a manual run) once the auth fix landed.

**Tests**

- No `./verify.sh` run for the auth-fix commit itself (no application code
  touched).
- `bash -n agent/automation/orchestrator.sh` after the change.
- The real test: a manual run confirmed `ensure_authenticated_remote()`
  unblocks `sync_control_clone`, and dispatch of Tasks 009-011 succeeded
  immediately afterward with correct branch names and worktrees.

**Decisions**

- Embedded the token in the control clone's remote URL rather than trying
  to grant the cron-launched process broader Keychain access (e.g. via
  `security` ACL commands): the token approach is verifiable in a plain
  file, doesn't depend on macOS's session-scoped Keychain behavior at all,
  and re-applying it every run handles token rotation automatically.
  Scoped to the control clone only -- the user's interactive checkout
  keeps using the normal keychain-backed credential helper.

**Assumptions**

- Assuming `gh auth token` itself succeeds under cron's session (it must,
  since the fix worked on the very next real cron tick); if `gh`'s own
  token storage ever turns out to have the same session-dependency
  problem as git's credential helper, `ensure_authenticated_remote()`
  would need a non-keychain-dependent way to obtain the token too.

**Open questions**

- Whether `gh` itself (not just plain `git`) would hit a similar
  session-dependent Keychain failure for any command that runs before
  `ensure_authenticated_remote()` has a chance to embed a fresh token --
  not yet observed, since `sync_control_clone`'s `git fetch` was always
  the first operation to fail.

**Recommended next task**

- Let Tasks 009-011 run all the way through review and merge unattended,
  watching specifically for whether `gh` commands (not just `git`) ever
  hit a cron-specific auth failure of their own.

### 2026-09-05 — Parallel dispatch of Tasks 007/008, four more orchestrator bugs, and self-healing merge conflicts

**Changed**

- Framed and queued Tasks 007 (Goals domain) and 008 (Plan-versus-Actual)
  via Codex in one pass, deliberately chosen to be mutually independent
  (own files/migrations, no shared dependency) so they could be dispatched
  into separate worktrees in the same orchestrator pass. Confirmed genuine
  parallel execution directly via `claude agents --json`: both sessions
  showed `status: busy, state: working` simultaneously, as separate OS
  processes, seconds apart.
- Installed the cron entry from `agent/automation/README.md`
  (`*/5 * * * *`). It fires on schedule but fails immediately with `bash:
  .../orchestrator.sh: Operation not permitted` -- almost certainly macOS
  blocking `cron` itself from `~/Documents` without Full Disk Access
  (System Settings -> Privacy & Security). Not yet granted; every dispatch/
  review/merge in this entry was still run by hand.
- Fixed four more real bugs found running both tasks through review and
  merge, none caught by `bash -n` or the previous live run (which only ever
  had one PR open at a time):
  1. `review_and_merge_phase`'s `while read pr_number branch; do ... done
     <<< "$prs"` shared stdin (fd 0) with `codex exec`, called deep inside
     the loop body. `codex exec` is an interactive-capable CLI and can
     read from/seek on whatever it inherits as stdin; doing so silently
     consumed the loop's remaining input, so the second PR in a two-PR
     list was never even attempted -- no error, no log line, nothing.
     Fixed by reading from fd 3 instead (`done 3<<< "$prs"`, `read -u 3`),
     which fully decouples the loop's input from anything a subprocess in
     its body does with fd 0.
  2. `try_merge()` edited `$task_file` locally (status -> `MERGED`)
     immediately after `gh pr merge` succeeded, without first fetching the
     squash-merge commit `gh pr merge` had just created on `origin/main`
     via GitHub's API -- a purely remote change this clone didn't know
     about yet. That squash commit already contains the PR branch's own
     last edit to the same file (e.g. the worker's `status: IN_REVIEW`),
     so the local edit diverged from it and failed to push as a
     genuinely-conflicting rebase, which crashed the whole script
     mid-run (`set -e`) and blocked every PR still left in that run from
     being processed at all. Fixed by fetching and hard-resetting the
     control clone to `origin/main` immediately after a successful `gh pr
     merge`, before touching `$task_file` again.
  3. `push_control_main()`'s conflict fallback (`git pull --rebase` then
     retry push) had no failure path of its own: if the rebase itself hit
     a real conflict (as in bug 2), the failing command wasn't inside its
     own conditional, so `set -e` killed the script leaving the control
     clone mid-rebase -- a broken state that would have made every
     subsequent orchestrator run fail until a human intervened. Fixed to
     detect this, `git rebase --abort` back to a clean state, log the
     conflict, and return failure to the caller instead of crashing or
     leaving broken state behind.
  4. Discovered only via manual recovery, not by re-running the fixed
     script: my own manual fix for bug 2 (merging `main` into an
     already-created worktree without first syncing that worktree to the
     PR's true current remote tip) produced a merge that silently omitted
     Codex's own review commit -- the merge "succeeded" but the resulting
     history was wrong. `handle_blocking()` already did this sync
     correctly (`git fetch` + `git reset --hard origin/$branch` before
     handing a worktree back to a worker); the new `handle_conflict()`
     below follows the same pattern deliberately, with a comment pointing
     at this exact mistake so it isn't repeated.
- Added self-healing for merge conflicts between parallel tasks, which is
  what actually happened to PR #9 (Task 007) once PR #10 (Task 008) merged
  first -- both had independently edited `README.md`'s Status prose,
  `agent/implementation-log.md`'s shared append point, and (a real code
  conflict) `ApiExceptionHandler.java`, where each task had added its own
  independent `@ExceptionHandler` method:
  - **Prevention**: `agent/automation/worker-prompt.md` now has every
    worker merge `origin/main` into its own branch and resolve conflicts
    -- keeping both sides' independent additions -- before opening its PR,
    re-running `./verify.sh` afterward. This only prevents conflicts that
    exist *before* a PR opens, not ones introduced by a sibling merging
    later.
  - **Self-healing**: `try_merge()` now checks `gh pr view --json
    mergeable` before attempting a merge. `CONFLICTING` dispatches a new
    `handle_conflict()`, which redispatches a worker (bounded by the same
    `fix_rounds`/`MAX_FIX_ROUNDS` limit as a `BLOCKING` review finding)
    with explicit instructions: keep both sides for independent doc/code
    additions, use judgment only for a genuine logical clash, and always
    defer a Flyway migration-version conflict to a human (`STALLED`)
    rather than resolving it automatically -- that one is a product/schema
    decision, not a mechanical merge.
- Manually resolved PR #9's actual conflict this way (README.md,
  implementation-log.md, ApiExceptionHandler.java -- keeping both tasks'
  additions in each), confirmed `./verify.sh` still passes (208 tests) on
  the merged result, before building the automated version above.

**Tests**

- No `./verify.sh` run for the orchestrator/prompt changes themselves (no
  application code touched). `./verify.sh` was run on PR #9's manually
  resolved merge commit: 208 tests, 0 failures, migrations V1-V5 applying
  cleanly -- the real evidence that "keep both sides" was the correct
  resolution for every conflict encountered.
- `bash -n agent/automation/orchestrator.sh` after every change in this
  entry.

**Decisions**

- Did not attempt to eliminate the shared-file contention structurally
  (e.g. splitting `agent/implementation-log.md` into one file per task, or
  generating `README.md`'s Status section from `agent/tasks/*.md` instead
  of hand-editing prose) in this pass. That would reduce how often this
  class of conflict happens at all, but "make the agent able to resolve it
  itself" (the capability actually asked for) doesn't require it, and
  restructuring durable historical files is a bigger, separate decision.
  Recorded as an open question below rather than a silent scope increase.
- Reused the existing `fix_rounds`/`MAX_FIX_ROUNDS` bound for conflict
  rounds rather than adding a separate counter: both are "another
  automatic round before giving up and asking a human," and one shared,
  already-understood limit is easier to reason about than two.
- Deliberately excluded Flyway migration-version conflicts from
  `handle_conflict()`'s automatic resolution (instructed the worker to
  stop and mark `STALLED` instead) -- that class already has its own
  dedicated pre-merge check (`check_migration_collision`), and letting a
  worker improvise a schema-version renumbering unattended is a
  meaningfully different risk than reconciling two doc paragraphs.

**Assumptions**

- Assuming `gh pr view --json mergeable` reliably distinguishes
  `MERGEABLE`/`CONFLICTING`/other in practice; treated anything other than
  those two as "not computed yet, retry next run" rather than erroring,
  since GitHub can return this asynchronously.
- The macOS Full Disk Access diagnosis for the cron failure is inferred
  from the error text and from the identical script succeeding when run
  directly by an already-permitted process, not confirmed by actually
  granting the permission and observing success -- that confirmation is
  still pending.

**Open questions**

- Whether `README.md`'s Status section and `agent/implementation-log.md`
  are worth restructuring (generated from `agent/tasks/*.md`; split into
  per-task files) specifically to reduce how often parallel tasks collide
  on them, now that it's happened on two consecutive multi-task runs.
- Whether `orchestrator.sh` needs an automated test harness (raised in the
  previous entry too, now with two more bug classes -- stdin sharing and
  post-merge sync ordering -- that a mocked `claude`/`codex`/`gh` harness
  would have caught before a live task branch did).
- Whether cron actually works once Full Disk Access is granted, or there's
  a second macOS restriction layered underneath it.

**Recommended next task**

- Grant `cron` Full Disk Access and confirm a real unattended tick
  succeeds end-to-end (dispatch or review/merge, whichever is pending at
  the time) with nobody running the script by hand.
- Frame a task that's very likely to collide with something already
  `IN_PROGRESS` (e.g. another README-touching task) specifically to
  exercise `handle_conflict()` automatically end-to-end, the way Task 006
  was the first real test of the base pipeline.

### 2026-09-05 — Task 007: Household Financial Goals

**Changed**

- Added a `FinancialGoal` JPA entity (`backend/src/main/java/com/waypoint/household/`):
  household, `name`, `targetAmount`, `currency`, `targetDate`, `priority`,
  explicit `currentAmount`, plus `id`/`createdAt`/`updatedAt`, following the
  existing `Asset`/`Liability` field/annotation style. No update or delete
  path exists, matching the task's out-of-scope list.
- Added `FinancialGoalRepository` (`findByHousehold_IdOrderByPriorityAscCreatedAtAscIdAsc`,
  `findByIdAndHousehold_Id`), `FinancialGoalNotFoundException`, and
  `FinancialGoalService` (household-scoped create/get/list, matching the
  `AssetService` not-found and normalization pattern: trims `name`,
  uppercases `currency`). Progress arithmetic
  (`FinancialGoalService.remainingAmount`/`progressPercentage`) is exposed as
  static, DB-independent methods so it is callable and testable without a
  request context, per `AGENTS.md`'s "financial calculations must be callable
  without an LLM" and "deterministic and testable" rules.
- Added `FinancialGoalController` under `/api/households/{householdId}/goals`
  (`POST`, `GET /{id}`, `GET` list), `CreateFinancialGoalRequest` (Bean
  Validation: non-blank name, `targetAmount > 0`, non-negative
  `currentAmount`, both `@Digits(integer=17, fraction=2)`, 3-letter
  `currency`, `@FutureOrPresent targetDate`, `priority >= 1`), and
  `FinancialGoalResponse` (adds computed `remainingAmount` and
  `progressPercentage` alongside the stored fields — PD-002: progress is
  derived on read, never stored).
- Wired `FinancialGoalNotFoundException` into `ApiExceptionHandler`
  (`FINANCIAL_GOAL_NOT_FOUND`, 404).
- Added Flyway migration `V5__create_financial_goals.sql`: `financial_goals`
  table with `CHECK (target_amount > 0)`, `CHECK (current_amount >= 0)`,
  `CHECK (priority > 0)`, and a household FK+index. Task 008
  (plan-versus-actual) is running in a parallel worktree off the same `main`;
  this migration only touches a new `financial_goals` table and does not
  edit any Task 008 file, per the task's independence constraint.

**Tests**

- `FinancialGoalServiceTest` (Mockito unit tests, 12 cases): household-not-
  found on create/get/list; name trimming and currency uppercasing on
  create; cross-household get returns not-found; priority-ordered listing;
  `remainingAmount` for a normal and an overachieved (current > target)
  goal; `progressPercentage` at a mid-range value, clamped to 100 when
  current exceeds target, clamped to 0 at zero current amount; and a test
  that computing progress does not mutate the entity's stored amounts.
- `FinancialGoalApiIntegrationTest` (`@SpringBootTest` +
  `@AutoConfigureMockMvc` + Testcontainers `PostgreSQLContainer`, real Flyway
  migration run, 21 cases): create/retrieve with computed `remainingAmount`/
  `progressPercentage` in the response; lowercase-currency normalization;
  progress bounded at 100% with a negative `remainingAmount` for an
  overachieved goal; rejection of zero/negative `targetAmount`, negative
  `currentAmount`, blank name, malformed currency, a past `targetDate`
  (today itself is accepted), zero/negative `priority`, excessive fractional
  scale, and precision overflow; unknown-household 404 on create and on get;
  cross-household get returns `FINANCIAL_GOAL_NOT_FOUND` without disclosing
  the record; empty list for a new household; list ordered by ascending
  priority regardless of creation order; household isolation between two
  households' goal lists; and a check that creating a goal does not alter an
  existing asset in the same household.
- Full suite: `./verify.sh` — 187 tests (33 new: 12 unit + 21 integration),
  0 failures, Flyway migrating a clean database through V1 -> V5
  automatically.
- Exercised the primary flow manually: started a disposable Postgres
  container (`docker run ... postgres:16-alpine`, since no local Postgres or
  `docker compose` stack was already running in this environment) and ran
  `./mvnw spring-boot:run` against it. Created a household, created a
  "Retirement" goal (`targetAmount` 1,000,000.00 PHP, `currentAmount`
  50,000.00, lowercase `currency` "php"), confirmed the response normalized
  currency to "PHP" and computed `remainingAmount` 950,000.00 and
  `progressPercentage` 5.00; retrieved the goal by ID and via the list
  endpoint; confirmed a blank name and a past `targetDate` both return
  `VALIDATION_FAILED`/400; confirmed an unknown household returns
  `HOUSEHOLD_NOT_FOUND`/404; tore the container down afterward.

**Decisions**

- List goals ordered by ascending `priority` (then `createdAt`, then `id`),
  not creation order. The product brief defines priority as "lower numbers
  are higher priority" but does not specify list ordering; every existing
  list endpoint (`Asset`, `Liability`, `IncomeStream`, `Obligation`) orders
  by creation order because none of those domains has a priority field.
  Ordering goals by priority is the one ordering that makes the response
  directly useful for the goal's own purpose (seeing what matters most
  first) without a client-side sort, and Task 008-Plan-vs-Actual is not
  depended on for this. Flagged as an assumption for Product Owner review
  since it is not explicit in the brief.
- `remainingAmount` is left unclamped (can go negative for an overachieved
  goal) while only `progressPercentage` is bounded to [0, 100]. The brief's
  PD-002 only calls the percentage "bounded"; clamping the remaining amount
  too would hide that a goal has been exceeded, which is itself useful,
  auditable information.
- Did not add a goal-specific "invalid value" exception (unlike `Asset`'s
  `InvalidAssetValueException` for its cross-field `planningValue <=
  estimatedValue` rule): every Goal validation rule in the brief is a
  single-field constraint expressible in Bean Validation on
  `CreateFinancialGoalRequest`, so no service-level cross-field check exists
  to need one (AGENTS.md principle 9: don't add infrastructure before it's
  required).

**Assumptions**

- **Flagged brief/domain conflict, resolved in favor of domain sense:** the
  product brief's Scope section says validation should enforce "non-future
  target dates," but a financial goal's target date is, by definition and by
  the roadmap's own examples ("delays a mortgage, education, or retirement
  goal"), a date the household is planning *toward* — i.e. it must not
  already be in the past. Implementing "non-future" literally (target date
  must be today or earlier) would make it impossible to create a goal for
  any real future objective. Implemented `@FutureOrPresent` (target date
  today or later) instead of `@PastOrPresent`. This is very likely a
  drafting inversion in the brief rather than an intentional constraint; per
  this task's unattended-worker instructions, proceeding with the smallest
  safe (domain-sensible) assumption rather than stalling, and recording it
  here plus in the product brief's acceptance-evidence section for Codex to
  confirm or correct during review.
- List ordering by priority (see Decisions above) is an assumption, not a
  stated requirement; Codex may prefer creation order instead.
- No goal-to-snapshot, goal-to-obligation, or goal-to-plan-versus-actual
  relationship was added; `currentAmount` is entirely caller-supplied, per
  the brief's explicit out-of-scope boundary.

**Open questions**

- Should the brief's "non-future target dates" wording (see Assumptions) be
  corrected to "non-past" / "future-or-present" so future workers don't
  re-derive this from scratch?
- Same as the product brief: whether goals later need contributions,
  milestones, person ownership, multiple metric types, or snapshot-derived
  progress are all deferred.

**Recommended next task**

- Correct the product brief's target-date wording (see Open questions), and
  consider a small `docs/`/`AGENTS.md` note that acceptance-criteria wording
  conflicts found by an unattended worker should be called out explicitly in
  the PR description (this task's "System evolution" candidate — see below).

**System evolution candidate**

- This is the first task where an unattended worker found a plausible
  wording inversion in an already-`READY` product brief with no one to ask.
  `agent/collaboration-workflow.md` already tells an implementer to record
  such an assumption and keep going, which is what happened here — no
  process gap found beyond that. Not proposing a rule change; recording this
  here in case a second occurrence suggests one (e.g. a lightweight
  brief-linting pass in Codex's framing step).

### 2026-09-05 — Task 008: Plan-versus-Actual Snapshot Analysis

**Changed**

- Added a new, additive module rather than extending
  `FinancialSnapshotService`/`FinancialSnapshotController`, to keep this
  task's diff isolated from Task 006/007 files per the task file's
  independence constraint:
  - Domain (`backend/src/main/java/com/waypoint/household/`):
    `PlannedCurrencyTotals` (currency, `assetTotal`, `liabilityTotal`,
    `netWorth` — the caller's disposable plan input, never persisted),
    `VarianceDirection` (`ABOVE_PLAN`/`BELOW_PLAN`/`ON_PLAN`, deliberately
    neutral per PD-002), `PlanVersusActualVariance` (planned, actual,
    signed `actual - planned` variance, direction), `CurrencyPlanVersusActual`
    (currency + one `PlanVersusActualVariance` per measure), and
    `PlanVersusActualAnalysis` (the source `FinancialSnapshot` entity +
    `List<CurrencyPlanVersusActual>`).
  - `PlanVersusActualService`: a new `@Transactional(readOnly = true)`
    service that depends only on the existing `FinancialSnapshotService`
    (reused for household/snapshot lookup, ownership enforcement, and
    `totalsByCurrency`, so no new repository dependency was needed). It
    normalizes each planned currency (`trim().toUpperCase()`, matching
    `AssetService`/`ObligationService`'s existing normalization
    convention) before validating and diffing against actuals, so a plan
    currency casing (e.g. `"php"`) still matches the snapshot's stored
    `"PHP"` totals. A planned currency absent from the snapshot is
    compared against zero actuals rather than rejected or omitted.
  - Validation lives in the service, not bean validation, for the two
    cross-field/cross-record rules that field-level annotations can't
    express: duplicate currencies across the planned list, and planned
    `netWorth` not equal to `assetTotal - liabilityTotal`. Non-negative
    `assetTotal`/`liabilityTotal` and required/format checks (3-letter
    currency, non-null, `Digits(17,2)`) are enforced at the DTO layer via
    Bean Validation, matching `CreateAssetRequest`/`CreateObligationRequest`.
    All are surfaced through one new `InvalidPlanException`, wired into
    `ApiExceptionHandler` as `VALIDATION_FAILED` (400) alongside the
    existing `InvalidAssetValueException`/`InvalidScheduleException`
    precedent.
  - `PlanVersusActualController`: `POST
    /api/households/{householdId}/financial-snapshots/{snapshotId}/plan-comparison`,
    returning `200 OK` (not `201`, since nothing is created). `POST` was
    chosen over `GET` because the planned-measures list has no natural
    query-string encoding at this size/shape (unlike Task 006's two-UUID
    `GET .../comparison`), while still being read-only.
  - DTOs under `com.waypoint.household.web.dto`:
    `PlannedCurrencyTotalsRequest`, `PlanVersusActualRequest`
    (`@NotEmpty @Valid List<PlannedCurrencyTotalsRequest> plannedMeasures`),
    `VarianceResponse`, `CurrencyPlanVersusActualResponse`,
    `PlanVersusActualResponse` (reuses the existing
    `FinancialSnapshotSummaryResponse` for the source snapshot, consistent
    with Task 006's comparison response).
  - No Flyway migration and no new repository: this feature persists
    nothing (matches PD-001), so there is no schema change and no
    migration-version-collision risk with the other concurrently
    `IN_PROGRESS` tasks.

**Tests**

- `PlanVersusActualServiceTest` (11 new Mockito unit tests, mocking only
  `FinancialSnapshotService`): household-not-found and snapshot-not-found
  both propagate unchanged from the snapshot lookup; duplicate planned
  currency (case-insensitive: `"PHP"` + `"php"`) rejected before the
  snapshot is even read; negative planned `assetTotal`/`liabilityTotal`
  rejected; inconsistent planned `netWorth` rejected; `ABOVE_PLAN`,
  `BELOW_PLAN`, and `ON_PLAN` directions computed correctly per measure;
  a planned currency absent from the snapshot compared against zero
  actual; currency-case normalization when matching a planned entry to
  the snapshot's actual totals; and identical repeated calls producing an
  equal result while only ever invoking `getSnapshot` (no other
  interaction with the mocked collaborator, confirming no incidental
  persistence path exists).
- `PlanVersusActualApiIntegrationTest` (10 new
  `@SpringBootTest`/Testcontainers integration tests): above/below-plan
  variances across two currencies including one absent from the snapshot;
  identical repeated requests producing byte-identical responses, then
  independently re-reading the snapshot to confirm its line items are
  unchanged; unknown household; unknown snapshot; cross-household
  snapshot ownership rejected (404, not disclosing the other household's
  snapshot); empty `plannedMeasures`; duplicate planned currencies;
  negative planned totals; inconsistent planned net worth; and missing
  required fields on a planned measure.
- Full suite: `./verify.sh` — 175 tests (21 new: 11 unit + 10 integration),
  0 failures.
- Exercised the primary flow manually. Did **not** reuse the project's
  shared named Postgres volume (`waypoint-postgres-data`) for this: `docker
  compose -p waypoint-task008 up --build` briefly attached to that exact
  volume before I noticed the "volume already exists but was created for
  project 'waypoint'" warning in the compose output — the volume predates
  this session (created 2026-09-02) and may hold genuine household data
  from prior manual use, so I stopped and removed the containers
  (`docker compose ... stop` then `down`, no `-v`) before any request
  reached the app; nothing was read from or written to that volume. Reran
  the smoke test instead against a fully throwaway, anonymously-named
  `docker run postgres:16-alpine` container plus `./mvnw spring-boot:run`
  on the host against it, then removed that container afterward. Created
  a household, a PHP asset (120.00) and liability (30.00), and a snapshot;
  posted a plan with `PHP` (planned 100.00/30.00/70.00) and `USD` (planned
  50.00/0/50.00, absent from the snapshot); confirmed `PHP` returned
  `ABOVE_PLAN` variances (+20.00 asset, +20.00 net worth) and `ON_PLAN`
  liability, `USD` returned `BELOW_PLAN` (actual 0, variance -50.00);
  confirmed inconsistent planned net worth returned 400; and re-read the
  snapshot afterward to confirm its line items and totals were unchanged.

**Decisions**

- Kept this feature in new files rather than extending
  `FinancialSnapshotService`/`FinancialSnapshotController`/their tests,
  even though Task 006's comparison feature is the closer precedent and
  reuse would have been technically simpler. The task file's independence
  constraint is scoped to Task 007 (Goals), not Task 006, but new files
  minimize the diff's overlap surface with any concurrently `IN_PROGRESS`
  task touching snapshot files, at the cost of one extra service/controller
  pair. `ApiExceptionHandler` was still edited (unavoidable — it is the
  one shared cross-cutting registry for every domain exception), but that
  edit is a pure addition (one import, one handler method) with low
  collision risk.
- Represented planned input as a full `CurrencyTotals`-shaped triple
  (`assetTotal`, `liabilityTotal`, `netWorth`) rather than just
  `assetTotal`/`liabilityTotal` with `netWorth` derived server-side: the
  brief's acceptance criteria explicitly calls for validating "internally
  consistent planned net worth," which only makes sense if the caller
  states it explicitly and the server checks it, rather than the server
  computing it and there being nothing to validate.
- `netWorth` has no `@DecimalMin("0")` (unlike `assetTotal`/
  `liabilityTotal`): a household's planned net worth can legitimately be
  negative if planned liabilities exceed planned assets, so only the
  *inputs* to that subtraction are constrained to be non-negative, per the
  brief's "non-negative planned totals" language (plural — the two totals,
  not net worth).
- `VarianceDirection` has three states, not two: `ON_PLAN` avoids forcing
  an exact-zero variance into an arbitrary `ABOVE_PLAN`/`BELOW_PLAN` bucket,
  and keeps the enum a pure function of `variance.signum()`.

**Assumptions**

- A plan need not cover every currency present in the snapshot, and the
  snapshot need not contain every currency in the plan — the response is
  scoped to exactly the currencies the caller supplied (matching the
  acceptance criterion "for every supplied currency"), with a currency
  present in the plan but not the snapshot compared against zero actuals
  (mirroring Task 006's precedent for a currency present in only one of
  two compared snapshots).
- "Complete measures" (brief, Scope > In scope) means each planned
  currency entry must supply all three measures (`assetTotal`,
  `liabilityTotal`, `netWorth`) — enforced via `@NotNull` per field —
  rather than requiring the plan to cover every currency the snapshot
  itself has.
- Currency codes in the request are normalized (`trim().toUpperCase()`)
  before duplicate-detection and before matching against the snapshot's
  stored (already-uppercase) currencies, consistent with how
  `AssetService`/`LiabilityService`/`ObligationService` normalize currency
  on write. Duplicate detection therefore treats `"PHP"` and `"php"` as
  the same planned currency.

**Open questions**

- Same shared-volume issue Task 006 already flagged under "System
  evolution" (`agent/implementation-log.md`, 2026-09-05, Task 006) recurred
  here, almost silently, in a second worker session: `docker-compose.yml`
  hardcodes `volumes.waypoint-postgres-data.name: waypoint-postgres-data`,
  so it is not scoped per Compose project and every `docker compose ...
  up` on this machine attaches to the same physical volume regardless of
  `-p`. This is no longer a hypothetical risk — it has now caused two
  independent worker sessions to nearly run manual smoke-test data against
  what may be genuine household data. Recommend this become its own small,
  explicit follow-up task (not folded into either feature): either
  parameterize the volume name per Compose project/environment variable,
  or add an explicit, documented scratch-volume convention (e.g. in
  `AGENTS.md` or `README.md`) that every future manual-verification step
  is required to follow. Flagging again here rather than fixing it
  unilaterally, since it is repository-wide infrastructure outside this
  task's scope and both Task 006 and this task independently deferred it
  to a human/Product-Owner decision.
- Same deferred items as the product brief: whether future plans should
  gain persistence, effective dates, provenance, or approval state; which
  measures beyond asset/liability/net-worth totals should be plannable;
  and whether any measure should get a household-approved
  favorable/unfavorable rule.

**Recommended next task**

- A small, standalone follow-up to fix or document the shared
  `waypoint-postgres-data` Compose volume (see "Open questions" above),
  since it has now surfaced independently in two consecutive tasks.
- Otherwise, per the product brief's follow-up opportunities: persisted/
  versioned plans, or goal-aware plan-versus-actual once Task 007's Goals
  domain lands.

### 2026-09-05 — First live run of the automated pipeline (Task 006), three bugs found and fixed

**Changed**

- Ran `agent/automation/orchestrator.sh` for real against Task 006 (read-only
  financial snapshot comparison, `agent/product/snapshot-comparison/
  product-brief.md`) end to end: dispatched an unattended `claude --bg`
  worker in its own worktree, which implemented the feature, ran
  `./verify.sh` (154 tests, 0 failures), and opened PR #7; then ran the
  review/merge phase, which had Codex review the PR unattended, record
  acceptance in the product brief, and — once the required `verify` check
  went green — merge PR #7 automatically with no human step. This is the
  first task to go through the full pipeline described in `agent/
  collaboration-workflow.md` -> "Automated pipeline".
- Fixed three real bugs this first live run surfaced in
  `agent/automation/orchestrator.sh`, none of which were caught by the
  syntax-only check in the previous entry:
  1. `dispatch_worker()` called `claude -p "$prompt" ... --bg`; `claude`
     rejects combining `--bg` with `-p`/`--print` outright ("the job would
     be unattachable"). Fixed to pass the prompt positionally with `--bg`
     alone.
  2. That same call's session-id extraction assumed the last non-empty line
     of `claude --bg`'s output was the id; the real output is `backgrounded
     · <id> · <name>` on line 1, followed by a few `claude <subcommand> <id>
     ...` help lines — the "last line" was actually one of those help lines,
     never the id. Fixed to parse field 2 of line 1.
  3. `check_migration_collision()` compared a branch's *entire* migration
     file list against `main`'s and treated any overlap as a collision —
     which is every normal branch, always, since a branch inherits all of
     main's existing migrations by definition. This would have silently
     stalled every single future task at the merge step. Fixed to diff
     against the merge-base and only compare versions the branch actually
     introduces.
  - Also discovered (not a bug, but worth recording): unattended
    `--permission-mode bypassPermissions --bg` requires a one-time
    interactive disclaimer acceptance per machine
    (`claude --dangerously-skip-permissions`, run once in a real terminal —
    the harness's own non-interactive command execution doesn't provide a
    real TTY and can't get past this, by design); and `codex exec`'s own
    review commit retriggers the required CI check, so a review pass isn't
    "done" the instant Codex responds — the orchestrator only merges once
    `gh pr checks` reports the *current* head sha's check as `SUCCESS`,
    which already handled this correctly.

**Tests**

- No `./verify.sh` run for this change itself (touches only
  `agent/automation/orchestrator.sh`, not application code). The real test
  *was* the live run: Task 006's worker's own `./verify.sh` passed (154
  tests, 18 new, 0 failures), and the required CI `verify` check passed
  twice (once per PR #7 commit) before merge.

**Decisions**

- Left the state-tracking fix (record the review's *post*-commit head sha,
  not the pre-review one) in the same pass as the migration-collision fix,
  since both were found from the same live run and an incomplete fix would
  have left the pipeline re-reviewing its own review commit every tick.
- Did not add automated tests for `orchestrator.sh` itself (e.g. a bats
  suite) in this pass — the collision-check fix and the flag fixes were
  each verified against the real Task 006 branch/PR directly, which is
  stronger evidence than a synthetic test would be for this first pass, but
  a regression suite is a real gap now that the script has already shipped
  one bug per major function.

**Assumptions**

- Assuming the `claude --bg` output format (`backgrounded · <id> · <name>`
  plus help lines) is stable across versions; a future `claude` CLI update
  could change this format again the same way `codex exec review`'s
  argument handling turned out to differ from what the collaboration
  workflow originally documented.

**Open questions**

- Whether `orchestrator.sh` needs its own test harness (mocking `claude`/
  `codex`/`gh`) given it has now shipped bugs in dispatch, review-state
  tracking, and merge-safety logic — each caught only by an actual live run,
  not by `bash -n`.
- Whether the "wait for a fresh CI run after Codex's own commit" behavior
  should be made explicit/logged in `try_merge()` rather than implicitly
  handled by the existing check-state comparison, since it was a source of
  real confusion while diagnosing this run.

**Recommended next task**

- Frame Task 007 with Codex and let it flow through the now-verified
  pipeline unattended, ideally without a human intervening mid-run this
  time, as the real test of whether these fixes hold.
- Consider a minimal automated test harness for `agent/automation/
  orchestrator.sh`'s pure-logic pieces (`check_migration_collision`,
  frontmatter parsing) so the next bug is caught before a live task branch
  finds it.

### 2026-09-05 — Task 006: Financial Snapshot Comparison

**Changed**

- Added `FinancialSnapshotService.compareSnapshots(householdId, earlierSnapshotId, laterSnapshotId)`
  (`backend/src/main/java/com/waypoint/household/FinancialSnapshotService.java`):
  validates the household exists, rejects `earlierSnapshotId.equals(laterSnapshotId)`
  before touching either snapshot, then loads both snapshots scoped to the
  household (reusing `findByIdAndHousehold_Id`, so a snapshot belonging to
  another household 404s exactly like the existing single-snapshot `GET`).
  Reuses the existing `toDetail`/`computeTotals` path to get each snapshot's
  `totalsByCurrency`, then unions the two currency sets (a currency present
  in only one snapshot is compared against a zero `CurrencyTotals` rather
  than omitted) and computes later-minus-earlier deltas per currency.
- Added `CurrencyTotalsDelta` (currency, `assetTotalDelta`,
  `liabilityTotalDelta`, `netWorthDelta`) and `FinancialSnapshotComparison`
  (earlier snapshot, later snapshot, `List<CurrencyTotalsDelta>`) as plain
  domain records, and `IdenticalSnapshotComparisonException`.
- Added `GET /api/households/{householdId}/financial-snapshots/comparison?
  earlierSnapshotId=...&laterSnapshotId=...` to `FinancialSnapshotController`.
  It is a literal-path sibling of the existing `GET /{snapshotId}`; Spring's
  path-pattern specificity ranking (a literal segment beats a `{variable}`
  segment) resolves the two unambiguously, so `/comparison` never gets
  swallowed by the `{snapshotId}` route — confirmed empirically via the
  manual smoke test below, not just assumed.
- Added `FinancialSnapshotSummaryResponse` (id, `asOfDate`, `capturedAt` —
  deliberately omits line items and totals, since the brief asks the
  comparison to identify the two source snapshots, not repeat their full
  detail), `CurrencyTotalsDeltaResponse`, and
  `FinancialSnapshotComparisonResponse` DTOs under
  `com.waypoint.household.web.dto`.
- Wired `IdenticalSnapshotComparisonException` into `ApiExceptionHandler` as
  `VALIDATION_FAILED` (400, matching `InvalidScheduleException`'s
  precedent for a request-shape rule rather than a not-found case). Also
  added a `MissingServletRequestParameterException` handler (also
  `VALIDATION_FAILED` 400) — the two new query parameters are the first
  required `@RequestParam`s anywhere in this codebase, and without this
  handler a missing one would fall through to Spring's default
  unstructured error body instead of the repository's established
  structured shape.
- No Flyway migration: this feature persists nothing (PD-002), so there was
  no schema change and no migration-version-collision risk with the other
  concurrently `IN_PROGRESS` tasks.

**Tests**

- `FinancialSnapshotServiceTest` (7 new Mockito unit tests): household-not-
  found; rejecting a snapshot compared against itself; not-found when
  either snapshot doesn't belong to the household (checked independently
  for earlier and later); signed later-minus-earlier deltas across a
  three-line-item, two-currency scenario (including a currency —
  USD — present only in the later snapshot, asserting it still surfaces
  with the earlier side treated as zero rather than being dropped); and
  all-zero deltas when both snapshots' totals are equal. Entities built
  directly (not through the repository) needed `id` set via
  `ReflectionTestUtils.setField` — `@GeneratedValue` only assigns an id on
  actual persistence, and two unpersisted `FinancialSnapshot`s with `null`
  ids collided as the same mocked-repository key, which the first test run
  caught (deltas came back zero because both sides silently read the later
  snapshot's line items).
- `FinancialSnapshotApiIntegrationTest` (11 new
  `@SpringBootTest`/Testcontainers integration tests): valid comparison
  with mixed asset/liability changes; a negative net-worth delta when a
  later snapshot is smaller (checked from two different earlier points);
  a currency present in only one snapshot; zero deltas for two snapshots
  with identical contents; no persistence/mutation (comparing twice, then
  independently confirming the snapshot list count and the earlier
  snapshot's own line items are unchanged); self-comparison rejected;
  unknown household; missing earlier snapshot; missing later snapshot;
  cross-household comparison rejected without disclosing the other
  household's snapshot; and missing required query parameters.
- Full suite: `./verify.sh` — 154 tests (18 new: 7 unit + 11 integration),
  0 failures. Docker Desktop was not running at the start of this session
  (required for the Testcontainers-backed integration tests); started it
  (`open -a Docker`) and confirmed `docker info` succeeded before running
  `./verify.sh`.
- Exercised the primary flow manually against `docker compose up --build`.
  Deliberately did **not** reuse the project's shared named Postgres volume
  (`waypoint-postgres-data`, hardcoded in `docker-compose.yml` and shared
  across every worktree/compose invocation on this machine regardless of
  Compose project name) for this smoke test, since this is a real household
  financial application and that volume may hold genuine Ralph/wife data
  from prior manual sessions — writing scratch test households into it
  would be indistinguishable from real records once mixed in. Instead ran
  `docker compose -p waypoint-task006 -f docker-compose.yml -f
  <scratch-override>.yml up -d` with an override that points `postgres` at
  a throwaway named volume, then tore both the stack and that scratch
  volume down afterward (`down -v`, safe because the override volume is
  exclusive to this smoke test). Created a household, a PHP asset dated
  2026-08-01, and an earlier snapshot as of 2026-08-05 (net worth 1000.00);
  added a second PHP asset (250.00) and a PHP liability (100.00), then a
  later snapshot as of 2026-09-05; confirmed the comparison returned
  `assetTotalDelta: 250.00`, `liabilityTotalDelta: 100.00`,
  `netWorthDelta: 150.00` for PHP, both snapshots' ids/dates, and 400/404
  for self-comparison, a missing snapshot, a missing household, and missing
  query parameters.

**Decisions**

- Chose `GET .../financial-snapshots/comparison?earlierSnapshotId=&
  laterSnapshotId=` over a new top-level resource (e.g.
  `financial-snapshot-comparisons`) or a `POST` with a body: it's a
  read-only computed view over two already-identified snapshots, so `GET`
  with explicit query parameters fits the operation and keeps it visibly
  scoped under the snapshot collection it reads from, consistent with
  PD-002 (never persisted, so it's not really its own resource).
- Named the query parameters `earlierSnapshotId`/`laterSnapshotId` (not,
  say, `fromId`/`toId`) to match PD-001's explicit-direction language
  verbatim in the API surface, so the direction is unambiguous to a caller
  reading the request alone.
- Returned only `id`/`asOfDate`/`capturedAt` for each source snapshot
  (`FinancialSnapshotSummaryResponse`), not the full `FinancialSnapshotResponse`
  with line items and absolute totals: the brief asks for "both source
  snapshot dates/identifiers," and the deltas already carry the
  meaningful change; repeating both full snapshots would restate
  already-available `GET /{snapshotId}` data and risks the response being
  mistaken for a merged/aggregate record.
- Validated identical-snapshot-id before hitting the database for either
  snapshot: it's a pure request-shape check independent of what exists,
  so it can reject cheaply and consistently regardless of whether the
  (single) id happens to exist.
- Reused `FinancialSnapshotService` rather than adding a new service class:
  the comparison is built entirely from the same `toDetail`/`computeTotals`
  logic this service already owns, and PD-002 keeps it a pure on-demand
  read with no new persistence dependencies to justify a separate service.

**Assumptions**

- "Every supported snapshot financial measure" (per the brief's acceptance
  criteria) means the same per-currency `assetTotal`/`liabilityTotal`/
  `netWorth` triple `FinancialSnapshotResponse` already exposes as
  `totalsByCurrency` — there is no other snapshot-level measure in the
  current domain model to compare.
- A currency present in only one of the two snapshots is compared against
  zero (surfaced as a full-magnitude delta) rather than omitted from the
  response; omitting it would silently hide a real change (e.g. a household
  opening its first USD account between snapshots).
- `GET` with required query parameters, rather than a request body, is an
  acceptable shape for a read-only two-identifier lookup in this API,
  consistent with how the rest of the API already uses `GET` for reads and
  reserves bodies for `POST`/create.

**Open questions**

- `docker-compose.yml`'s Postgres volume is declared with a fixed
  top-level `name: waypoint-postgres-data`, so it is shared by every
  Compose invocation on this machine regardless of `-p`/project name —
  including the orchestrator's future parallel worker sessions once they
  start exercising the app manually, not just this one. Worth a small,
  explicit follow-up (flagged here per `agent/collaboration-workflow.md`'s
  "System evolution", not silently fixed as part of this task): either
  parameterize the volume name per Compose project, or add a documented
  convention (e.g. in `README.md` or `AGENTS.md`) for agents to use an
  isolated scratch volume/override for manual smoke testing instead of the
  default named volume, the way this task's manual verification did.
- Same open items as the product brief: whether a future presentation
  layer needs percentage changes or richer measures, and whether
  plan-versus-actual analysis should reuse this response shape, remain
  deferred.
- `agent/tasks/README.md` ("Who writes what") states
  `agent/automation/orchestrator.sh` "owns every other status transition
  ... nothing else should edit [`status`, `worktree`, `session`, `pr`,
  `claimed_at`, `fix_rounds`] directly," but `orchestrator.sh` itself never
  sets a task file's `status` to `IN_REVIEW` (grepped for it; the only hit
  is in `agent/automation/worker-prompt.md`'s own instruction to the
  worker). This worker followed `worker-prompt.md`/its task instructions
  and set `status: IN_REVIEW` directly, since otherwise the task would
  never leave `IN_PROGRESS` and the orchestrator would never pick it up for
  review. Flagging rather than silently resolving: either
  `orchestrator.sh` should perform that transition itself after detecting
  a pushed PR (matching README's stated ownership), or README.md's "who
  writes what" table should be corrected to say the worker sets `status:
  IN_REVIEW` as part of its handoff, with the orchestrator owning the
  remaining fields.

**Recommended next task**

- Goals domain (target metrics, target dates, progress) or plan-versus-
  actual analysis, per the product brief's follow-up opportunities — both
  now have financial snapshots and this comparison to build on.

### 2026-09-05 — Parallel task queue and automated Plan/Implement/Validate pipeline

**Changed**

- Replaced the single `agent/current-task.md` (one active task at a time)
  with `agent/tasks/<NNN>-<feature-slug>.md`, a small backlog format with a
  `status` lifecycle (`QUEUED` -> `IN_PROGRESS` -> `IN_REVIEW` -> `STALLED`
  or `ACCEPTED` -> `MERGED`) — see `agent/tasks/README.md`.
- Added `agent/automation/orchestrator.sh`: run repeatedly on a schedule
  (local cron, not itself a loop), it claims up to 3 `QUEUED` tasks at once,
  dispatches an unattended `claude -p ... --permission-mode
  bypassPermissions --bg` worker into a dedicated git worktree for each, then
  separately reviews open `task/*` PRs with an unattended `codex exec
  --dangerously-bypass-approvals-and-sandbox` invocation and merges
  automatically once Codex records acceptance and the required `verify`
  check is green. A `BLOCKING` review dispatches a bounded number of
  automatic fix rounds (default 2) before marking the task `STALLED` for a
  human. All of the orchestrator's own git operations run in a dedicated
  clone under `../waypoint-orchestrator/`, never in a human's interactive
  checkout. See `agent/automation/README.md` and `agent/automation/
  worker-prompt.md` / `review-prompt.md`.
- Added a migration-version-collision check before merge (compares a
  branch's new Flyway migration version numbers against `main`'s), since
  parallel tasks can independently add a colliding `V<n>` migration that git
  itself would merge cleanly (different filenames) but Flyway would then
  reject at runtime; a collision stalls the task instead of merging.
- Rewrote `AGENTS.md`, `agent/collaboration-workflow.md` (new "Automated
  pipeline" section), `agent/roles/product-owner.md`, `.claude/commands/
  codex.md`, `.claude/commands/prime.md`, `README.md`, `.github/
  pull_request_template.md`, and `agent/templates/implementation-plan.md` to
  point at `agent/tasks/` instead of `agent/current-task.md`, and to
  document the pipeline's two deliberate departures from the previous
  workflow: orchestrator-spawned sessions bypass permissions/sandboxing
  (nobody is present to approve a prompt in an unattended run — scoped
  strictly to sessions the orchestrator itself spawns, never to an
  interactive session), and merging is now fully automatic once Codex
  accepts and the required check is green (previously always required a
  human, Codex, or an explicit Claude Code ask). Left the historical
  `agent/current-task.md` references inside `agent/implementation-log.md`
  and existing `agent/product/*/product-brief.md` files untouched — they are
  accurate history of the workflow as it existed at the time, not live
  documentation.
- Fixed `.claude/commands/codex.md`'s `review` mode example, which
  documented `codex review --base main "<prompt>"` /
  `codex exec review --base main "<prompt>"`; both actually fail on the
  installed codex-cli (0.152.1) with `error: the argument '--base <BRANCH>'
  cannot be used with '[PROMPT]'` — a custom prompt can't be combined with
  `--base`/`--commit` on this version. Replaced the example with the
  working diff-yourself workaround (`codex exec` plus `gh pr diff`, which
  `agent/automation/review-prompt.md` also uses) and noted the CLI
  limitation inline so it isn't rediscovered silently again.

**Tests**

- `bash -n agent/automation/orchestrator.sh` — syntax check only; no
  `./verify.sh` run, since this task touched no application code.
- Did not run the orchestrator end-to-end against a real queued task: doing
  so has real side effects (worktrees, pushes, PRs, a merge to `main`), and
  no task is queued yet since the user hasn't chatted with Codex about one.
  `agent/automation/README.md` explicitly recommends one supervised manual
  run before installing the cron entry, specifically to confirm the
  `claude --bg` session-id parsing in `dispatch_worker()` matches what the
  installed `claude` version actually prints — the one piece this pass
  couldn't verify without a real task to dispatch.

**Decisions**

- Kept the orchestrator's own git state (a full clone plus per-task
  worktrees under `../waypoint-orchestrator/`) entirely separate from the
  user's interactive checkout, so a cron-triggered run can never mutate
  whatever branch a human happens to have checked out.
- Used a plain `mkdir`-based lock (`agent/automation/state/
  .orchestrator.lock`) rather than `flock`, since `flock` isn't shipped on
  macOS/BSD by default and this needed to work without an extra dependency.
- Bounded concurrency to 3 and automatic fix rounds to 2, and added the
  migration-collision check, specifically so "parallel" and "automatic"
  don't quietly trade away correctness — these are the concrete safety nets
  called for in `agent/collaboration-workflow.md` -> "Automated pipeline".
- Did not attempt to make Codex reviewable in GitHub Actions instead of
  locally: this machine's `codex` CLI authenticates via a ChatGPT
  subscription, not an API key, so the orchestrator has to run somewhere
  that auth already lives — a local cron job — not in CI.

**Assumptions**

- `claude --bg ... --permission-mode bypassPermissions` and `codex exec
  --dangerously-bypass-approvals-and-sandbox`, run only inside the
  orchestrator's own spawned sessions (each confined to one task's
  worktree/branch), is an acceptable, explicitly-authorized loosening of
  this repo's normal never-bypass-sandbox rule for this pipeline
  specifically — confirmed with the user directly, not inferred.
- Fully automatic merge (no human checkpoint at all once Codex accepts and
  `verify` is green) is what the user asked for, specifically to see a
  mature, highly-parallel pipeline in practice — confirmed with the user
  directly given this removes the workflow's previous single human
  safety gate.
- A local cron job (vs. a persistent `/loop` session) is an acceptable
  reliability tradeoff: the pipeline only runs while this machine is on and
  this user's cron/launchd context is active — confirmed with the user
  directly.

**Open questions**

- Whether the `claude --bg` output actually parses the way
  `dispatch_worker()` assumes is unverified until a real task runs through
  it.
- Whether GitHub's branch protection should require the orchestrator's own
  commits (task-file claims, review findings) to also pass `verify`, or
  whether direct-to-`main` process bookkeeping commits should stay exempt
  as they always have been for `agent/current-task.md`-era Codex commits.
- Whether 3 concurrent tasks and 2 fix rounds are the right defaults once
  this has run against real tasks, versus tuned from observed throughput
  and stall rate.

**Recommended next task**

- Have the user and Codex frame a first real task into `agent/tasks/` as
  `QUEUED`, run `agent/automation/orchestrator.sh` once by hand to watch it
  dispatch and verify the session-id parsing, then install the cron entry
  from `agent/automation/README.md` once that looks right.

### 2026-09-04 — Task 005 product acceptance

**Changed**

- Product Owner review accepted Task 005 against the financial snapshots
  product brief and checked all implementation criteria.
- Confirmed the implementation remains within scope: immutable copied
  balance-sheet observations, per-currency totals, no FX/cash-flow expansion,
  and no seeded household data.

**Tests**

- PR #4 records `./verify.sh` passing with 137 tests and zero failures; the
  required CI `verify` check is recorded as passing.
- An independent local `./verify.sh` rerun was attempted. It was blocked by
  the host Java 26 Mockito Byte Buddy self-attachment failure and unavailable
  Docker/Testcontainers runtime, so no contradictory application failure was
  observed.

**Decisions**

- No implementation changes were required during acceptance.

**Assumptions**

- The Java 21/Docker verification recorded by the implementation owner and
  the green required CI check are the authoritative execution evidence for
  this task; the local host environment is not the declared runtime.

**Open questions**

- Future work remains as recorded in the brief: source valuation history,
  income/obligation schedule snapshots, deterministic cash-flow normalization,
  and historical comparison.

**Recommended next task**

- Frame a narrow snapshot-comparison or plan-versus-actual increment, or
  proceed to Phase 5 goals.

### 2026-09-04 — Task 005: Financial Position Snapshots

**Changed**

- `task/005-financial-snapshots` was cut before Task 003 (`verify.sh`, the
  required CI check) and Task 004 (income/obligations) merged to `main`.
  Merged `origin/main` into the task branch before implementing; conflicts
  were `agent/current-task.md` (stale Task 004 content vs. the
  already-defined Task 005 task, resolved in favor of Task 005 since Task
  004 was already accepted and merged) and `agent/implementation-log.md`
  (both sides had added genuine new entries — Codex's Task 005 framing entry
  and Task 004 acceptance entry vs. this log's own Task 004 implementation
  entry — resolved by keeping both, newest first).
- Added `FinancialSnapshot` (header: household, `asOfDate`, `capturedAt`,
  `sourceType`), `SnapshotAssetLineItem`, and `SnapshotLiabilityLineItem`
  JPA entities (`backend/src/main/java/com/waypoint/household/`).
  `FinancialSnapshot` has no `updatedAt`: per PD-003 (create-only,
  immutable), there is no code path that would ever set one. Each line item
  stores `sourceAssetId`/`sourceLiabilityId` as a plain retained UUID column
  (no DB foreign key to `assets`/`liabilities`), plus a copied name, type,
  currency, source date (`valuedAt`/`balanceAsOf` at capture time), and
  value (`planningValue`/`outstandingBalance` at capture time); all line-item
  columns are `updatable = false`.
- Added `FinancialSnapshotRepository`, `SnapshotAssetLineItemRepository`,
  `SnapshotLiabilityLineItemRepository` (household/snapshot-scoped queries,
  matching the existing `findBy...OrderBy...AscIdAsc` /
  `findByIdAndHousehold_Id` pattern), `FinancialSnapshotNotFoundException`,
  and `FinancialSnapshotService`. The service reads a household's current
  `Asset`/`Liability` rows directly via their existing repositories
  (no new query methods added to `AssetRepository`/`LiabilityRepository`),
  filters in Java by `!sourceDate.isAfter(asOfDate)` (boundary-inclusive),
  copies eligible rows into line items in one transaction, and computes
  per-currency totals.
- Added `CurrencyTotals` (currency, assetTotal, liabilityTotal, netWorth)
  and `FinancialSnapshotDetail` (snapshot + both line-item lists + totals)
  as plain records returned by the service. The web layer does not navigate
  a lazy `snapshot.getLineItems()` collection (risky under
  `spring.jpa.open-in-view: false`); the service loads and bundles
  everything it needs within its own transaction instead.
- Added `FinancialSnapshotController` under
  `/api/households/{householdId}/financial-snapshots` (`POST`, `GET /{id}`,
  `GET` list) and `CreateFinancialSnapshotRequest` (`asOfDate` only —
  `@NotNull @PastOrPresent`, matching `Asset.valuedAt`'s validation style),
  `SnapshotAssetLineItemResponse`, `SnapshotLiabilityLineItemResponse`,
  `CurrencyTotalsResponse`, and `FinancialSnapshotResponse` DTOs.
- Wired `FinancialSnapshotNotFoundException` into `ApiExceptionHandler`
  (`FINANCIAL_SNAPSHOT_NOT_FOUND`, 404).
- Added Flyway migration `V4__create_financial_snapshots.sql`:
  `financial_snapshots`, `snapshot_asset_line_items`, and
  `snapshot_liability_line_items`, each with a `CHECK (value >= 0)` where
  applicable and an FK+index to their parent (line items to
  `financial_snapshots`, not to `assets`/`liabilities`).
- Updated `README.md`: documented the new endpoints, eligibility rule,
  per-currency totals semantics, and the `asOfDate`-vs-`capturedAt`
  distinction; updated the Status section (Phases 1-3 and Task 003 are
  accepted; Phase 4 is implemented and pending Product Owner acceptance).

**Tests**

- `FinancialSnapshotServiceTest` (Mockito unit tests): household-not-found
  on create/get/list; date-boundary eligibility filtering (a record dated
  exactly on `asOfDate` is included, one dated after is excluded); per-
  currency totals computed correctly without combining currencies (PHP
  assets+liabilities vs. a USD-only asset); an empty snapshot for a
  household with no eligible records; household-scoped retrieval and
  isolation; and ascending-order listing.
- `FinancialSnapshotApiIntegrationTest` (`@SpringBootTest` +
  `@AutoConfigureMockMvc` + Testcontainers `PostgreSQLContainer`, real
  Flyway migration run): create/retrieve with both an eligible asset and
  liability; date-boundary eligibility (included on `asOfDate`, excluded
  the day after); an empty zero-total snapshot for a household with no
  records; copied source identity and exact-decimal value preservation
  (with the line-item `id` asserted distinct from `sourceAssetId`); a
  negative net-worth result when liabilities exceed assets; multi-currency
  isolation in `totalsByCurrency`; future-`asOfDate` and missing-`asOfDate`
  rejection; an unsupported client-submitted `sourceType` field rejected;
  unknown-household 404 on create/list; cross-household retrieval returns
  404 without disclosing the record; new households return an empty list;
  duplicate `asOfDate` values permitted and listed in ascending `asOfDate`
  order; and cross-household isolation. 16 tests, all passing.
- Full suite: `./verify.sh` — 137 tests (24 new: 8 unit + 16 integration),
  0 failures, Flyway migrating a clean database through V1 -> V2 -> V3 -> V4
  automatically.
- Exercised the primary flow manually against `docker compose up --build`:
  created a household with a PHP asset/liability dated "today" and a USD
  asset dated "yesterday" (container clock is UTC, one day behind the
  host's local Asia/Manila date at the time — used the container's actual
  UTC date for `valuedAt`/`balanceAsOf`/`asOfDate` after confirming the
  offset via `docker exec ... date -u`); created a snapshot as of "today"
  (all three records eligible, PHP and USD totals correct, PHP net worth
  700.00, USD net worth 400.00) and one as of "yesterday" (only the USD
  asset eligible); retrieved the snapshot by ID; listed snapshots back in
  ascending `asOfDate` order; confirmed a future `asOfDate` and an unknown
  household both return the expected 400/404; confirmed cross-household
  retrieval returns 404; tore the stack down afterward.

**Decisions**

- Did not persist per-currency totals as their own table/rows: they are a
  pure, deterministic function of the immutable copied line items (PD-003),
  so the service recomputes them on every read from `TreeMap`-grouped sums
  rather than storing and risking drift between a stored total and its
  source line items.
- Combined asset total, liability total, and net worth into one
  `CurrencyTotals`/`totalsByCurrency` structure keyed by currency, rather
  than three separate parallel arrays, so a consumer never has to
  cross-reference three lists by currency code to find one figure.
- Did not add a DB foreign key from line items to `assets`/`liabilities`:
  the product brief calls the source reference "retained UUID metadata,"
  and Task 002 has no delete endpoint today, but a future one should not
  need to reason about historical snapshot references when removing a
  source row.
- Reused `AssetRepository`/`LiabilityRepository`'s existing
  household-scoped list query and filtered eligibility in Java rather than
  adding new derived-query methods to those repositories, since household
  asset/liability counts are small (a private household, not bulk data) and
  this keeps Task 002's repositories completely unchanged.
- Validated `asOfDate` not-in-the-future via Bean Validation
  (`@PastOrPresent`), matching `Asset.valuedAt`/`Liability.balanceAsOf`,
  rather than a service-level check, since it is a single-field,
  server-clock-relative rule with no cross-field comparison.
- Kept `FinancialSnapshot`/line-item entities in the existing
  `com.waypoint.household` package, consistent with Tasks 002 and 004.

**Assumptions**

- "Immutable" is enforced by omission (no update/delete routes and
  `updatable = false` line-item columns) rather than a database trigger or
  application-level guard, matching Task 004's precedent for similar
  invariants (e.g. `IncomeStream`'s `endDate` rule) being enforced at the
  layer that already owns write access.
- List endpoints return each snapshot's full detail (line items and
  totals), matching the single-GET shape, consistent with how
  `AssetController`/`LiabilityController`/`IncomeStreamController` already
  return full objects from their list endpoints; this is a small household
  dataset, not a paginated bulk listing.
- No household data was seeded; the manual verification evidence above used
  disposable "Task 005 Smoke Test" households in the local Docker Compose
  Postgres volume, alongside pre-existing smoke-test households left by
  prior tasks in that same shared local volume.

**Open questions**

- Same as the product brief: whether income/obligation schedules join
  future snapshots, whether asset/liability value changes become immutable
  valuation records or financial events, and whether snapshots need labels,
  notes, deletion, or comparison endpoints are all deferred.

**Recommended next task**

- A snapshot-comparison or plan-versus-actual endpoint, or continue the
  roadmap toward goals (Phase 5), now that historical balance-sheet state
  exists.

### 2026-09-04 — Task 005 product framing

**Changed**

- Defined the Task 005 product brief for immutable financial position
  snapshots and made it the active task in `agent/current-task.md`.
- Scoped the increment to copied asset/liability observations and
  per-currency net worth, with no FX conversion or cash-flow normalization.

**Tests**

- No application tests apply to product framing; implementation verification
  is pending.

**Decisions**

- Snapshots filter currently stored source records by `asOfDate`, preserve the
  actual `capturedAt` timestamp, and explicitly do not claim unavailable
  historical valuation reconstruction.
- Net worth is calculated only within each original currency.
- Snapshot data is copied and create/read-only so later source changes cannot
  rewrite historical observations.

**Assumptions**

- A household with no eligible records can create an empty zero-total
  snapshot.
- Duplicate snapshot dates are allowed because capture events are distinct.

**Open questions**

- Historical income/obligation schedule capture and deterministic cash-flow
  normalization remain for a later increment.
- Full source valuation history and historical comparison endpoints remain
  deferred.

**Recommended next task**

- Implement Task 005 on `task/005-financial-snapshots`, then return to the
  Product Owner Agent for independent review against the brief.

### 2026-09-04 — Task 004 Product Owner acceptance

**Review outcome**

- `ACCEPTED`: the Task 004 product brief's criteria are satisfied by the
  implementation evidence recorded below: local `./verify.sh` passed with 113
  tests and 0 failures, the required CI `verify` check is recorded as passing,
  and the primary API flow was manually exercised against Docker Compose.
- No unmet criteria or returned work were identified. GitHub was unreachable
  during this acceptance pass, so the recorded CI result was not independently
  re-queried.

**Recommended next task**

- Financial snapshots, immutable schedule history, and deterministic
  cash-flow normalization/planning.

### 2026-09-03 — Task 004: Income and Recurring Obligations

**Changed**

- `task/004-income-obligations` was cut from `main` before Task 003 merged,
  so it was missing `verify.sh`, the required `verify` CI check, and the
  Task 003 documentation updates. Merged `origin/main` into the task branch
  before implementing; the only conflict was `agent/current-task.md` (stale
  Task 003 content vs. the already-defined Task 004 task), resolved in favor
  of the Task 004 content since Task 003 was already accepted and merged.
- Added `IncomeStream` and `Obligation` JPA entities
  (`backend/src/main/java/com/waypoint/household/`), both household-scoped
  via a `@ManyToOne` to `Household`, with server-assigned `id`,
  `sourceType = MANUAL_ENTRY`, and Hibernate-managed `createdAt`/`updatedAt`.
  `IncomeStream` carries `incomeType`, `amount` (rate or flat amount
  depending on `frequency`), `frequency`, `currency`,
  `compensationClassification` (`GROSS`/`NET`/`UNKNOWN`), `certainty`
  (`CONFIRMED`/`EXPECTED`/`VARIABLE`), `startDate`, and optional `endDate`.
  `Obligation` carries `obligationType`, `amount`, `frequency`, `currency`,
  `startDate`, and optional `endDate`. Added the supporting enums
  (`Frequency`, `IncomeType`, `IncomeCertainty`,
  `CompensationClassification`, `ObligationType`) and a shared
  `InvalidScheduleException` for the `endDate < startDate` rule.
- Added `IncomeStreamRepository`/`ObligationRepository`
  (household-scoped `findBy...OrderByCreatedAtAscIdAsc` and
  `findByIdAndHousehold_Id` queries, matching the `Asset`/`Liability`
  pattern), `IncomeStreamService`/`ObligationService` (household lookup,
  name/currency normalization, `endDate`-not-before-`startDate` validation,
  not-found semantics), and `IncomeStreamNotFoundException`/
  `ObligationNotFoundException`.
- Added `IncomeStreamController`/`ObligationController` under
  `/api/households/{householdId}/income-streams` and
  `/api/households/{householdId}/obligations` (`POST`, `GET /{id}`, `GET`
  list), plus `CreateIncomeStreamRequest`/`IncomeStreamResponse` and
  `CreateObligationRequest`/`ObligationResponse` DTOs with Bean Validation
  (`@NotBlank`/`@NotNull`/`@DecimalMin(0)`/`@Digits(17,2)`/3-letter currency
  `@Pattern`). Unlike `valuedAt`/`balanceAsOf` on `Asset`/`Liability`,
  `startDate` has no `@PastOrPresent` constraint, so future starts are
  accepted per the product brief (the household has jobs starting in
  October 2026).
- Wired `IncomeStreamNotFoundException`, `ObligationNotFoundException`, and
  `InvalidScheduleException` into `ApiExceptionHandler`
  (`INCOME_STREAM_NOT_FOUND` / `OBLIGATION_NOT_FOUND` 404,
  `VALIDATION_FAILED` 400).
- Added Flyway migration `V3__create_income_streams_and_obligations.sql`
  creating `income_streams` and `obligations` tables (both with a
  household-id index and a `CHECK (amount >= 0)` constraint, matching the
  `assets`/`liabilities` non-negativity-constraint pattern; `endDate`
  ordering is enforced in the service layer only, consistent with how
  `Asset.planningValue <= estimatedValue` is enforced today).
- Updated `README.md`: documented the new endpoints, enum values, and
  semantics (future-dated starts allowed; `certainty` always returned as
  submitted, never inferred; `amount` is schedule-relative, not annualized),
  and updated the Status section (Phase 1/2 and Task 003 are accepted; Phase
  3 is implemented and pending Product Owner acceptance).

**Tests**

- `IncomeStreamServiceTest` / `ObligationServiceTest` (Mockito unit tests):
  name/currency normalization, household-not-found on create/get/list,
  `endDate`-before-`startDate` rejection, household-scoped retrieval and
  isolation, and creation-order listing.
- `IncomeObligationApiIntegrationTest` (`@SpringBootTest` +
  `@AutoConfigureMockMvc` + Testcontainers `PostgreSQLContainer`, real
  Flyway migration run): create/retrieve for both resources; future
  `startDate` accepted; zero amount accepted and negative rejected; blank
  name, malformed currency, and unrecognized `incomeType`/`obligationType`/
  `frequency`/`certainty`/`compensationClassification` rejected; `endDate`
  before `startDate` rejected and equal-to-`startDate` accepted; exact
  `NUMERIC(19,2)` decimal preservation; excessive fractional scale and
  precision-overflow rejection; an unsupported client-submitted `sourceType`
  field rejected; unknown-household 404 on create/list; cross-household
  retrieval returns 404 without disclosing the record; new households return
  empty lists; creation-order listing with permitted duplicate names; and
  cross-household isolation. 37 tests, all passing.
- Full suite: `./verify.sh` — 113 tests (32 asset/liability +
  37 income/obligation + 44 household/person/misc.), 0 failures, Flyway
  migrating a clean database through V1 -> V2 -> V3 automatically.
- Exercised the primary flow manually against `docker compose up --build`
  (Docker Desktop started for this session): created a household, an
  `EXPECTED`/`GROSS` monthly salary income stream with an October 2026
  start date, a `VARIABLE`/`UNKNOWN` hourly freelance income stream, and a
  monthly mortgage obligation; listed both collections back in creation
  order; confirmed a negative-amount obligation and an `endDate` before
  `startDate` both return `VALIDATION_FAILED` 400; confirmed an unknown
  household returns 404; confirmed the created records survived a
  `docker compose down` / `up` cycle (named-volume persistence), then tore
  the stack down.

**Decisions**

- Kept `IncomeStream`/`Obligation` in the existing `com.waypoint.household`
  package/module rather than introducing a separate `cash_flow` module,
  matching how `Asset`/`Liability` were placed in Task 002 and preserving
  the current single-module boundary until a concrete feature needs the
  split described in `docs/architecture/architecture.md`.
- Used one shared `Frequency` enum (`HOURLY`, `WEEKLY`, `BIWEEKLY`,
  `MONTHLY`, `ANNUAL`) for both `IncomeStream` and `Obligation`, since the
  product brief defines frequency once and both resources need the same
  vocabulary.
- Named the income amount/rate field `amount` (not `rate`) on both
  entities: per PD-001 it is stored and returned exactly as submitted, with
  `frequency` giving it meaning (e.g. an `HOURLY` amount is an hourly rate);
  no normalization, annualization, or total is calculated in this task.
- Did not add a database-level `CHECK` for `endDate >= startDate`, matching
  the existing precedent that `Asset.planningValue <= estimatedValue` is
  enforced only in `AssetService`, not in the V2 migration; enforced it in
  `IncomeStreamService`/`ObligationService` via `InvalidScheduleException`
  instead.
- Followed the product brief's PD-001-PD-003: preserved caller-stated
  schedule semantics without aggregation/annualization; made income
  `certainty` and `compensationClassification` caller-supplied and returned
  verbatim (never inferred from `incomeType` or `amount`); implemented
  create/read only, with no update/delete/history endpoints.

**Assumptions**

- `certainty` and `compensationClassification` apply to `IncomeStream` only,
  per the product brief's field list; `Obligation` has no comparable
  certainty concept in this task.
- A caller-supplied three-letter currency code (normalized to uppercase),
  matching the `Asset`/`Liability` precedent, is sufficient for this
  increment; no per-currency FX or catalogue validation is applied.
- No household data was seeded; the manual verification evidence above used
  a disposable "Task 004 Smoke Test" household in the local Docker Compose
  Postgres volume, alongside pre-existing smoke-test households left by
  prior tasks in that same shared local volume.

**Open questions**

- Same as the product brief: an hourly-rate expected-hours companion field,
  required-vs-discretionary obligation flagging beyond `obligationType`,
  and day- vs. month-level date precision remain deferred to a concrete
  downstream planning need.
- Aggregation, FX conversion, taxes, and net-cash-flow semantics remain
  undefined until the deterministic planning-engine phase.

**Recommended next task**

- Phase 4 of the roadmap: financial snapshots (point-in-time net worth and
  cash-flow summary), which can now read from `Asset`, `Liability`,
  `IncomeStream`, and `Obligation`.

### 2026-09-03 — Task 003: Automate Delivery Gates (PR #2)

**Changed**

- Added `verify.sh` at the repository root: the single canonical verification
  command. It `cd`s into `backend/` (no location ambiguity for the caller)
  and runs `./mvnw --batch-mode test` — the complete Java 21 Maven suite,
  including the PostgreSQL/Testcontainers integration tests. Requires only a
  JDK on PATH (the Maven wrapper provisions Maven) and a running Docker
  daemon (used by Testcontainers, not to run Maven itself); exits nonzero on
  any test failure.
- Added `.github/workflows/verify.yml`: a least-privilege (`permissions:
  contents: read`) GitHub Actions workflow that runs on `pull_request`
  events targeting `main`, checks out the PR, sets up Temurin JDK 21, and
  runs `./verify.sh` unmodified — the same command local agents run. The job
  is named `verify`, which is also the exact reported status-check context.
- Revised `.github/pull_request_template.md` to require: the local
  `./verify.sh` result, the required CI `verify` check result and run link,
  the manually exercised primary flow, task/product-brief links, UI evidence
  (when applicable), and deviations/known limitations.
- Updated `AGENTS.md` ("Agent collaboration") and
  `agent/collaboration-workflow.md` ("Branching and pull requests", and
  step 4's checklist) so both documents consistently state: Codex may
  commit and push completed Product Owner review findings/acceptance
  without asking each time; Codex may merge only after its acceptance
  commit is on the PR and the required `verify` check is green; neither
  agent may merge past, or routinely bypass, a failed or missing required
  check; and same-account Codex review is not an independent formal GitHub
  approval — the product brief's recorded acceptance is the durable record.
- Added decision `D014` to `docs/decisions/decisions.md` documenting the
  required-CI-gate/one-canonical-command decision and its rationale.
- Updated `README.md`: the Tests section now documents `./verify.sh` as the
  canonical command (with `./mvnw test` from `backend/` noted as
  equivalent), and a new "Continuous integration" section documents the
  required `verify` check and points to this log for the settings
  read-back.
- Opened PR #2 (`task/003-delivery-gates` -> `main`) and configured `main`
  branch protection to require the `verify` status check.

**Tests / verification evidence**

- Local: `./verify.sh` from the repository root — `Tests run: 62, Failures:
  0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. Confirmed identical to running
  `./mvnw -B test` directly from `backend/` (no behavior change from the
  command wrapper).
- Fail-fast behavior verified deliberately: temporarily injected a failing
  JUnit test (`Assertions.fail("canary")`) into `HouseholdServiceTest`, ran
  `./verify.sh`, confirmed exit code `1` and a Maven `BUILD FAILURE` /
  surefire failure report, then reverted the file (`git checkout --`) before
  committing anything.
- CI: PR #2's `verify` job on GitHub-hosted `ubuntu-latest`
  (run `https://github.com/codelinguist/waypoint/actions/runs/33758865382`)
  reported `Tests run: 62, Failures: 0, Errors: 0, Skipped: 0` and
  `BUILD SUCCESS` — the same count and result as the local run, satisfying
  the "reported test count agrees with local execution" criterion.
- Branch protection read-back (`gh api
  repos/codelinguist/waypoint/branches/main/protection`, credentials never
  logged):
  ```json
  {
    "required_status_checks": {
      "strict": false,
      "contexts": ["verify"],
      "checks": [{"context": "verify", "app_id": 15368}]
    },
    "enforce_admins": {"enabled": false},
    "required_linear_history": {"enabled": false},
    "allow_force_pushes": {"enabled": false},
    "allow_deletions": {"enabled": false}
  }
  ```
  No `required_pull_request_reviews` is configured (confirmed absent from
  the read-back), so `main` does not require an approving review that a
  single shared GitHub identity (`codelinguist`, used by both Claude Code
  and Codex) could not validly provide. `enforce_admins: false` preserves a
  deliberate administrator recovery path if the CI definition itself breaks,
  per the product brief's PD-002/PD-003 and the acceptance criteria.
- Confirmed the required check is live on PR #2 itself:
  `gh pr view 2 --json mergeable,mergeStateStatus,statusCheckRollup`
  reported `mergeable: MERGEABLE`, `mergeStateStatus: CLEAN`, and a single
  `verify` check with `conclusion: SUCCESS` — i.e., GitHub is actually
  evaluating the required check against this PR, not merely accepting the
  protection configuration.
- Directly observed GitHub blocking merge while the check runs, not just
  after: pushing the implementation-log update triggered a second `verify`
  run; while it was `IN_PROGRESS`, `gh pr view 2` reported
  `mergeStateStatus: BLOCKED`; once that run completed
  (`conclusion: SUCCESS`), the same query reported `mergeStateStatus: CLEAN`
  again — confirming the "GitHub prevents merge while the check is missing,
  running, or failing" behavior empirically, not just via configuration
  read-back.
- Full pre-existing suite (Task 001 + Task 002 tests) is included in the 62
  and remains green; no application-domain, API, schema, or UI code was
  touched by this task.

**Decisions**

- Wrapped the verification command as a thin shell script that `cd`s into
  `backend/` and calls the existing Maven wrapper directly on the runner's
  JDK, rather than re-running Maven inside a nested `maven:3.9-eclipse-
  temurin-21` container (as an earlier local Task 002 session had done to
  work around a sandbox that lacked a host JDK). Running Maven natively lets
  Testcontainers talk to the Docker daemon directly with zero extra
  networking configuration (no `TESTCONTAINERS_HOST_OVERRIDE` /
  `TESTCONTAINERS_RYUK_DISABLED` needed), works identically on this host and
  on GitHub-hosted `ubuntu-latest` runners via `actions/setup-java`, and
  avoids Docker-Desktop-vs-Linux `host.docker.internal` networking
  differences entirely. See `D014`.
- Named the workflow `Verify` and its single job `verify`; GitHub reports
  the status-check context as exactly `verify`, which is what branch
  protection now requires — confirmed empirically from the first live run
  rather than assumed.
- Required only the `verify` status check on `main`, deliberately omitting a
  required approving review, since Claude Code and Codex share one GitHub
  identity and an unsatisfiable requirement would deadlock every PR. Recorded
  this explicitly (`AGENTS.md`, `agent/collaboration-workflow.md`, PR
  template) rather than silently relying on the same-account review as if it
  were independent.
- Left `enforce_admins: false` so a broken CI definition has a real recovery
  path, per the acceptance criteria; this is intentionally not a routine
  bypass and is documented as a deliberate exception in `D014` and the
  collaboration workflow.

**Assumptions**

- GitHub Actions and branch protection on this repository's current plan
  support one required status check without cost; confirmed directly rather
  than assumed, since this session has admin access to `codelinguist/
  waypoint`.
- A JDK is available on every environment expected to run `./verify.sh`
  directly (this host, and GitHub Actions via `actions/setup-java`). If a
  future contributor's environment has Docker but no JDK, `verify.sh` would
  need a container-based fallback; not built now since it is not the
  documented constraint (Docker, not Java, is the project's stated only
  local-development prerequisite for *running the application*; running
  tests directly has always been the documented "optional, faster inner
  loop" path per `README.md`).

**Open questions**

- Per the product brief: separate GitHub identities/Apps for genuinely
  independent reviewer identity, and additional security/UI/deployment/
  risk-tier-specific checks, remain deliberate follow-up work, not part of
  this baseline gate.

**Recommended next task**

- Return to the Product Owner Agent (Codex) for independent review of PR #2
  against `agent/product/delivery-gates/product-brief.md`'s acceptance
  criteria, then resume the product-domain roadmap (e.g., financial
  snapshots or the facts/assumptions provenance model) once Task 003 is
  accepted and merged.

### 2026-09-03 — Task 002 review findings addressed (PR #1)

**Changed**

- F-001 (BLOCKING): Added `@Digits(integer = 17, fraction = 2)` to
  `estimatedValue`/`planningValue` (`CreateAssetRequest`) and
  `outstandingBalance` (`CreateLiabilityRequest`), matching `NUMERIC(19,2)`.
  Excess fractional scale and precision overflow are now rejected with a
  structured 400 before persistence instead of being silently rounded or
  crashing with a 500.
- F-002 (BLOCKING): Set
  `spring.jackson.deserialization.fail-on-unknown-properties: true` in
  `application.yml`. A client can no longer submit `sourceType` (or any other
  unrecognized field) on a create request; it is now rejected with a
  structured 400 (`MALFORMED_REQUEST`) instead of being silently ignored.
  Applied globally rather than per-DTO so it also covers future request types.
- F-003 (BLOCKING): Added the missing liability-side integration tests that
  already existed for assets (blank name, malformed currency, unrecognized
  enum, future date, populated-list ordering with duplicate names), plus new
  exact-decimal round-trip, excessive-scale, precision-overflow, and
  unsupported-provenance tests for both assets and liabilities.
- F-004 (BLOCKING): The PR's documented test command mounted `$(pwd)` at
  `/workspace` without noting it must be run from `backend/` (`pom.xml` lives
  there, not at the repo root). Corrected the command in the PR description
  and this log to mount `backend/` explicitly.

**Tests**

- 62 tests passing under Java 21 (`docker run --rm -v "$(pwd)/backend":/workspace -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e TESTCONTAINERS_RYUK_DISABLED=true -w /workspace maven:3.9-eclipse-temurin-21 mvn -B test`, run from the repository root), up from 49 — the 13 new tests cover the four findings above.
- Live-verified against a rebuilt `docker compose up --build`: unsupported
  `sourceType` now returns 400 `MALFORMED_REQUEST`; excess fractional scale
  and precision overflow both now return 400 `VALIDATION_FAILED` (the latter
  previously crashed with a raw 500); a valid exact-decimal value
  (`1234.56`) still round-trips correctly.

**Decisions**

- Fixed unsupported-field rejection globally via Jackson configuration rather
  than per-DTO, since the underlying concern (clients cannot claim
  server-owned fields) applies to every request type, not only this task's.

**Open questions**

- Same as the initial Task 002 entry below; unchanged by this fix round.

**Recommended next task**

- Return to the Product Owner Agent for re-review of PR #1 against the four
  `ACCEPTED` findings above.

### 2026-09-03 — Task 002: Record Household Assets and Liabilities

**Changed**

- Added Flyway `V2__create_assets_and_liabilities.sql`: `assets` and
  `liabilities` tables, FKs to `households`, scoped indexes, and DB-level
  CHECK constraints for non-negative values and `planning_value <=
  estimated_value`.
- Added `Asset`/`Liability` JPA entities and `AssetType`/`LiabilityType`/
  `Liquidity`/`SourceType` enums under `com.waypoint.household`, mirroring the
  Task 001 package layout.
- Added `AssetRepository`/`LiabilityRepository` with household-scoped and
  household+id-scoped lookups; `AssetService`/`LiabilityService` implementing
  create/get/list, household existence checks, and the planning-value-vs-
  estimated-value business rule (`InvalidAssetValueException`).
- Added `AssetController`/`LiabilityController` under
  `/api/households/{householdId}/assets` and `.../liabilities`, validated
  request/response DTOs, and three new `ApiExceptionHandler` mappings
  (`ASSET_NOT_FOUND`, `LIABILITY_NOT_FOUND`, and the reused
  `VALIDATION_FAILED` code for the cross-field business rule).
- Documented representative requests and enum values in `README.md`, and
  extended its Backend/Status sections to cover Phase 2.

**Tests**

- 49 tests passing under Java 21 (`docker run maven:3.9-eclipse-temurin-21`,
  matching the project's own Dockerfile base image): all prior Task 001 tests
  plus `AssetServiceTest` (7), `LiabilityServiceTest` (6), and
  `AssetLiabilityApiIntegrationTest` (19).
- `AssetLiabilityApiIntegrationTest` runs against a real, ephemeral
  Testcontainers PostgreSQL instance and confirmed Flyway applies V1 then V2
  cleanly on an empty schema.
- `docker compose up --build` against the existing Task 001 named volume
  confirmed a genuine upgrade path: Flyway logged "Current version of schema
  public: 1" then "Migrating schema public to version 2", both services
  reported healthy.
- Live smoke tests against the running container covered: create/get/list for
  both assets and liabilities; zero-value acceptance; negative-value
  rejection; planning-value-exceeds-estimated-value rejection; future-date
  rejection; unknown-household 404 on create; and cross-household 404 without
  disclosing the record's existence.

**Decisions**

- Kept `Asset`/`Liability` in the existing flat `com.waypoint.household`
  package rather than introducing a new sub-package, consistent with Task
  001's structure and the brief's smallest-increment scope.
- Validated non-negative values and the not-in-the-future date constraint
  declaratively via Bean Validation (`@DecimalMin`, `@PastOrPresent`) to match
  the existing DTO style; validated the planning-value-vs-estimated-value
  cross-field rule in the service layer (not a class-level Bean Validation
  constraint) since it is a genuine domain invariant, not a request-shape
  check, and needs to be independently unit-testable per `AGENTS.md`.
- Added DB-level CHECK constraints mirroring the two non-negative rules and
  the planning/estimated relationship, as defense-in-depth for financial
  data integrity; did not add a currency-format CHECK constraint, matching
  Task 001's precedent of leaving that to application-level validation.
- Reused the `VALIDATION_FAILED` error code for the planning-value business
  rule rather than minting a new code, since the brief treats it as one
  family of input-validation failures alongside the Bean Validation checks.

**Assumptions**

- "Upgrades a Task 001 database" is satisfied by both the Testcontainers
  empty-schema run (V1 then V2 in sequence) and the Docker Compose run against
  the already-migrated Task 001 volume; no separate V1-only intermediate
  environment was constructed, since Flyway's version-tracking behavior here
  is standard and not specific to this migration.
- Docker-in-Docker networking on this host's Docker Desktop required
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` and
  `TESTCONTAINERS_RYUK_DISABLED=true` when running Testcontainers-backed
  tests from inside the `maven:3.9-eclipse-temurin-21` container; this is a
  local test-runner detail, not a project dependency change.

**Open questions**

- Value-history semantics (how an asset/liability value changes over time)
  remain undecided before any update endpoint is introduced, per PD-004.
- Person-level ownership of individual assets/liabilities is still deferred.
- Exchange-rate sources and cross-currency aggregation remain undefined.

**Recommended next task**

- Independent Product Owner acceptance of Task 002 against
  `agent/product/assets-liabilities/product-brief.md`, then frame the next
  vertical increment (e.g., financial snapshots or the facts/assumptions
  provenance model) per the roadmap.

### 2026-09-03 — Task 001 product acceptance

**Changed**

- Product Owner Agent accepted Task 001 against the linked Household Foundation
  product brief and recorded the evidence there.
- No implementation changes were required during acceptance.

**Tests**

- Independently ran the complete suite under the declared Java 21 Docker
  environment: 17 tests passed with 0 failures and 0 errors.
- Built and started the application with Docker Compose from an empty database;
  PostgreSQL and the application both became healthy, and Flyway applied V1.
- Exercised household create/retrieve, person add/list, validation, and
  not-found responses through the live HTTP API.
- Verified a created household remained retrievable after `docker compose down`
  and `docker compose up`, confirming named-volume persistence.

**Decisions**

- Accepted Task 001 with no returned work or new architectural decision.

**Assumptions**

- The failed host-side test attempt under an installed Java 26 runtime is not a
  product failure: the project declares Java 21, and the same suite passed in
  the Java 21 Docker environment. Docker remains the canonical local path under
  D013.

**Open questions**

- Person-role vocabulary and member lifecycle remain intentionally deferred.
- Authentication remains required before any non-private deployment.

**Recommended next task**

- Frame Phase 2 as a small vertical increment for household assets and
  liabilities before overwriting `agent/current-task.md`.

### 2026-09-03 — Task 001: Scaffold the backend and Household aggregate

**Changed**

- Added a Spring Boot 3.5 / Java 21 backend at `backend/`, built with Maven
  (`backend/pom.xml`, `backend/mvnw`).
- Implemented the `Household` and `Person` JPA entities
  (`backend/src/main/java/com/waypoint/household/`), with UUID primary keys,
  Hibernate-managed `createdAt`/`updatedAt` timestamps, and a `Person` →
  `Household` many-to-one association.
- Added `HouseholdRepository` and `PersonRepository` (Spring Data JPA), a
  `HouseholdService` and `PersonService` for creation/lookup, and a
  `HouseholdNotFoundException` mapped to HTTP 404.
- Added Bean Validation request DTOs (`CreateHouseholdRequest`,
  `CreatePersonRequest`) and response DTOs (`HouseholdResponse`,
  `PersonResponse`), plus `HouseholdController` and `PersonController`
  exposing:
  - `POST /api/households`, `GET /api/households/{id}`
  - `POST /api/households/{id}/people`, `GET /api/households/{id}/people`
- Added a `@RestControllerAdvice` (`ApiExceptionHandler`) returning a
  structured `ErrorResponse` for validation failures, malformed
  requests/path variables, and unknown households.
- Added Flyway migration `V1__create_household_and_person.sql` creating
  `households` and `people` tables with a foreign key and an index on
  `people.household_id`.
- Added `backend/src/main/resources/application.yml`, sourcing all
  datasource/server settings from environment variables
  (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
  `SERVER_PORT`) with local-friendly defaults, and enabled the Actuator
  `health` endpoint.
- Added a multi-stage `backend/Dockerfile` (Maven build stage, Eclipse
  Temurin 21 JRE Alpine runtime stage, non-root user, container
  `HEALTHCHECK` against `/actuator/health`).
- Added root `docker-compose.yml` with `postgres` (named volume
  `waypoint-postgres-data`, `pg_isready` healthcheck) and `app`
  (`depends_on: condition: service_healthy`), plus `.env.example` and a
  `.gitignore` covering `backend/target/`, `.env`, and editor/OS files.
- Updated `README.md` with Docker-based setup, shutdown/reset, logs, host
  run, and test instructions.

**Tests**

- `HouseholdServiceTest` / `PersonServiceTest` (Mockito-based unit tests):
  name/currency/role normalization, and not-found behavior for unknown
  households.
- `HouseholdApiIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`
  + Testcontainers `PostgreSQLContainer`, real Flyway migration run):
  household create/retrieve, blank-name and malformed-currency validation
  (no persisted record), unknown-household 404 on get/add/list, empty
  member list for a new household, creation-order member listing,
  household-scoped member isolation, and permitted duplicate person names.
  17 tests total, all passing (`./mvnw test`).
- Verified `docker compose up --build` end-to-end from a clean state:
  app waits for Postgres `service_healthy` before starting, Flyway runs
  migrations automatically, the app container's own `HEALTHCHECK` reports
  `healthy`, household/person creation and retrieval work via `curl`,
  validation and not-found paths return 400/404, and a household created
  before `docker compose down` (no `-v`) was still retrievable after
  `docker compose up` again, confirming volume persistence.

**Decisions**

- Fixed a schema/entity mismatch found during testing: the initial
  migration declared `base_currency` as `CHAR(3)`, but Hibernate schema
  validation (`ddl-auto: validate`) expects `VARCHAR(3)` for a
  `@Column(length = 3)` String field; the migration now uses `VARCHAR(3)`.
- Used `GenerationType.UUID` (Hibernate 6) for entity IDs rather than a
  database-side UUID default, keeping ID generation in application code.
- Placed `Person` in the `household` package/module rather than a separate
  top-level module, matching the architecture doc's single "household"
  conceptual module for this increment.
- Followed the product brief's PD-001–PD-005: no seeded household/person
  data, minimal `Person` fields (name, role, household, id, timestamps),
  blank-field rejection, uppercase-normalized currency, not-found semantics
  for unknown households, member ordering by creation time with ID as a
  stable tie-breaker, and permitted duplicate names.
- Chose Spring Boot 3.5.16 (latest 3.5.x at implementation time) over the
  newly available Spring Boot 4.x line, to avoid taking on an unreviewed
  major-version migration in the foundational scaffold.

**Assumptions**

- A caller-supplied three-letter currency code (normalized to uppercase) is
  sufficient validation for this increment; no currency catalogue check is
  applied yet, per the product brief.
- No authentication/authorization exists yet; the API is intended for
  trusted local/private use only, per Task 001 constraints.
- `role` remains free text with no fixed vocabulary or authorization
  meaning.

**Open questions**

- Same as the product brief: person-role vocabulary, and
  update/delete/archival/membership-transfer behavior are undefined and
  deferred until a concrete downstream feature needs them.
- Authentication must be designed before any non-private deployment.

**Recommended next task**

- Phase 2 of the roadmap: `Asset` and `Liability` aggregates (balances,
  valuation dates, liquidity classification), building on the `Household`
  foundation established here.

---

### 2026-09-03 — Cross-agent collaboration workflow

**Changed**

- Added a repository-native Claude Code-to-Codex UI workflow.
- Added durable design-brief and visual-review templates.
- Added shared-agent handoff rules and Claude Code repository instructions.
- Added an explicit Product Owner role for problem framing, prioritization,
  design decisions, review triage, and final feature acceptance.
- Reassigned product ownership from Ralph to a dedicated Product Owner Agent and
  added its role charter and reusable product-brief template.

**Tests**

- Documentation structure and references reviewed manually; no application tests
  apply because application code has not been scaffolded.

**Decisions**

- Claude Code defaults to UI exploration and visual review; Codex defaults to
  implementation, integration, and final verification.
- Ralph and his wife are users and household authorities, not default product
  managers.
- The Product Owner Agent owns routine product decisions and evidence-based
  feature acceptance in a session separate from implementation.
- Product acceptance is distinct from household approval of material financial
  decisions or canonical-data changes.
- Agent identity does not override task scope or verified output quality.

**Assumptions**

- UI artifacts will be introduced only when a current task includes UI work.
- Claude Code will be installed and authenticated separately before its first
  live workflow stage.

**Open questions**

- No design system or screenshot harness is selected because the frontend is not
  yet in scope.

**Recommended next task**

- Continue Task 001: scaffold the Java backend and Household aggregate.

## Template for future entries

### YYYY-MM-DD — Task XXX

**Changed**

- ...

**Tests**

- ...

**Decisions**

- ...

**Assumptions**

- ...

**Open questions**

- ...

**Recommended next task**

- ...
