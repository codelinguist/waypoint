# Design

Use for material UI work; mechanical UI changes and backend tasks do not need a
separate Design stage. Read the Jira issue, relevant product documents,
matching decisions selected through `docs/decisions/index.md`, and the existing
or proposed design artifact.

The implementer explores meaningfully different directions using
`agent/templates/ui-design-brief.md`. Codex approves a direction directly with
the user. Do not self-approve a draft or silently broaden scope. Record the
approved brief under `agent/ui/<feature-slug>/design-brief.md`. Implementation
starts only on a separate user request. Cover information hierarchy,
accessibility and keyboard behavior, wide/narrow layouts, and deliberate
loading, empty, error, and configuration states.
