# Orchestration lab implementation plan

Status: STARTED — LAB-01 specification drafted; manual initiation only.
Date: 2026-09-06
Owner: Ralph, working with Codex and Claude Code

## Current progress

LAB-01 was explicitly initiated on 2026-09-06. Its
[execution contract](lab-01-execution-contract.md) and
[product brief](product-brief.md) are ready for review and subsequent implementation.
Runtime guarantees remain unimplemented. Next increment: LAB-02.
Ralph approved the LAB-01 contract on 2026-09-06. PR closure and required green
verification remain outstanding; user approval does not waive these gates.
The user crontab was observed empty; six Claude sessions were reported blocked.
No scheduler or worker was changed. See the contract for the observation limits.

## Objective

Learn production orchestration by implementing and comparing the same Jira →
implementation → review → CI → merge workflow across several architectures.
Learning depth, failure recovery, and architectural comparison are explicit
objectives, independent of Waypoint's current project scale.

Waypoint is also a real application intended for real users and consequential
household decisions. Ralph explicitly reaffirmed this on 2026-09-06. Learning is
an additional objective, not permission to compromise product correctness,
security, usability, or delivery. Workflow experiments must support shipping
useful, accepted product increments throughout this plan.

Use synthetic tasks and isolated environments for destructive failure injection.
Do not inject faults into live household data or active product work. Introduce
an experimental coordinator to real delivery only after its applicable recovery,
authority, and verification gates are demonstrated, with a rollback path. A
successful workflow demonstration is not evidence that the application feature
it produced is correct; product acceptance and deterministic financial tests
remain independently required.

Build working increments, inject failures, and record evidence. Keep the existing
product acceptance and required `verify` merge gates throughout the experiments.

## Manual execution and transition away from cron

- This plan does not authorize automatic dispatch. Do not put its tasks in
  `agent/tasks/` with `QUEUED` status while cron can claim them.
- The task backlog below is the planning record until a manual execution contract
  is established. LAB identifiers are planning IDs, not repository task numbers.
- Ralph and an agent select one task and initiate it explicitly. Before coding,
  create its approved brief, assign an unused repository task number, and work on
  its own branch/worktree through a PR under the collaboration workflow.
- Before running an experimental coordinator with write authority, disable the
  existing cron trigger and inspect active workers. Disabling cron does not stop
  workers already running. Finish or explicitly stop them before transferring
  ownership of their tasks or worktrees.
- Start new coordinators in observation mode using synthetic tasks. Exactly one
  coordinator may dispatch or merge any given task.
- Cron is not required to start this plan. Event triggers become the primary
  mechanism as experiments progress. Durable timers and reconciliation remain
  valid recovery mechanisms; eliminating cron does not eliminate recovery work.
- No schedule, existing worker, Jira issue, or executable queue entry was changed
  by saving this plan.

## Ownership and execution contracts

Define field ownership before implementing synchronization:

| System | Authority |
| --- | --- |
| Jira | Backlog priority, dependency planning, and human-facing delivery tracking |
| Repository | Approved execution scope, acceptance criteria, implementation, review evidence |
| Coordinator | Task attempts, worker ownership, leases, retries, and execution state |
| GitHub | PR revisions, CI results, and actual merge outcome |

Jira edits that change approved scope must become explicit change requests or a
new approved brief revision. They must not silently rewrite a running contract.
Dragging an issue to Done does not authorize a merge or replace verification.

Separate task identity from attempt identity. Record implementation, fix, and
review attempts independently, including their input revision, worker identity,
timestamps, outcome, and usage when available. Define legal transitions and
idempotency keys before choosing a storage or workflow framework.

Keep adapters replaceable: start implementation, request review, read CI outcome,
merge an accepted revision, and update Jira. Deterministic code owns authority,
budgets, retry rules, and merge eligibility; models implement and review inside
those boundaries. Recheck the relevant commit identity at mutation boundaries.

## Stages

| Stage | Deliverable | Evidence |
| --- | --- | --- |
| 1. Execution contract | State machine, task/attempt identities, dependencies, structured results | Illegal transitions and duplicate claims rejected |
| 2. Recovery baseline | Duplicate prevention, leases, stale-lock handling, timeouts, bounded backoff, pause | Crash at dispatch boundaries and recover without duplicate workers or lost edits |
| 3. Jira integration | Stable issue mapping, status mapping, brief/PR links, recoverable synchronization | Retries create no duplicate issues; human edits survive; completion follows verified merge |
| 4. Visibility and budgets | Structured history, status view, timings, usage reporting, limits, actionable notifications | Explain why a task is waiting, running, stalled, or costly |
| 5. GitHub Actions experiment | Repository/workflow event triggers and authenticated external dispatch | Compare latency, permissions, execution constraints, and recovery against baseline |
| 6. Webhook and queue experiment | Authenticated receiver, durable inbox, deduplication, acknowledgments, dead letters, completion hooks | Duplicate, late, and interrupted deliveries processed safely |
| 7. Durable workflow experiment | Persistent workflows, external signals, timers, retries, cancellation | Restart coordinator mid-task and resume from recorded progress |
| 8. SDK and isolation experiment | One SDK worker adapter; isolated execution and scoped credentials | Compare results, cancellation, usage, permission boundaries, and complexity with CLI workers |
| 9. Evaluation and cutover | Repeated scenarios, comparison report, selected architecture, rollback procedure | Evidence supports selection; one active coordinator after cutover |

