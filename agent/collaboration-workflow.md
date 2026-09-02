# Codex and Claude Code Collaboration Workflow

## Purpose

Use each agent where it tends to add the most value while keeping the repository,
not chat history, as the source of truth.

This workflow is required for material UI features. It is optional for small,
mechanical UI changes and does not apply to backend-only tasks.

## Default responsibilities

### Claude Code: design explorer and implementer

- turn product requirements into two or three meaningfully different UI directions
- reason about information hierarchy, interaction flow, responsive behavior,
  accessibility, and visual coherence
- create the design brief using `agent/templates/ui-design-brief.md`
- implement the approved direction and integrate frontend, API, validation,
  and tests as required
- render or run the application and capture evidence for Codex's acceptance
  review, in place of a separate visual-review stage

### Product Owner Agent (Codex): product decision owner

- convert user-reported problems and feedback into durable product briefs
- define the user problem, desired outcome, priority, and boundaries
- decide whether a feature is ready for design and implementation
- own testable acceptance criteria and resolve product tradeoffs
- select and explicitly approve a design direction before implementation
- review rendered implementation evidence and record findings using
  `agent/templates/ui-visual-review.md`
- accept, reject, or defer changes found during that review
- accept the completed feature or return it with unmet acceptance criteria

The Product Owner Agent runs as Codex, in a task or session kept separate from
the implementation agent, so acceptance stays independent of the agent that
built the feature. Its complete instructions are in
`agent/roles/product-owner.md`. It owns routine, reversible product decisions
that can be grounded in repository evidence.

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

agent/ui/<feature-slug>/
  design-brief.md
  evidence/
  visual-review.md
```

The brief and review must be understandable without access to either agent's
conversation. Images and recordings belong in `evidence/`; do not put sensitive
household financial data in screenshots when representative fixtures will do.

## Workflow

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

When ready, it creates or updates `agent/current-task.md` as the active execution
contract. The task must name a user-facing feature or explicitly authorize
design exploration. Agents must not build a speculative frontend for a
backend-only task.

`agent/current-task.md` is overwritten in full for each new task, not appended
to — it always describes exactly one active task. History lives in
`agent/implementation-log.md` and the product brief's "Delivery handoff"
section, not in this file.

### 2. Explore with Claude Code

Claude Code reads the product, domain, architecture, decision, and current-task
documents listed in `AGENTS.md`. It then creates `design-brief.md` from the
template and proposes two or three distinct directions.

Each direction must explain tradeoffs rather than merely changing colors. The
brief must cover narrow and wide layouts, loading/empty/error states,
accessibility, and the visual distinction between facts, assumptions, goals,
recommendations, and decisions when relevant.

Status remains `DRAFT`.

### 3. Select a direction

The Product Owner Agent records the selected direction, any amendments, the
decision rationale, and status `APPROVED` in `design-brief.md`. It consults the
user only when the choice expresses a material, undocumented preference.

Approval authorizes implementation of that brief only. It does not authorize a
change to canonical financial data, a new product rule, or broader scope.

### 4. Implement with Claude Code

Claude Code checks the approved brief for conflicts or missing acceptance
criteria, then implements the smallest complete vertical increment. It
preserves domain logic outside the UI and uses deterministic application
services for financial calculations.

Before handoff, Claude Code must:

- run relevant unit, integration, type, and lint checks
- exercise the primary user flow
- render representative wide and narrow layouts
- place screenshots or other evidence in the feature's `evidence/` directory
- record commands, results, deviations, and known limitations in the brief
- update `agent/implementation-log.md`

If implementation reveals a material design change, status returns to `DRAFT`
until the Product Owner Agent approves the revised brief.

### 5. Review and decide on follow-up changes with the Product Owner Agent

Back in the Codex session, the Product Owner Agent inspects the approved brief
and the implementation evidence — it does not edit application code. Findings
go in `visual-review.md` and are classified as:

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
and re-renders affected layouts. It updates the review with verification
evidence, marks the brief `IMPLEMENTED`, and updates
`agent/implementation-log.md` again.

### 7. Accept the feature

The Product Owner Agent compares the verified result with the defined user
outcome and acceptance criteria. It either records feature acceptance or returns
the feature with specific unmet criteria. New ideas discovered during acceptance
become follow-up tasks rather than silently expanding the feature. The user may
always provide feedback or reject an outcome that does not solve the real
problem; the Product Owner Agent then reframes or reprioritizes the work.

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
