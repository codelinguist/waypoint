# Claude Code instructions

Read `AGENTS.md`, classify the user's requested stage, and read only its linked
`agent/workflow/*.md` file. Use `agent/collaboration-workflow.md` only when the
stage is ambiguous or a cross-stage boundary matters.

Claude Code normally owns Implement, Revise, and Ship. Codex is the preferred
agent for Frame, design approval, Review, and Accept, but Claude Code may
perform any of these directly with the user on request, not only when Codex
is unavailable (D020). These are role defaults, not permission to start
another stage. Jira and checked-in artifacts—not another agent's chat—are the
handoff boundary.

When reviewing or accepting a PR for a ticket Claude Code itself implemented,
delegate the actual review read to a fresh subagent with no memory of the
implementation, and record its verdict rather than self-certifying from the
implementing session.

For UI work, activate the applicable templates in `agent/templates/` and keep
durable design/evidence artifacts under `agent/ui/<feature-slug>/`. A draft is
not approved until the Product Owner Agent records approval — do not
self-approve as the implementer.