Choose specific engines, queues, SDKs, and hosting after documenting evaluation
criteria and checking current official documentation, authentication requirements,
and costs. These are experiments, not a requirement to retain every component in
the final architecture.

## Manual task backlog

Use these as Jira tasks after project setup. Each gets an implementation brief
before coding, its own PR, and a recorded learning result.

| ID | Task | Depends on | Definition of done |
| --- | --- | --- | --- |
| LAB-01 | Define lifecycle and field ownership | None | Transition table, task/attempt schema, cancellation policy, revision binding, and manual-start contract approved |
| LAB-02 | Prevent duplicate dispatch and active-worktree resets | LAB-01 | Repeated ticks/events cannot start concurrent workers for one attempt or reset an active worker's changes |
| LAB-03 | Recover interrupted claims and abandoned workers | LAB-02 | Persisted dispatch intent, leases/fencing, stale-lock recovery, and crash-boundary tests |
| LAB-04 | Bound retries, runtime, and cancellation | LAB-03 | Failure classes, backoff, attempt/runtime limits, pause/cancel behavior, and exhausted-work handling verified |
| LAB-05 | Map Jira issues to execution contracts | LAB-01 | Project/status metadata inspected; durable issue mapping and field ownership tested; existing issues deduplicated |
| LAB-06 | Implement recoverable Jira synchronization | LAB-04, LAB-05 | Unattended authentication configured securely; pending updates retried; human edits preserved; merge-to-Jira failure recovers |
| LAB-07 | Add execution visibility and usage controls | LAB-04 | Status query/view, attempt history, timings, usage availability indicators, budget policy, and deduplicated alerts |
| LAB-08 | Run GitHub Actions coordination experiment | LAB-06, LAB-07 | Event-triggered synthetic task completes through existing gates; credentials and event recursion documented |
| LAB-09 | Build authenticated event inbox and queue | LAB-06, LAB-07 | Durable acceptance, deduplication, delivery ordering policy, retry/dead-letter handling tested |
| LAB-10 | Add event-driven worker lifecycle | LAB-09 | Completion hooks plus independent lease recovery; dependencies and capacity enforced |
| LAB-11 | Implement the workflow with a durable engine | LAB-10 | Same contract supports signals, timers, restart recovery, and cancellation; limitations recorded |
| LAB-12 | Compare CLI and SDK worker adapters | LAB-07 | Same synthetic task and structured result contract; auth, usage, cancellation, and behavior compared |
| LAB-13 | Isolate workers and scope authority | LAB-12 | Filesystem/network/credential boundaries demonstrated; tests prove worktree isolation alone is insufficient |
| LAB-14 | Run comparative failure experiments and cut over | LAB-08, LAB-11, LAB-13 | Comparison report, architecture decision, rollback exercise, and verified event-driven ownership transfer |

Suggested Jira epics:

1. Execution correctness and recovery — LAB-01–04.
2. Jira integration and operational visibility — LAB-05–07.
3. Event-driven orchestration — LAB-08–10.
4. Durable workflows and agent runtimes — LAB-11–13.
5. Failure experiments and architecture evaluation — LAB-14, gathering evidence throughout.

## Common failure experiments

Run these against each applicable architecture using synthetic work first:

- Worker starts; coordinator crashes before recording its session.
- Coordinator records intent; crashes before starting the worker.
- Event arrives twice, late, or out of order.
- Worker loses its lease and later resumes after a replacement starts.
- Jira/GitHub is unavailable or returns rate limits.
- PR changes after review; CI succeeds for an older commit.
- Merge succeeds; recording completion or updating Jira fails.
- Review repeatedly fails or a worker exceeds its runtime/usage budget.
- Cancellation arrives during execution or near a merge boundary.
- Coordinator restarts with pending tasks, timers, and synchronization updates.

For each, record expected invariant, injected fault, observed behavior, recovery,
and remaining uncertainty. A completion hook alone cannot establish worker health.

## Measurements and acceptance

Track event-to-start latency, queue time versus execution time, recovery time,
duplicate attempts, human interventions, escaped defects, and cost per accepted
task where observable. Mark missing usage data as unavailable rather than zero;
do not equate subscription utilization with API dollar cost.

Each completed task must contain:

- Learning objective and approved implementation scope.
- Relevant tests, including failure behavior rather than only happy paths.
- Commands/results and links to reproducible evidence.
- Tradeoffs and a concise decision record.
- Required `verify` success and independent acceptance before merge.
- An implementation-log entry and any workflow/documentation corrections.

## Connection state and setup needs

Interactive Atlassian Rovo access was verified on 2026-09-06 for
`https://codelinguistics.atlassian.net`, including the Waypoint Financial project
`KAN`. Confirm that destination before creating the lab backlog. Three existing
sample issues were observed; do not overwrite or delete them implicitly.

The desktop connector does not automatically authenticate the cron process,
GitHub Actions, a webhook receiver, or a workflow service. Select and configure
appropriate unattended authentication during LAB-06; never commit credentials.
Task descriptions should contain software requirements and links, not household
financial records.

## First working session

1. Confirm the Jira destination and create the epics/planning tasks manually or
   through the connected tool when Ralph is ready.
2. Inspect and disable the existing cron trigger before changing execution
   ownership; account for any active workers.
3. Start LAB-01 explicitly, approve its contract, then implement LAB-02.
4. Continue through dependency-ready tasks with manual initiation until event
   dispatch is deliberately enabled and tested.
