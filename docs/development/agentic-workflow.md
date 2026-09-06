# Our agentic development workflow

Agreed direction: 2026-09-06. Setup and implementation have not started.

This is infrastructure for how we build software together. It is separate from
Waypoint's financial application, product roadmap, product briefs, and task queue.
We will implement this guide interactively: Ralph handles account access and
service settings; Codex inspects configuration and writes the necessary scripts.
We are currently planning. Codex may refine this guide as our decisions evolve.
Documentation approval does not authorize installing services, writing implementation
scripts, changing authentication, or launching workers. Ralph will explicitly start
implementation and enable live execution when we are ready.
This document does not enqueue work or authorize an unattended rollout.

## What we want

A prepared Jira issue moves through implementation in an isolated Git worktree,
independent review, verification, and merge. Jira shows progress throughout.
Routine coordination uses scripts; agents run only when reasoning or coding is
needed. Start with one task at a time to control token consumption.

## Backlog and board

Use Jira's separate backlog for ideas and future work. The board has five columns:

| Column | Meaning | Who acts |
| --- | --- | --- |
| To Do | Selected work being clarified and scoped; design prepared when needed | Codex, with Claude for design exploration |
| Ready | Ralph moves the issue here after scope/design readiness; this authorizes automatic pickup | Ralph authorizes; Claude execution bridge claims |
| In Progress | Claude claims the issue and implements or revises in a dedicated worktree | Claude, with automated status updates |
| Review | Claude has opened/updated and linked the PR; Codex reviews automatically, then Ralph merges if accepted | Claude hands off; Codex reviews; Ralph merges |
| Done | Ralph merged the accepted PR with required checks passing | Jira Automation confirms merge and updates status |

A blocked flag and short reason preserve the column where work stopped. Moving a
card manually to Done is not evidence of acceptance or merge. Backlog membership
is not an implementation trigger. Pause/cancellation must stop new launches;
running work is reconciled before its worktree can be removed.

## Current human checkpoints and review loop

Only Ralph moves an issue to Ready. Neither Codex, Claude, Rovo, nor an automation
rule may promote work to Ready on his behalf. A validated Ready transition triggers
Claude pickup; after a successful claim, set In Progress before implementation.
Duplicate events must not start another worker.

Claude opens and links the PR before moving the issue to Review. Prefer native Jira
rules to perform that status update on Claude's behalf. Entry into Review automatically
requests Codex review; wait for required checks to pass before the paid review run.
If Review has no valid linked PR, flag the problem instead of launching a reviewer.
Pending checks resume through check events/reconciliation, without repeated reviews.

Requested changes return the issue to In Progress for a bounded Claude revision;
Claude returns it to Review with updated PR evidence. Revisions on this same approved
scope do not require another Ready transition. Material scope changes pause work for
reframing and renewed authorization from Ralph.

After Codex accepts the current implementation revision and required checks pass,
notify Ralph that the PR is ready to merge. Keep the issue in Review while waiting.
Ralph performs the merge. Codex, Claude, and the coordinator do not merge automatically
in this initial workflow. Later code changes invalidate the previous ready-to-merge
handoff and require fresh verification/review as appropriate.

Jira Automation then moves the issue to Done after confirming the intended repository,
linked delivery PR, target branch, merge, and acceptance/check evidence. An unrelated
or partial PR merge must not close it. Ralph does not need to move the card manually.
If a merge lacks the required evidence, flag the exception rather than falsely
reporting accepted completion. Worktree cleanup follows only when the worker is idle
and unpublished changes have been preserved.

Automatic merging is a future, explicitly enabled option, never implied by Codex
acceptance. The existing legacy orchestrator may auto-merge; it must not own these
Jira-managed PRs. Verify that exclusion before the pilot. This guide does not change
that legacy runtime or its settings during planning.

## Codex creates framed work in the backlog

Ralph authorizes Codex to create Jira backlog issues after framing work, without
asking for permission for each issue. Once Jira access and the destination are
configured, use a native connector/API or supported Jira capability first; write
custom integration only if necessary. This standing authorization does not enable
account setup or live execution during our current planning phase.

