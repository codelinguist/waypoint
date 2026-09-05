# AGENTS.md

This repository contains a personal AI financial planning application initially built for Ralph and his wife.

## Mission

Build a long-lived household financial operating system that combines:

- structured financial state
- deterministic financial calculations
- scenario modeling
- goals and planning
- historical financial snapshots
- AI-assisted interpretation and recommendations
- auditable user decisions

## Product principles

1. The repository and application data are the source of truth.
2. The LLM is never the authoritative financial datastore.
3. Important financial arithmetic must be deterministic and testable.
4. Facts, assumptions, goals, recommendations, and decisions are distinct concepts.
5. Never silently convert an inferred value into a financial fact.
6. Any material change proposed by AI must require explicit user approval before persistence.
7. Prefer simple, auditable designs over agentic complexity.
8. Prefer small vertical increments over broad scaffolding.
9. Do not add infrastructure until required by a concrete feature.
10. This is initially a private household product, not a generic SaaS platform.

## Before coding

Read:

1. `docs/product/vision.md`
2. `docs/product/user-zero.md`
3. `docs/product/problems.md`
4. `docs/product/principles.md`
5. `docs/domain/financial-model.md`
6. `docs/architecture/architecture.md`
7. `docs/decisions/decisions.md`
8. `docs/product/roadmap.md`
9. Your assigned file in `agent/tasks/` (see `agent/tasks/README.md`)

Also read `agent/collaboration-workflow.md` for the branching and pull-request
mechanics that apply to every task. UI work additionally follows its design
approval and visual-review gates.

## Implementation style

- Keep domain logic separate from transport/UI concerns.
- Financial calculations must be callable without an LLM.
- Favor explicit types and domain objects.
- Persist timestamps and provenance for material financial values.
- Tests should focus on financial rules and domain behavior, not only endpoint coverage.
- Avoid premature event sourcing, microservices, queues, or vector databases.
- A modular monolith is preferred initially.

## AI behavior

The AI may:

- interpret natural-language user input
- retrieve current financial state
- explain financial position
- run deterministic scenario tools
- propose changes
- summarize tradeoffs
- surface inconsistencies
- create draft recommendations

The AI may not:

- silently change canonical financial values
- treat assumptions as facts
- perform important financial arithmetic only inside free-form model reasoning
- present uncertain inferred data as confirmed
- execute investment, banking, or payment transactions in the initial product

## After coding

Update `agent/implementation-log.md` with:

- what changed
- tests added
- architectural decisions made
- any assumptions introduced
- unresolved questions
- recommended next task
- any rule, template, or doc that should change because of what this task
  revealed (see `agent/collaboration-workflow.md` -> "System evolution")

`agent/tasks/<NNN>-<feature-slug>.md` holds your one active task; do not
overwrite another task's file, and do not treat any of them as a log —
`agent/implementation-log.md`, the linked product brief, and git history are
the record of past tasks. See `agent/tasks/README.md` for the status
lifecycle the orchestrator drives these files through.

If a new long-lived architectural or product decision is made, add it to `docs/decisions/decisions.md`.

## Agent collaboration

- Treat checked-in briefs and application code as the handoff boundary between
  agents; do not rely on another agent's chat history.
- A dedicated Product Owner Agent owns problem framing, priority, scope,
  acceptance criteria, design decisions, and evidence-based feature acceptance.
- Ralph and his wife are users and household authorities. They provide problems,
  context, corrections, preferences, and feedback; they are not expected to act
  as product managers.
- Up to 3 tasks may be implemented at once, each strictly isolated in its own
  Git worktree and branch — see `agent/tasks/README.md`. Outside that
  automated pipeline, only one agent should edit a feature at a time unless
  the work is isolated in separate worktrees with explicit ownership.
