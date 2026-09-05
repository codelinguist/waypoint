# Codex and Claude Code Collaboration Workflow

## Purpose

Use each agent where it tends to add the most value while keeping the repository,
not chat history, as the source of truth.

The design-brief and visual-review stages are required for material UI
features and optional for small, mechanical UI changes; they do not apply to
backend-only tasks. Branching and pull requests (below) apply to every task
regardless of size.

## Default responsibilities

### Claude Code: design explorer and implementer

- research the codebase and external documentation needed to explore
  directions and plan implementation — the technical side of the research
  split described below
- turn product requirements into two or three meaningfully different UI directions
- reason about information hierarchy, interaction flow, responsive behavior,
  accessibility, and visual coherence
- create the design brief using `agent/templates/ui-design-brief.md`
- implement the approved direction and integrate frontend, API, validation,
  and tests as required
- render or run the application and capture evidence for Codex's acceptance
  review, in place of a separate visual-review stage

### Product Owner Agent (Codex): product decision owner

- research the product and problem space — existing product context, prior
  briefs, user input — the product side of the research split described below
- convert user-reported problems and feedback into durable product briefs
- define the user problem, desired outcome, priority, and boundaries
- decide whether a feature is ready for design and implementation
- own testable acceptance criteria and resolve product tradeoffs
- select and explicitly approve a design direction before implementation
- review rendered implementation evidence and record findings using
  `agent/templates/ui-visual-review.md`
- accept, reject, or defer changes found during that review
- accept the completed feature or return it with unmet acceptance criteria

The Product Owner Agent runs as Codex. Claude Code invokes it non-interactively
through the local `codex` CLI rather than the user running a separate manual
session, so acceptance stays independent of the agent that built the feature:
each invocation starts from zero shared context and reads only checked-in
artifacts (the product brief, the relevant `agent/tasks/` file, the PR diff) — never
Claude Code's planning or implementation conversation. Its complete
instructions are in `agent/roles/product-owner.md`. It owns routine, reversible
product decisions that can be grounded in repository evidence.

### Invoking the Product Owner Agent

Claude Code (or the user directly) triggers Codex through the `codex` CLI,
already authenticated on this machine against the user's ChatGPT
subscription. This automates the mechanics of the Product Owner role; it does
not change its authority or responsibilities above.

- **Framing, brief-writing, design approval, acceptance** — run through
  `codex exec`, sandboxed as `workspace-write` so Codex can write
  `agent/product/<feature-slug>/product-brief.md`,
  `agent/ui/<feature-slug>/design-brief.md`, and `agent/tasks/<NNN>-<feature-slug>.md`, and
  commit its own changes to the task branch (its existing standing
  authorization to commit findings and acceptance records applies the same
  way here).
- **PR review** — intended to run through `codex review` (or `codex exec
  review`) with `--base main` or `--commit <sha>`, purpose-built for
  reviewing a diff non-interactively. On the installed codex-cli version
  this does not work combined with a custom prompt — both reject
  `--base <branch> "<prompt>"` with `error: the argument '--base <BRANCH>'
  cannot be used with '[PROMPT]'`. Until a codex-cli release fixes this, use
  plain `codex exec` and have Codex diff the branch itself (`gh pr diff
  <number>` or `git diff main...HEAD`) — see
  `agent/automation/review-prompt.md` for the working prompt shape. Prefer
  the `review` subcommand again the moment it accepts a custom prompt.
- **Follow-up questions** — Codex may end an invocation by asking a
  clarifying question instead of finishing the brief. Claude Code relays that
  question to the user in the same conversation — no tool switch required —
  then continues the same Codex session with
  `codex exec resume <session-id> "<answer>"` rather than starting a fresh
  one, so the back-and-forth reads as one continuous framing conversation on
  Codex's side.
- **Context boundary** — every invocation's prompt points Codex only at
  checked-in files. Never paste Claude Code's conversation into a `codex`
  prompt; if context beyond a file's content is needed, put it in the file
  first.
- The user can always run `codex` interactively themselves instead, for any
  step where they want to be directly in the loop.

### Automated pipeline