Each issue should contain a clear title, problem/context, intended outcome, bounded
scope, acceptance criteria, relevant repository/design links, and known dependencies
or unresolved questions. Check existing issues first and update or link matching work
rather than creating duplicates. Keep inferred assumptions visibly separate from
confirmed requirements. Break work into independently useful increments when needed.

Create the issue in the separate backlog, not Ready. Verify actual backlog membership;
a To Do status alone does not prove the issue is off the board. A framed issue can
remain in the backlog with To Do status until selected for active preparation.
Missing design approval or dependencies must remain visible and block implementation
readiness. Do not invent unsupported Jira statuses to represent them.

Backlog creation authorizes recording planned work only. It does not authorize
implementation, move the issue to Ready, or enqueue a parallel executable repository
task. Report the created issue key and link so Ralph can inspect it. If a response is
ambiguous, reconcile whether creation succeeded before retrying.

## Responsibilities

**Ralph:** provide direction and preferences, enable services, authenticate locally,
move issues to Ready, merge accepted PRs with green required checks, and decide
when we enable automatic execution and what spending limits apply.
Never paste credentials into this document, Git, or chat.

**Codex:** clarify scope and acceptance criteria, create framed issues in the Jira
backlog under the authorization above, approve design direction, implement
this workflow infrastructure with Ralph, and independently review Claude's feature
implementations. Infrastructure we build here is not dispatched through the product
task queue. Feature delivery can continue using the repository's existing handoff
artifacts; building this workflow does not require another product brief or task file.

**Claude:** explore feature designs, implement feature code in an isolated worktree,
run tests, open a PR, and address accepted review findings. Implementation starts in
a fresh conversation using approved scope and repository instructions.

**Coordinator scripts:** detect eligible work, record ownership, launch the appropriate
agent, update Jira, enforce limits, and reconcile failures. No LLM is needed to move
cards, check status, deduplicate events, or count attempts.

Codex acceptance and required GitHub checks remain merge gates; Ralph performs
the merge in this initial workflow. Household financial
decisions still require household approval; development automation grants no banking,
investment, payment, or canonical financial-data authority.

## Where things belong

- This guide: `docs/development/agentic-workflow.md`.
- Scripts, adapters, and setup instructions: `agent/automation/`.
- Focused script tests: `agent/automation/tests/`.
- Runtime state and credentials: outside tracked files, with appropriate local access
  restrictions and redacted logs.
- Feature implementation: its own branch and Git worktree, following repository rules.

Workflow tooling must run independently of the financial application. Do not add
Java classes, backend Maven dependencies, Spring endpoints, or database migrations
for Jira coordination. Extend the existing shell tooling; use a small Python helper
where HTTP, JSON, or durable state handling makes that simpler. Tooling gets its own
focused tests. Repository delivery checks do not determine tooling placement.

## Native features first — minimize custom scripts

Ralph prefers configuring supported Jira/GitHub capabilities over writing scripts.
Inspect native functionality before implementing each responsibility. Use ordinary
Automation conditions/actions for deterministic coordination; use Rovo only where
text interpretation adds value. Native automation also has quotas and failure modes;
verify availability, costs, permissions, retries, and audit evidence in our account.

| Responsibility | Preferred implementation |
| --- | --- |
| Ready transition and basic field conditions | Jira Automation |
| Link issue to branch/PR and expose development activity | Jira's GitHub integration |
| PR-created / merged notifications and status projection | Jira Automation development triggers, with exact PR/repository and acceptance conditions |
| Draft descriptions, suggest missing criteria, summarize progress | Optional Rovo assistance; Codex retains scope/design/acceptance authority |
| Required build checks and repository protection | GitHub Actions and repository controls |
| Local worktree creation and CLI worker ownership | Small local bridge only if local execution remains the selected path |
| Independent Codex review and merge handoff | Automate review routing and notify Ralph; he merges, then Jira updates Done |

Do not add a second writer for a Jira field already owned by a native rule. Record
rule ownership, conditions, inputs, and failure recovery alongside any bridge. Use
exports where supported and redacted configuration notes otherwise. PR-created does
not mean check-passing, and a merged unrelated/partial PR must not mark an issue Done.
Rovo output is a suggestion, not a deterministic readiness or merge authorization.

