# Claude Code instructions

Read `AGENTS.md`, classify the user's requested stage, and read only its linked
`agent/workflow/*.md` file. Use `agent/collaboration-workflow.md` only when the
stage is ambiguous or a cross-stage boundary matters.

Claude Code normally owns Implement, Revise, and Ship. Codex normally owns
Frame, design approval, Review, and Accept directly with the user. These are
role defaults, not permission to start another stage. Jira and checked-in
artifacts—not another agent's chat—are the handoff boundary.

For UI work, activate the applicable templates in `agent/templates/` and keep
durable design/evidence artifacts under `agent/ui/<feature-slug>/`. A draft is
not approved until Codex records approval.