`agent/automation/orchestrator.sh`, run repeatedly on a schedule (a local
cron job — see `agent/automation/README.md`), makes steps 4 through 7 below
run without a human driving each one: the user and Codex still frame tasks
together (step 1), but once a task is `QUEUED` in `agent/tasks/`, the
orchestrator claims it, dispatches an unattended Claude Code worker in a
dedicated git worktree, reviews the resulting PR with Codex, applies bounded
automatic fix rounds if Codex finds `BLOCKING` findings, and merges once
Codex records acceptance and the required `verify` check is green.

This is a deliberate, explicit change to two things this document previously
treated as fixed, made because the user asked to see what a mature,
highly-parallel version of this workflow looks like, independent of this
being a single-developer household project:

- **Asymmetric sandboxing.** Nobody is present to answer a permission prompt
  in an unattended run. Claude workers still use
  `--permission-mode bypassPermissions`: Claude's `--restricted` mode removes
  Bash, which these workers require for Git, Maven, and `./verify.sh`, and its
  background-session state is tied to the invoking macOS user. A worktree only
  isolates their Git branch; it does not restrict filesystem, network, process,
  or credential access. Codex review now runs with
  `-s workspace-write -c sandbox_workspace_write.network_access=true`, which
  confines filesystem/process writes to its workspace while retaining the
  network access needed for `gh pr diff` and `git push`. Network access has no
  host allowlist, and Codex still receives `GH_TOKEN`. A manually provisioned,
  repository-scoped token at `agent/automation/state/gh-token.scoped` reduces
  that credential's blast radius; setup and fail-closed behavior are documented
  in `agent/automation/README.md`. Containerized workers or a restricted OS
  account remain the next hardening step before this pipeline is trusted with
  anything more sensitive than this private repository.
- **Automatic merge.** Merging previously required a human, Codex, or
  Claude-Code-only-when-asked to act after acceptance; the orchestrator now
  merges the moment both gates are satisfied, with no further human step.
  The same-GitHub-identity limitation below still applies — Codex's recorded
  acceptance in the product brief, not a GitHub approving review, remains the
  authoritative independent-review record.

Bounded concurrency and safety nets, so "parallel" doesn't trade correctness
for throughput:

- At most 3 tasks may be `IN_PROGRESS` at once (`agent/tasks/README.md`).
  Beyond that, queued tasks simply wait for a slot.
- Before merging, the orchestrator checks the branch's new Flyway migrations
  against `main`'s for a colliding version number (two parallel tasks both
  claiming, say, `V6`). A collision stalls that task (`STALLED`, needs a
  human to rebase and renumber) rather than merging a schema that would
  break on the next deploy.
