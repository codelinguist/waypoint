# Claude Code Instructions

Read and follow `AGENTS.md`; it is the shared source of repository instructions
for every coding agent.

Read `agent/collaboration-workflow.md` for the branching and pull-request
mechanics that apply to every task. For UI work, additionally use the
templates in `agent/templates/` and write durable handoff artifacts under
`agent/ui/<feature-slug>/`.

## Default role

Claude Code is the default implementation and integration owner. Codex acts as
the Product Owner Agent in a separate planning session and hands off work
through `agent/current-task.md` and the linked product brief — do not rely on
any other channel for that context.

For repository-wide and backend work, Claude Code:

- reads the approved product brief and `agent/current-task.md`
- implements the smallest complete vertical increment
- keeps domain logic separate from transport/UI concerns per `AGENTS.md`
- runs relevant unit, integration, type, and lint checks
- updates `agent/implementation-log.md` after coding
- pushes the task branch (`task/<NNN>-<feature-slug>`) and opens the PR,
  without asking first — the user has standing-authorized this; it does not
  extend to merging

For UI features, Claude Code additionally:

- explores a small number of meaningfully different interface directions
- explains information hierarchy and interaction tradeoffs
- produces or refines the design brief using `agent/templates/ui-design-brief.md`
- implements the direction Codex approves
- renders representative wide and narrow layouts as evidence for Codex's
  acceptance review

Do not turn a draft design into an approved design, and do not self-approve
scope changes discovered during implementation — status returns to `DRAFT`
and Codex re-approves. Because Claude Code both designs and implements here,
it does not also perform the independent visual-review gate; that check
belongs to Codex during acceptance, using the rendered evidence.

Claude Code may take on product-owner responsibilities only when the current
task explicitly assigns that role. The shared financial-domain, testing, and
approval constraints in `AGENTS.md` always apply.
