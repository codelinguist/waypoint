# LAB-01 — Execution lifecycle and ownership contract

Status: USER-APPROVED for implementation in LAB-02–04; specification only.
Approval: Ralph, 2026-09-06 — "execution contract looks good to me".
Date: 2026-09-06
Initiation: Ralph explicitly requested starting LAB-01 in the current conversation.
Parent: [Implementation plan](implementation-plan.md)

## Problem and outcome

The existing coordinator can process a cached BLOCKING review on successive runs
and dispatch overlapping fixes. It records claims before dispatch, but has no
durable attempt model for resolving interrupted dispatch. This contract defines
the invariants required before adding new triggers. A cron tick, webhook, manual
command, or workflow signal must obey the same rules.

Scope: task/attempt records, state transitions, ownership, dispatch identity,
revision binding, cancellation, and manual initiation. No runtime, scheduler,
Jira workflow, or existing task state changes in LAB-01. Storage technology and
worker SDK selection remain open. No household financial values are included.

Waypoint is a real product, not a disposable lab application. This contract must
support reliable delivery of useful features for real users. Experimental engines
must demonstrate applicable safeguards before owning real tasks; fault injection
uses isolated synthetic work. Workflow acceptance never replaces feature acceptance,
deterministic financial validation, or explicit household approval requirements.

## Authority

| Data/action | Owner | Other systems |
| --- | --- | --- |
| Backlog priority and proposed dependencies | Jira/product owner | Coordinator validates dependencies before claiming |
| Approved scope and acceptance criteria | Versioned repository brief | Jira links to this revision; changes require reapproval |
| Execution state, attempts, ownership, retry counters | One coordinator | Jira displays a projection; workers report facts |
| PR head, CI result, merge outcome | GitHub | Coordinator reads and validates exact revisions |
| Acceptance verdict | Independent reviewer, durable evidence | Valid only for its recorded revision/context |
| Pause/resume/cancel/start request | Authorized human or configured trigger policy | Coordinator validates and records request |

No event handler or worker independently writes task lifecycle state. All updates
go through the coordinator's version-checked transition boundary. A Jira edit is
input, not an imperative command to a running worker. No adapter may broaden a
task's authority. Acceptance never authorizes household financial-data changes.

## Records (logical schema, version 1)

All timestamps are UTC RFC 3339. IDs are opaque and immutable. Unknown values are
null, never invented. Every mutation records actor, reason, and request/event ID.

### Task

- `schema_version`, `task_id`, `repository`, optional `jira_issue_id`/`jira_key`.
- `brief_path`, `brief_commit`, `brief_content_digest`: frozen approved scope.
- `dependencies`: task IDs whose MERGED outcome is required; reject cycles.
- `state`, monotonically increasing `version` for compare-and-swap updates.
- `owner_coordinator_id`, `ownership_epoch`: assigned by a single durable authority.
- `active_attempt_id`: null or the one nonterminal attempt for this task.
- `branch`, `worktree_id`, `pr_number`: identifiers, not proof of ownership.
- `pause_requested`, `cancel_requested_at`, `blocked_reason`.
- `created_at`, `updated_at`; retry/runtime budgets and counters by failure class.
- Optional acceptance record and observed merge commit; Jira sync state is separate.

### Attempt

- `attempt_id`, `task_id`, `kind`: IMPLEMENTATION, FIX, REBASE, REVIEW, or MERGE.
- `state`, `version`, `ownership_epoch`, `dispatch_key`.
- `input_brief_digest`, `input_head_sha`, `input_base_sha` as applicable.
- `worker_id`, `provider_session_id`, `worktree_id` (nullable before dispatch).
- `lease_expires_at`, `last_heartbeat_at`, `started_at`, `finished_at`.
- `deadline_at`, `failure_class`, `result_reference`, `cancellation_requested_at`.
- `usage`: provider-reported measurements with units/source, or null if unavailable.

A dispatch key is derived from repository, task ID, and attempt ID. Retrying a
dispatch uses the same key. A deliberate new attempt receives a new ID and key.
A retry of the same dispatch is not a fix round and must not increment its budget.

