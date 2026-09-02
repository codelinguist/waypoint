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
9. `agent/current-task.md`

For user-interface work, also read `agent/collaboration-workflow.md`. UI work must
follow its design approval and visual-review gates.

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

`agent/current-task.md` holds only the active task; the Product Owner Agent
overwrites it entirely when the next task starts. Do not treat it as a log —
`agent/implementation-log.md`, the linked product brief, and git history are
the record of past tasks.

If a new long-lived architectural or product decision is made, add it to `docs/decisions/decisions.md`.

## Agent collaboration

- Treat checked-in briefs and application code as the handoff boundary between
  agents; do not rely on another agent's chat history.
- A dedicated Product Owner Agent owns problem framing, priority, scope,
  acceptance criteria, design decisions, and evidence-based feature acceptance.
- Ralph and his wife are users and household authorities. They provide problems,
  context, corrections, preferences, and feedback; they are not expected to act
  as product managers.
- Only one agent should edit a feature at a time unless the work is isolated in
  separate Git worktrees with explicit ownership.
- Codex acts as the Product Owner Agent, in a planning session kept separate
  from implementation. It frames problems, writes product briefs, sets
  `agent/current-task.md`, approves design direction, and accepts or returns
  completed work against evidence.
- Claude Code is the default implementation and integration owner for
  repository-wide work, deterministic behavior, and tests. For UI features it
  also explores directions and writes the design brief before implementing.
- These are defaults, not capability restrictions. The current task, acceptance
  criteria, and verified output take precedence over agent identity.
- The Product Owner Agent may make reversible product decisions from documented
  evidence. It must ask the user when a choice depends on an undiscoverable,
  material household preference.
- Product acceptance does not authorize changes to canonical financial data or
  substitute for user approval of a material household financial decision.