Atlassian documents Claude Agent for Jira (Beta), backed by Anthropic Managed Agents:
it can take assigned work and open GitHub PRs. It requires Jira admin installation,
Managed Agents access/API credentials, and a GitHub organization/service account.
This is hosted execution, not our local Claude CLI/worktree model. Before choosing
it, verify actual availability, billing, repository/test environment support, Ready
trigger behavior, cancellation/duplicate handling, and independent Codex review.
Do not assume our CLI subscription covers it or switch execution models implicitly.

Rovo's general automation action generates text for subsequent actions; it is not
itself a local shell/worktree runner. Jira also offers a separate Jira Coding Agent;
using that instead of Claude would require an explicit change to the agreed roles.

Sources checked 2026-09-06 (account availability remains unverified):

- [Automation triggers](https://support.atlassian.com/cloud-automation/docs/jira-automation-triggers/)
- [Automation actions](https://support.atlassian.com/cloud-automation/docs/jira-automation-actions/)
- [Rovo agent automation behavior](https://support.atlassian.com/rovo/docs/agent-actions/)
- [Use Rovo text action](https://support.atlassian.com/studio/docs/what-is-the-use-rovo-action/)
- [Claude Agent for Jira setup](https://support.atlassian.com/jira-software-cloud/docs/set-up-claude-agent-for-jira/)

## Event-driven architecture — local execution design

First verify the native options above. The local design below applies if we retain
local CLI/worktree execution; it is not a mandate to build capabilities Jira supplies.
Use events from the first pilot. Cron is not required and is not part of the new
workflow's design. A persistent local coordinator receives notifications, records
pending work durably, and dispatches it when a slot is available.

```text
Jira transition to Ready
  → Jira Automation sends an authenticated HTTPS request
  → HTTPS tunnel forwards it to our local listener
  → Coordinator validates and durably records the event, then acknowledges it
  → Worker loop rechecks readiness, claims the issue, and creates a worktree
  → Claude implements and opens PR; Review triggers Codex
  → Codex accepts with green checks; Ralph merges; Jira Automation sets Done
```

Start with a local listener reached through an HTTPS tunnel. Select the tunnel
provider and credential mechanism during setup; none is provisioned yet. Authenticate
incoming requests, expose only the event endpoint, and keep event handling separate
from long-running agent execution. Do not acknowledge successful receipt until the
event is durably stored. Deduplicate notifications and claims independently.

Pickup is prompt after delivery when the Mac is awake, connected, and has capacity;
it is not a guaranteed instantaneous launch. Recorded events survive a coordinator
restart. Events sent while the machine or tunnel is unavailable are not guaranteed
to reach local storage. Recover eligible work by querying current Jira/GitHub state
on startup and with an occasional internal timer. Recheck scope, readiness, and
ownership before admitting recovered work. This is recovery, not agent-based polling
or the normal pickup mechanism, and needs no cron job.

A local service manager can keep the coordinator running and restart it after failure;
it does not substitute for durable state or keep an asleep Mac executing. If accepting
events while the Mac is offline becomes necessary, add a hosted durable receiver
and queue. That is a later option, not a prerequisite for the local pilot.

## How we will build it together

### 1. Inspect native capabilities and enable access

When Ralph starts implementation, first inventory Jira Automation, Rovo, GitHub
integration, and Claude integration availability. Map responsibilities to native
features and identify only the remaining gaps. Confirm hosted versus local execution
with Ralph before writing a listener or provisioning a tunnel. Later local-bridge
steps apply only to gaps in the selected design.

Codex checks available connections and asks Ralph for the minimum service setup
needed at each step. Ralph authenticates through the service or a local credential
setup, never by sending secrets in chat.

Inspect the actual Jira site, project, board, management type, statuses, transitions,
and permissions. Confirm the separate backlog and five columns. Record nonsecret
configuration using actual IDs rather than guessed project names. Determine a
supported authentication method for scripts; an interactive connector connection
does not automatically provide unattended script credentials.

Check GitHub repository access, required checks, local Codex/Claude authentication,
and where the coordinator will run. Inspect any existing scheduler and workers before
changing execution ownership. Keep existing work intact.

Result: verified access and nonsecret configuration, with missing setup clearly listed.

### 2. Read Jira without executing anything

Write a small command that lists eligible Ready issues and explains why other issues
are excluded. Require clear approved scope and acceptance criteria, and exclude
blocked or canceled work. Decide the exact readiness representation from the real
Jira setup and existing repository conventions before enabling execution.

Handle pagination, authentication errors, rate limits, and malformed responses.
Treat issue content as task data, not instructions to override repository controls.
Preview must not launch agents or modify Jira, branches, worktrees, or task queues.

Result: Ralph and Codex can see precisely what would be picked up.

### 3. Receive events in preview mode

Build the local listener and durable event inbox, then configure the tunnel and a
Jira Automation rule for transitions into Ready. Inspect actual automation permission,
quota, and delivery behavior during setup; do not assume the retry semantics of other
Jira webhook mechanisms apply to this rule. Start in preview mode: record and display
eligibility without launching agents. Test duplicate delivery, invalid authentication,
restart recovery, and unavailable endpoints.

Result: a Ready transition reaches the coordinator and is recorded once, with no
agent run. Exact service configuration and commands are documented after verification.

### 4. Run one issue through the whole path

Select one small real issue together and enable event-driven execution only for that
pilot issue. A Ready transition uses the verified event path to record the issue
ID/key, approved scope revision, attempt, branch, worktree, worker identity, and PR.
Start with one active task and an explicit pilot issue allowlist.

Before launch, persist ownership and launch intent. Repeated runs must not create
duplicate workers. An uncertain launch pauses for reconciliation. Preserve dirty
worktrees and unpublished commits. Never automatically reset an owned worktree.

Claude implements and opens the PR. When required checks pass, Codex reviews the
implementation revision independently. Requested changes return to Claude within
a bounded revision allowance. Acceptance applies to the code being merged; later
code changes invalidate stale acceptance. Merge requires current acceptance and
green required checks. Notify Ralph and keep the issue in Review until he merges.
Jira Automation confirms the intended merge and evidence before moving it to Done.

Record failed Jira updates and retry them without rerunning implementation or merge.
Pause or cancellation must be checked before launch and merge. Keep failure reasons
visible and actionable.

Result: one issue reaches Done with a verified PR and no duplicate agent run.

### 5. Expand automatic pickup

After the pilot and failure tests pass, Ralph enables automatic execution for the
agreed scope. Use one coordinator for each issue. Prevent the existing repository
queue and Jira intake from dispatching the same work. Do not migrate the old backlog
or change existing scheduler ownership implicitly.

Use the persistent event-driven coordinator proven in the pilot. Set concurrency,
runtime ceilings, retry limits, and pause controls before expanding its scope.
The old scheduler is neither required nor implicitly disabled by this plan.

Result: moving approved work to Ready starts the bounded workflow automatically.

### Event handoffs used by the pilot and rollout

Use service events and worker-completion signals to invoke the same operations:

| Event | Action |
| --- | --- |
| Ralph moves issue to Ready | Validate authorization/readiness, claim once, and set In Progress |
| Claude opens/updates linked PR and sets Review | Wait for checks, then automatically request Codex review |
| Review requests changes | Set In Progress, launch bounded Claude revision, then return to Review |
| Acceptance and checks are current | Notify Ralph; remain in Review awaiting his merge |
| Ralph’s intended PR merge is confirmed | Jira Automation verifies evidence and sets Done; safely clean up idle worktree |

Use the local listener for external events and a worker wrapper/completion signal
for agent exits; confirm available CLI hooks during implementation rather than
assuming a specific hook exists. Authenticate external events, persist receipt,
deduplicate, and read authoritative state when events arrive late or out of order.
Startup recovery and the internal reconciliation timer use these same operations.

Result: event-driven handoffs throughout, without cron or LLM status polling.

## Token and failure limits

Proposed starting limits: one active task, one automatic review-fix round, one rebase
round, and at most two infrastructure retries with backoff. Agree on runtime ceilings
and available usage limits with Ralph before automatic execution. Exhaustion pauses
work instead of starting an endless loop. Infrastructure errors are not code-review
findings and must not spend implementation revision allowances.

Avoid unchanged-code reviews and agent-based polling. Give agents required repository
instructions and relevant scope/evidence, not other agents' full conversations.
Report measured usage only where available; missing usage is unknown, not zero.
Runtime and attempt limits reduce exposure but do not guarantee an exact token cap.

Test duplicate pickup, interrupted launch, active-worktree preservation, cancellation,
failed checks, stale acceptance, and Jira outage recovery with fixtures and stubbed
agent/service calls before unattended use. Live agent runs are deliberate pilot work,
not fixture tests. Worktree isolation is not a security sandbox; preserve existing
permission boundaries unless a specific change is explicitly agreed.

## Engineering practices we will build into the workflow

These practices are part of the delivery plan, regardless of team size. Introduce
implementation with the relevant increment below, rather than provisioning extra
services up front. The aim is to build a useful product while learning engineering
practices we can explain, measure, and operate.

### Execution state and ownership

Jira columns are a user-facing summary, not the coordinator's complete state model.
Before the pilot, define and test an explicit transition table for admission, claim,
launch intent, running, waiting for checks, reviewing, revising, merging, completion,
pause, cancellation, and unresolved failure. These are internal states, not proposed
additional Jira columns.

Each transition records its trigger, prior state, attempt identifier, timestamp,
owner, and evidence. Define which component can perform it and its prerequisites.
Use atomic claims and version checks so competing or delayed events cannot overwrite
newer state. Completion from an old attempt cannot advance a newer attempt. Do not
claim exactly-once event delivery; enforce idempotent effects and single ownership.
An ambiguous external side effect is reconciled before it is retried.

Jira owns readiness and user intent; repository artifacts own approved scope;
coordinator records own execution attempts; GitHub owns PR/check/merge evidence.
Define how their disagreements are resolved without silently discarding human edits.

### Observability

Provide structured, redacted logs and a simple status command before live pilot work.
Trace an issue through event delivery ID, attempt ID, worker session, worktree, PR,
reviewed revision, and merge commit. Show the current phase, last progress time,
waiting or blocked reason, retries used, and next permitted action.

Measure queue wait, execution and review duration, completion/failure counts,
revision counts, and reported token usage when available. Include coordinator health
and last successful reconciliation so silence is distinguishable from normal idleness.
Notify Ralph about actionable failures, required input, and completion; suppress
repetitive unchanged-state messages. Start with logs and a command, not a dashboard
service. Define retention and avoid storing unnecessary issue content or secrets.

### Security and authority

Before enabling live execution, document a permission matrix for the receiver,
coordinator, coding worker, reviewer, and merge operation. The receiver can validate
and record events, not execute arbitrary commands. Give workers only the repository
and service access required for their role; do not pass receiver secrets or merge
credentials into coding sessions. Ralph retains merge authority for this rollout;
use repository controls to enforce that boundary where possible. A future automated
merge identity requires explicit enablement and current acceptance/check gates.

Verify actual credential capabilities rather than treating an instruction as an
enforced restriction. Record residual authority where identities cannot be separated.
Keep secrets outside Git and prompts, redact logs, and document rotation/revocation.
Treat issue text, repository content, and external responses as potentially untrusted
input; they cannot change tool permissions, launch arbitrary shell commands through
interpolation, or bypass approval gates.

A worktree isolates changes, not processes or credentials. Choose and test an actual
worker isolation boundary before broad unattended use, based on required filesystem,
network, and tool access. Existing bypassed permissions are not automatically extended
to the new coordinator. Verify that pause/stop and credential revocation work.

### Agent quality evaluation

Maintain a small, versioned set of representative tasks and known failure cases with
expected outcomes. Prefer deterministic checks and human inspection where possible;
do not spend another agent call grading every mechanical action.

Assess scope adherence, functional correctness, unnecessary changes, recovery
behavior, and resource use. Include the Jira tooling mistake as a regression case:
development tooling must stay outside the application's Java/Maven build. Include
missing requirements, misleading issue instructions, and a test-passing change that
fails the requested user behavior.

Record model, prompt/instruction revision, tool versions, and relevant configuration
for reproducibility. Run a bounded baseline before the pilot and targeted evaluations
when those inputs change materially. Compare quality and measured usage rather than
assuming a newer model or a longer prompt is better. Keep paid evaluation runs within
an agreed budget; use fixtures for coordinator failures.

Independent AI review is a useful gate, not proof of correctness: both agents can
miss the same defect. Deterministic tests, acceptance evidence, and Ralph's product
feedback remain essential. Infrastructure Codex authors must not be described as
independently reviewed by that same authoring session; arrange a separate review
against the diff and evidence when we deliver it.

### Release verification and rollback

The board's Done continues to mean accepted and merged. It does not mean deployed
or proven healthy in production. Track release/environment status separately, linked
to the issue, PR, commit, and deployed artifact; no extra board column is required.

Before automating a deployment, define the destination, release trigger, authority,
reproducible artifact, environment configuration, smoke checks, and health criteria.
Record deployment evidence and monitor the real primary user flow after release.
A failed deployment or health check must surface clearly rather than being hidden by
a Done card. Do not deploy automatically merely because merge is authorized.

Document and rehearse rollback to a known-good artifact in a safe environment. Schema
changes need compatibility and recovery plans; reverting code does not reverse a
migration or restore lost data. Define backup/restore requirements before consequential
data changes. Apply release/version and rollback discipline to the coordinator too,
including compatibility with persisted execution state.

### Operational recovery

Before broad automatic pickup, write and exercise short runbooks for:

- Inspecting a stuck task without starting another worker.
- Reconciling launch uncertainty using worker identity and durable intent.
- Replaying an event safely after an outage or malformed delivery is corrected.
- Pausing admission and merge-ready handoffs globally; notifying Ralph not to merge
  paused work and stopping workers without deleting their work.
- Restarting after a crash and reconciling persisted state with Jira and GitHub.
- Recovering from Jira update failure after a successful merge.
- Rotating credentials and restoring coordinator state from a tested backup.

Distinguish request-to-cancel from confirmed worker termination. Never clear ownership
just to make a task runnable. Backups need an explicit retention/access policy;
restoring a backup requires reconciliation because external operations may have
succeeded after it was taken. Preserve evidence of incidents and record the fix that
prevents recurrence. Test normal recovery with stubs/disposable worktrees; any live
failure exercise is agreed separately and must not endanger household data.

## Learning and implementation evidence

For each increment, keep a short record beside this guide describing:

- What we built and the engineering concept it demonstrates.
- Why we chose this approach and the important tradeoff.
- Which failure we deliberately tested and the expected behavior.
- What evidence showed successful operation and recovery.
- What surprised us, what changed, and the next useful improvement.

These are implementation notes, not another experimental roadmap or product-task
pipeline. Learn through the real delivery path and bounded failure exercises.

Introduce the practices at these points:

| Increment | Evidence required |
| --- | --- |
| Access and intake preview | Service/credential boundaries, readiness rules, fixture tests |
| Event preview | Durable receipt, replay protection, structured logs, restart recovery |
| Single-issue pilot | State transitions, correlated status, limits, agent baseline, current-revision gates |
| Broader pickup | Exercised runbooks, verified worker isolation, alerts, state backup/restore |
| Deployment automation | Explicit release authority, smoke/health checks, rollback evidence |

The local receiver intentionally has laptop availability limits. Hosted buffering
becomes useful when accepting events during downtime is required; recording that
tradeoff honestly is part of learning to operate the system.

## Current position

Only this guide exists. No Jira setup, script implementation, new queued task,
authentication change, scheduler change, or live execution is performed by documenting
it. We remain in planning; next discuss any remaining setup choices, then wait for
Ralph to start implementation before inspecting account connections or provisioning
anything. Update this guide as decisions evolve and, later, with verified commands.