### Worker result

Includes schema version, task/attempt IDs, ownership epoch, dispatch key, observed
input revision, provider session ID, outcome, output commit/PR references, test
evidence, failure class, timestamps, and available usage. The coordinator validates
the identity, current authority, and allowed outcome before applying the result.
REVIEW results also record verdict, reviewed head/base SHAs, brief digest, and
evidence. Free-form text alone cannot authorize merge.

## Task state machine

| From | Trigger and guard | To |
| --- | --- | --- |
| PLANNED | Approved brief; validated dependency graph; explicit start authorization recorded | READY |
| READY | Dependencies MERGED; capacity; unpaused; atomically claim and persist attempt intent | ACTIVE |
| ACTIVE | Implementation/fix/rebase succeeds; PR and output revision verified | WAITING_REVIEW |
| WAITING_REVIEW | Capacity; no active attempt; claim review for current head/base | ACTIVE |
| ACTIVE | Review ACCEPTED and evidence validated | WAITING_CHECKS |
| ACTIVE | Review BLOCKING; prior attempt terminal; retry budget available | READY |
| WAITING_CHECKS | Acceptance stale because code/base changed | WAITING_REVIEW |
| WAITING_CHECKS | Acceptance and required CI valid for eligible revision; merge attempt claimed | MERGING |
| MERGING | GitHub confirms merge | MERGED |
| MERGING | Outcome unknown after timeout | MERGING (reconcile; never blindly retry) |
| MERGING | Confirmed not merged; revision/gates changed | WAITING_REVIEW or WAITING_CHECKS |
| Any nonterminal except uncertain MERGING | Failure needs intervention or retry budget exhausted | BLOCKED |
| BLOCKED | Explicit resume with reason; prior ownership resolved | READY or relevant waiting state |
| Any nonterminal | Cancel requested | Same state with cancellation flag until effects resolved |
| Any nonterminal | Cancellation confirmed and no unresolved mutation/worker | CANCELLED |

MERGED and CANCELLED are terminal. Reopening creates a new task. READY after a
review finding carries an explicit next attempt kind FIX; it does not restart an
initial implementation. Transient failures retain their required next action and
eligible retry time. Unknown dispatch/merge outcomes cannot enter ordinary retry.
Pause suppresses new attempts; it does not imply that an active process stopped.

These states are the lab target, not new values for the existing shell parser.
Legacy mapping: QUEUED → READY only after authorization/dependency validation;
IN_PROGRESS → ACTIVE only after ownership reconciliation; IN_REVIEW → inspect
PR/review/CI to select waiting state; STALLED → BLOCKED; MERGED → verified MERGED.

## Attempt state machine and dispatch protocol

INTENT → DISPATCHING → RUNNING → SUCCEEDED | FAILED | CANCELLED.
DISPATCHING or RUNNING may enter UNKNOWN when acknowledgement or health is lost.
UNKNOWN resolves only through provider/session and side-effect reconciliation.
An expired lease revokes authority but does not prove a process is dead.

1. Atomically verify task version, ownership epoch, no active attempt, capacity,
   dependencies, and pause/cancel flags. Persist intent and reserve capacity.
2. Dispatch through an adapter using the durable dispatch key.
3. Persist the returned session identity and move to RUNNING.
4. On restart, reconcile DISPATCHING/UNKNOWN attempts before considering retries.
5. If the provider cannot look up or deduplicate a dispatch, stop in BLOCKED with
   dispatch uncertainty. Do not claim exactly-once process launch or launch again.
6. Accept completion only for the current attempt/epoch. Persist outcome and release
   capacity atomically. Duplicate results return the recorded outcome without effects.

At most one nonterminal attempt owns a task/worktree. Initially reviews and writes
are serialized too. Every dispatch path, including fixes and rebases, shares the
same capacity enforcement. Never reset/remove a worktree while its prior worker's
ownership or termination is unresolved.

## Leases, fencing, and cancellation