- A `BLOCKING` review gets a bounded number of automatic fix rounds (default
  2, `agent/tasks/README.md`'s `fix_rounds` field) before the task is marked
  `STALLED` for a human instead of looping indefinitely.
- Two parallel tasks independently touching the same shared file (most
  often `README.md`'s Status prose or `agent/implementation-log.md`'s
  append point, occasionally an actual shared code file like
  `ApiExceptionHandler`) is expected, not a bug, once more than one task is
  ever `IN_PROGRESS` at a time. Prevention: `agent/automation/
  worker-prompt.md` has each worker merge `main` into its own branch and
  resolve conflicts (keeping both sides' independent additions) before
  opening its PR. Self-healing for a conflict that appears later anyway
  (introduced by a sibling task merging afterward): the orchestrator
  detects `gh pr view --json mergeable == CONFLICTING` at merge time and
  dispatches the same bounded number of automatic rounds as a `BLOCKING`
  finding does, with the worker doing the actual rebase-and-resolve, before
  falling back to `STALLED`. Confirmed against a real two-task run where
  both mechanisms were needed.
- The orchestrator's own git operations (claiming tasks, running Codex's
  review, merging) happen in a dedicated clone under
  `../waypoint-orchestrator/`, never in a human's interactive checkout — see
  `agent/automation/README.md`.

A task can still be run the fully manual way described in the rest of this
document — write a `QUEUED` file, but implement, review, and merge it
yourself via an interactive session — if you want to be in the loop for a
particular piece of work; the orchestrator only acts on tasks it finds
`QUEUED`, `IN_REVIEW`, or eligible for a fix round.

### User and household authority

Ralph and his wife are users, domain-context sources, and household authorities.
They:

- present problems, goals, observations, corrections, and feedback
- answer questions when a material preference cannot be discovered or safely
  inferred
- approve changes to canonical household financial data
- approve material household recommendations, decisions, and financial rules

They are not required to write product briefs, prioritize implementation detail,
choose routine interface conventions, or perform feature acceptance testing.

Product ownership is separate from authority over household finances. Accepting
a feature does not approve a recommendation, scenario, canonical-data change,
investment action, banking action, or household policy. Those require the
explicit approval prescribed by the product's financial rules.

These responsibilities are defaults, not claims that an agent cannot perform
another role. A current task may override them explicitly.

## Durable artifacts

Each material feature gets a product brief, and each material UI feature also
gets UI artifacts:

```text
agent/product/<feature-slug>/
  product-brief.md
  implementation-plan.md   # optional — see agent/templates/implementation-plan.md

agent/ui/<feature-slug>/
  design-brief.md
  evidence/
  visual-review.md
```

The brief and review must be understandable without access to either agent's
conversation. Images and recordings belong in `evidence/`; do not put sensitive
household financial data in screenshots when representative fixtures will do.

## Branching and pull requests

Every task — UI or backend-only — ships through a branch and a pull request,
not a direct commit to `main`. This is the general delivery mechanism; the
UI-specific design-brief and visual-review artifacts above apply only when the
task includes UI work.

- One branch per task, cut from `main`: `task/<NNN>-<feature-slug>`, matching
  the task number and slug in its `agent/tasks/` file and
  `agent/product/<feature-slug>/`.
- Claude Code pushes the branch and opens the PR itself when implementation
  (or a fix round) is ready for review, without asking per task. The user has
  standing-authorized this so the loop stays hands-off; it does not extend to
  merging.
- Before opening or updating a PR, Claude Code runs the canonical repository
  verification command, `./verify.sh` (repository root), which runs the
  complete Java 21 Maven suite including the PostgreSQL/Testcontainers
  integration tests. The same command runs, unmodified, as the required
  `verify` GitHub Actions check on every PR targeting `main`; there is no
  separate local-only or CI-only verification path.
- The PR description must link its `agent/tasks/` file, the linked
  product brief, and the design brief when one applies, and must record: the
  local `./verify.sh` result, the CI `verify` check result and run link, the
  primary user flow as manually exercised, applicable UI evidence, and any
  deviations or known limitations (mirroring what step 4 below requires in
  the brief, and the pull-request template).
- The Product Owner Agent reviews the PR diff and evidence — via `codex review`
  / `codex exec review` against the PR branch (see "Invoking the Product Owner
  Agent" above), or `gh pr diff` / `gh pr view` when the user runs Codex
  interactively — instead of raw working-tree files. Findings
  are still recorded in the product brief (and `visual-review.md` for UI
  work); referencing the PR number is enough, PR review comments are not the
  durable record. The Product Owner Agent (Codex) has standing authorization
  to commit and push its completed review findings and acceptance record
  directly to the task branch, without asking each time.
- Claude Code and Codex currently authenticate to GitHub as the same account,
  so GitHub cannot provide an independent formal approving review. The
  product brief's recorded findings and acceptance are the durable,
  authoritative record of independent review; a same-account GitHub review
  must never be represented as one.
- Merging requires both the Product Owner Agent marking the brief `ACCEPTED`
  and the PR's required `verify` check reporting green. Once both hold, a
  task running through `agent/automation/orchestrator.sh` merges
  automatically — see "Automated pipeline" above. A task run the manual way
  instead is merged by the user, by the Product Owner Agent (Codex), or by
  Claude Code only when the user explicitly asks. Neither agent nor the
  orchestrator merges, nor routinely bypasses via administrator override, a
  PR whose required check is failing, pending, or missing; recovering from a
  genuinely broken check definition is a deliberate, explicitly-decided
  exception, not a routine action.
- Up to 3 task branches may be open at once, one per `IN_PROGRESS` file in
  `agent/tasks/`, each isolated in its own git worktree — see
  `agent/tasks/README.md` and "Automated pipeline" above.

## Workflow

This cycles through three phases, repeated once per feature:

- **Plan** — steps 1-3: frame the problem, explore directions, approve one.
- **Implement** — step 4: build the approved direction.
- **Validate** — steps 5-7: review, fix, accept.

Plan and Implement run in separate Claude Code conversations. Once the
Product Owner Agent approves a brief, start a fresh conversation for step 4,
seeded with only the approved brief and the documents `AGENTS.md` lists — not
the exploration conversation that produced it. The brief exists precisely so
implementation doesn't need that conversation; carrying it forward defeats the
point and lets implementation quietly lean on reasoning nobody wrote down.

### 0. User presents a problem

The user describes the problem in natural language and may provide examples,
constraints, corrections, or desired outcomes. The user does not need to define
a solution or produce implementation-ready requirements.

### 1. Product Owner Agent frames and authorizes the feature

The Product Owner Agent investigates existing product context and creates
`agent/product/<feature-slug>/product-brief.md` from the product-brief template.
It defines the outcome, priority, scope, risks, out-of-scope boundaries, and
testable acceptance criteria. It asks the user only when missing input would
materially alter the outcome or relies on household authority.

When ready, it writes a new `QUEUED` file in `agent/tasks/` (see
`agent/tasks/README.md` for the format) as the active execution contract. The
task must name a user-facing feature or explicitly authorize design
exploration. Agents must not build a speculative frontend for a
backend-only task.

Each task gets its own file, written once and then owned by
`agent/automation/orchestrator.sh` for every status transition after that —
it is not edited by hand once queued, and it is not a log. History lives in
`agent/implementation-log.md` and the product brief's "Delivery handoff"
section, not in the task file.

### 2. Explore with Claude Code

Claude Code reads the product, domain, architecture, decision, and current-task
documents listed in `AGENTS.md`. It then creates `design-brief.md` from the
template and proposes two or three distinct directions.

Each direction must explain tradeoffs rather than merely changing colors. The
brief must cover narrow and wide layouts, loading/empty/error states,
accessibility, and the visual distinction between facts, assumptions, goals,
recommendations, and decisions when relevant.

Claude Code may use research sub-agents here to investigate the codebase or
gather documentation — a different kind of research from the product and
problem investigation Codex already did in step 1, and never a stand-in for
drafting implementation code, which stays in the main conversation once step
4 begins.

Status remains `DRAFT`.

### 3. Select a direction

The Product Owner Agent records the selected direction, any amendments, the
decision rationale, and status `APPROVED` in `design-brief.md`. It consults the
user only when the choice expresses a material, undocumented preference.

Approval authorizes implementation of that brief only. It does not authorize a
change to canonical financial data, a new product rule, or broader scope.

### 4. Implement with Claude Code

Usually this is `agent/automation/orchestrator.sh` claiming the `QUEUED` task
and dispatching an unattended Claude Code worker into a fresh git worktree —
see "Automated pipeline" above. The rest of this step describes what that
worker does, and applies identically to a human-driven session working the
task the manual way instead.

In a fresh conversation seeded only with the approved brief — not the step 2/3
exploration conversation, per the Plan/Implement/Validate note above — Claude
Code checks the approved brief for conflicts or missing acceptance criteria.
For a task complex enough that the brief alone leaves real ambiguity about
where things go, it first writes
`agent/product/<feature-slug>/implementation-plan.md` from
`agent/templates/implementation-plan.md` — file-level tasks, patterns to
mirror, and a validation command per task — before writing any code; small,
well-bounded tasks can skip straight to implementing. Either way, Claude Code
implements the smallest complete vertical increment. It
preserves domain logic outside the UI and uses deterministic application
services for financial calculations.

Before handoff, Claude Code must:

- run the canonical verification command, `./verify.sh`, plus any other
  relevant type/lint checks
- exercise the primary user flow
- render representative wide and narrow layouts (UI tasks only)
- place screenshots or other evidence in the feature's `evidence/` directory
  (UI tasks only)
- record commands, results, deviations, and known limitations in the brief
- update `agent/implementation-log.md`
- push the task branch and open the PR per "Branching and pull requests" above

If implementation reveals a material design change, status returns to `DRAFT`
until the Product Owner Agent approves the revised brief.

### 5. Review and decide on follow-up changes with the Product Owner Agent

Usually `agent/automation/orchestrator.sh` triggers this automatically as
soon as it sees a new commit on an open `task/*` PR (`agent/automation/
review-prompt.md`); otherwise, Claude Code invokes the Product Owner Agent
against the PR by hand (`codex exec` with a diff-yourself prompt, per
"Invoking the Product Owner Agent" above). Either way, Codex inspects the
approved brief and the PR's diff and evidence — it does not edit application
code. Findings
go in the product brief (and `visual-review.md` for UI work) and are
classified as:

- `BLOCKING`: prevents correct, accessible, or usable completion
- `RECOMMENDED`: meaningful improvement that remains within approved intent
- `OPTIONAL`: polish with a weak cost-benefit case

Every finding must cite visible evidence and a concrete acceptance condition.
Preference alone is not a defect. The Product Owner Agent marks each finding
`ACCEPTED`, `REJECTED`, or `DEFERRED`. Blocking correctness or accessibility
defects still require resolution, but the remedy must not silently introduce a
new product decision.

### 6. Apply and verify with Claude Code

Claude Code applies accepted findings, reruns the relevant automated checks,
and re-renders affected layouts if applicable. It pushes the fix commits to
the same task branch and PR, updates the review with verification evidence,
marks the brief `IMPLEMENTED`, and updates `agent/implementation-log.md`
again. In the automated pipeline this is a bounded automatic fix round (see
"Automated pipeline" above); past `agent/tasks/README.md`'s `fix_rounds`
limit, the task stops at `STALLED` for a human instead.

### 7. Accept the feature

The Product Owner Agent compares the verified result with the defined user
outcome and acceptance criteria. It either records feature acceptance or returns
the feature with specific unmet criteria. New ideas discovered during acceptance
become follow-up tasks rather than silently expanding the feature. The user may
always provide feedback or reject an outcome that does not solve the real
problem; the Product Owner Agent then reframes or reprioritizes the work.

Acceptance authorizes the merge. In the automated pipeline it also performs
it, immediately and without a further human step — see "Automated pipeline"
above. Outside that pipeline, acceptance authorizes but does not perform the
merge — see "Branching and pull requests" above.

## Completion scorecard

A UI feature is complete only when all applicable answers are yes:

- Does it satisfy the approved user flow and acceptance criteria?
- Does it preserve the repository's financial-domain boundaries?
- Are facts and assumptions visually and semantically distinguishable?
- Are canonical changes protected by explicit approval?
- Do empty, loading, validation, and failure states behave deliberately?
- Is the primary flow keyboard accessible and usable at narrow widths?
- Do automated checks pass?
- Does rendered evidence match the approved direction?
- Are deviations and deferred findings recorded?

## Avoiding agent churn

- Do not pass the same code back and forth for unconstrained aesthetic edits.
- Do not let the Product Owner Agent rewrite the approved direction during
  review; findings are constraints on the existing direction, not a new one.
- Do not run both agents as simultaneous editors in the same working tree.
- Stop after one review-and-fix cycle unless blocking findings remain or the
  Product Owner Agent explicitly requests another polish pass.
- Keep the Product Owner Agent (Codex) session separate from the
  implementation agent (Claude Code) session so acceptance evidence is judged
  independently of the agent that produced it.
- Keep Plan and Implement in separate Claude Code conversations too, for the
  same reason: implementation should stand on what the brief says, not on
  reasoning that only exists in the exploration conversation.
- Do not raise `agent/tasks/README.md`'s concurrency bound past 3 without
  also reconsidering the migration-collision and fix-round safety nets in
  "Automated pipeline" above — they were sized for "a few things in flight
  at once," not arbitrary parallelism.

## System evolution

A defect found during review or acceptance is two things, not one: something
to fix in this feature, and a signal about what's missing from the shared
rules that let it happen. Before closing out a finding, ask explicitly: does
`AGENTS.md`, a template, or a `docs/` file need to change so this class of
mistake can't recur — and if a regression test can catch it going forward,
add one.

Don't fold that change into the feature's fix silently. Propose it as its own
small, explicit edit, and say why, in `agent/implementation-log.md`. This is
deliberate evolution of shared rules, not autonomous rewriting of them — the
same care that applies to a material design change applies here.
