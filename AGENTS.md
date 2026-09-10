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

## Context loading

Use progressive disclosure. Load the smallest authoritative set for the current
stage, then open supporting material when the issue, diff, or a concrete finding
makes it relevant.

Always read this file and the applicable stage in
`agent/collaboration-workflow.md`. For Jira work, read the issue description in
full. Then load context by stage:

- **Frame:** read the full product set (`vision.md`, `user-zero.md`,
  `problems.md`, `jobs-to-be-done.md`, `principles.md`, and `roadmap.md`),
  `docs/domain/financial-model.md`, `docs/architecture/architecture.md`, and
  every Accepted decision in `docs/decisions/decisions.md`.
- **Design:** read the issue, approved or proposed design artifacts, relevant
  product documents, and decisions selected through the decision index. UI work
  also follows its design approval and visual-review gates.
- **Implement and Revise:** read the issue, current-round review findings when
  revising, affected code and contracts, and decisions selected through the
  decision index. Open product, domain, or architecture documents only when the
  work changes or depends on their rules.
- **Review and Accept:** read the issue, current PR diff and affected code,
  tests, current-round findings, and available evidence. Scan the decision index
  and read in full only decisions whose trigger matches the change or a finding.
  Open other product, domain, or architecture documents only when a specific
  acceptance criterion or finding turns on them.
- **Ship:** read the current acceptance record, PR revision, required check
  status, and the Ship stage. Load other context only to resolve a discrepancy.

A Superseded decision needs only its index entry and status pointer unless its
history is directly relevant. Do not load an entire document merely because it
is listed as durable context elsewhere.

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

Record what matters in its durable home rather than a task log:

- a new long-lived architectural or product decision goes in
  `docs/decisions/decisions.md`
- a missing or wrong rule, template, or doc that this task revealed gets
  corrected directly (see `agent/collaboration-workflow.md` ->
  "System evolution")
- an unresolved question or assumption worth a human decision goes on the
  Jira issue as a comment

Your assigned Jira issue holds the active task's scope and acceptance
criteria; do not treat it as a log — the Jira issue's comment history and
git history are the record of past tasks. Pre-existing
`agent/tasks/<NNN>-<feature-slug>.md` files are historical records only.
`agent/implementation-log.md` was retired and later removed outright (its
history remains in git history if ever needed) because a running log —
root or feature-local — is pure token-cost overhead: new feature work must
not create `agent/implementation-log.md` or a feature-local
`agent/product/<slug>/implementation-log.md`. The durable per-feature
contract belongs in `agent/product/<slug>/api.md` (or equivalent).

If a new long-lived architectural or product decision is made, add it to `docs/decisions/decisions.md`.

## Agent collaboration

- The user initiates each meaningful stage with a short command or request.
  Agents complete the requested stage autonomously, then report the result.
  Do not poll Jira, claim backlog tasks, launch unattended workers, advance to
  another stage, run automatic review/fix loops, or merge automatically.
- Treat the Jira issue and checked-in application code as the handoff
  boundary; do not rely on another agent's chat history.
- Codex is the Product Owner Agent: it frames problems by creating Jira
  issues, approves design direction, and independently accepts or returns
  work by commenting on the issue. See `agent/roles/product-owner.md` for its
  full responsibilities, autonomy boundaries, and artifact conventions.
- Claude Code is the default implementation and integration owner. These
  are defaults, not capability restrictions; an explicit task takes precedence.
- Ralph and his wife provide problems, preferences, corrections, and household
  authority. They need not write detailed prompts or act as product managers.
- Keep planning, implementation, and independent review in separate sessions.
  Research sub-agents may investigate, but implementation stays in the main
  conversation. Only one agent edits a feature at a time; concurrent tasks
  require separate worktrees with explicit ownership.
- Ship each task through its own branch and PR, never directly to main.
  Use `task/<issue-key>-<feature-slug>` (lowercase issue key) for Jira-driven
  feature work, `codex/<feature-slug>` for an explicitly requested repository
  change not tied to an issue, or the legacy `task/<NNN>-<feature-slug>` only
  for pre-existing numbered tasks.
- The implementation stage includes verification, pushing the branch, and
  opening/updating its PR. Codex may commit and push completed review findings
  and acceptance records to that task branch without asking again.
- Merging requires an explicit user ship/merge request, Product Owner
  acceptance recorded on the Jira issue, and a green required `verify` check
  for the current revision. Never bypass a failing, pending, or missing
  required check.
- Both agents share a GitHub account. The Jira issue's recorded independent
  review is authoritative; do not portray a same-account GitHub review as
  independent.
- Product acceptance never authorizes canonical financial-data changes or
  material household decisions. Ask when a choice requires an undiscoverable,
  material household preference.

See `agent/collaboration-workflow.md` for stage boundaries and evidence.