Heartbeats renew a current lease through version-checked updates. A replacement
requires a higher ownership epoch. Reject stale-epoch results and mutation requests.
Fencing only works where the mutation boundary checks it: workers with direct Git
push credentials or unrestricted filesystem access can bypass coordinator checks.
Until those effects are mediated or isolated, require verified termination before
replacement; a lease timeout alone is insufficient.

Cancel records intent first, suppresses new attempts, and requests provider stop.
Confirm termination or remain BLOCKED/UNKNOWN. Preserve worktree and evidence.
For an in-flight merge, read GitHub's actual outcome: a confirmed merge wins and
records that cancellation arrived too late. Cancellation never undoes a merge.

## Revision-bound acceptance and merging

Bind review to head SHA, base SHA, and approved brief digest. A head/base change
invalidates acceptance unless an explicitly specified and tested policy permits
that exact change. Reviewer evidence written as a new commit is also a head change:
initial implementation must store evidence separately or require revalidation;
it must not silently relabel the old review as covering new code.

Required CI must cover the candidate revision. Serialize merge attempts, recheck
the current head and gates, and use the provider's expected-head precondition where
available. Validate integration with the current base via enforced branch policy
or a merge queue; a clean merge alone does not establish integration correctness.
Persist merge intent before calling GitHub and reconcile ambiguous responses.
Actual merge outcome is authoritative even if recording it or syncing Jira fails.

## Event and synchronization boundaries

Persist accepted events before acknowledging them. Deduplicate by source/event ID
and semantic operation identity; distinct events can request the same operation.
Treat events as signals to reconcile authoritative state, not ordered commands.
Persist pending external updates alongside state changes using a transactional
outbox or equivalent atomic mechanism. Jira failures cannot undo a merge or trigger
another implementation. Do not overwrite human-owned fields during reconciliation.

## Manual start contract

For this specification, the current user request authorizes LAB-01 documentation
only. Subsequent implementation begins by explicit selection of a lab task and an
approved brief. Record operator, task ID, brief revision, and unique start request
ID in the future coordinator before dispatch; repeated submission is idempotent.

Until that interface exists, keep lab contracts in this directory outside the
legacy executable queue and initiate the implementation conversation manually.
Do not invent a new legacy frontmatter status. An implementation PR must define
the supported manual task-file convention before using `agent/tasks/` for lab work.
Existing product acceptance and `verify` gates remain required. This document
approves design direction; it is not acceptance of unimplemented guarantees.

## Acceptance walkthroughs for implementation

| Scenario | Required behavior |
| --- | --- |
| Cached BLOCKING review arrives on next tick while FIX runs | No new attempt; no worktree reset; no retry increment |
| Crash after launch, before session persistence | Reconcile by dispatch key; if unavailable, block without relaunch |
| Crash before launch after intent persisted | Reconcile provider evidence before safe continuation |
| Same completion reported twice | One terminal outcome and one capacity release |
| Worker resumes after ownership revoked | Reject result; prevent effects at boundary or require termination before replacement |
| PR pushed after acceptance | Reject stale acceptance and review new revision |
| Old CI success arrives | Does not authorize current head |
| Merge succeeds, Jira unavailable | MERGED plus pending sync; no duplicate merge/implementation |
| Cancel during ambiguous merge | Reconcile actual merge before terminal cancellation |
| Manual start submitted twice | One task claim and one attempt intent |
| Dependency incomplete or coordinator paused | No dispatch |

## Delivery and remaining work

Specification review: transition/ownership, dispatch uncertainty, cancellation,
revision binding, and manual initiation are documented above. No runtime tests
were run because this increment changes documents only. Implement and test these
guarantees incrementally in LAB-02–04; independent PR acceptance remains pending.

Environment observation on 2026-09-06: `crontab -l` returned an empty user crontab.
`claude agents --json` reported six blocked sessions, including two fix sessions
each for tasks 012 and 013. No sessions were stopped or worktrees changed. This is
a point-in-time observation, not proof that no other scheduler/process exists.

Next: LAB-02 product brief and implementation of one shared guarded dispatch path.
LAB-03 supplies durable recovery; do not claim its guarantees in LAB-02 prematurely.