- Codex acts as the Product Owner Agent. It frames problems, writes product
  briefs, writes new `QUEUED` files in `agent/tasks/`, approves design
  direction, and accepts or returns completed work against evidence. Claude
  Code invokes it non-interactively through the local `codex` CLI (`codex
  exec` for framing, brief-writing, design approval, and acceptance; plain
  `codex exec` with a diff-yourself prompt for PR review — `codex review` /
  `codex exec review` cannot combine `--base`/`--commit` with a custom prompt
  on the installed codex-cli version, see `agent/automation/review-prompt.md`)
  rather than the user running a separate manual Codex session — see
  `agent/collaboration-workflow.md` -> "Invoking the Product Owner Agent" for
  the mechanics. Each invocation reads only checked-in artifacts (the product
  brief, the relevant `agent/tasks/` file, the PR diff); it is never given
  Claude Code's planning or implementation conversation, preserving the same
  independent-review boundary a separate session would.
- `agent/automation/orchestrator.sh`, run on a schedule, is what makes this
  automatic in practice: it claims `QUEUED` tasks and dispatches unattended
  Claude Code workers in dedicated worktrees, and separately reviews and
  merges open PRs via Codex. See `agent/collaboration-workflow.md` ->
  "Automated pipeline" for what that changes about this project's safety
  posture (bypassed permissions/sandbox and automatic merge, both scoped to
  that pipeline only) and why.
- Claude Code is the default implementation and integration owner for
  repository-wide work, deterministic behavior, and tests. For UI features it
  also explores directions and writes the design brief before implementing.
- Research splits by role: Codex investigates the product and problem space
  (existing product context, prior briefs, user input) as part of framing;
  Claude Code investigates the codebase and external documentation (patterns,
  libraries, prior implementations) since it is the one that actually touches
  code. See `agent/collaboration-workflow.md` -> "Default responsibilities".
  Either way, sub-agents (Claude Code's own, or a research pass run through
  the `codex` CLI) are for research only — never for writing implementation
  code. Coding stays in the main conversation, which needs the full history
  of files already touched; delegating it away risks hallucinated changes
  that don't fit what's actually there.
- Planning and implementation run in separate conversations. Once a product
  brief or design brief is approved, Claude Code starts a fresh conversation
  for implementation, loaded with the approved brief and the documents this
  file lists — not the exploration conversation that produced the brief. See
  `agent/collaboration-workflow.md` -> "Workflow" for where this applies. An
  orchestrator-dispatched worker satisfies this by construction: it always
  starts cold in its own worktree.
- Every task ships on its own branch (`task/<NNN>-<feature-slug>`) via a pull
  request, never a direct commit to `main`. The user has standing-authorized
  Claude Code to push the branch and open the PR itself as part of completing
  a task, without asking each time.
- Every pull request targeting `main` must pass the required `verify` GitHub
  Actions check, which runs the same root-level `./verify.sh` command Claude
  Code runs locally before opening or updating a PR. Neither agent, nor the
  orchestrator, may merge, or routinely bypass via administrator override, a
  PR whose required check is failing, pending, or missing.
- The Product Owner Agent (Codex) has standing authorization to commit and
  push its completed review findings and acceptance record directly to the
  task branch, without asking each time.
- Merging requires both the Product Owner Agent's acceptance (`ACCEPTED` in
  the product brief) and a green required check. Once both hold, the
  orchestrator merges automatically — no further human step. The user, Codex,
  or Claude Code (if explicitly asked) may still merge by hand for a task run
  outside the automated pipeline. See `agent/collaboration-workflow.md` ->
  "Automated pipeline" for the reasoning behind removing the manual
  checkpoint here specifically.
- Claude Code and Codex currently authenticate to GitHub as the same account,
  so GitHub cannot provide an independent formal approving review. The
  product brief's recorded findings and acceptance — not a same-account
  GitHub review — are the durable, authoritative record of independent
  review. See `agent/collaboration-workflow.md` -> "Branching and pull
  requests".
- These are defaults, not capability restrictions. The current task, acceptance
  criteria, and verified output take precedence over agent identity.
- The Product Owner Agent may make reversible product decisions from documented
  evidence. It must ask the user when a choice depends on an undiscoverable,
  material household preference.
- Product acceptance does not authorize changes to canonical financial data or
  substitute for user approval of a material household financial decision.
