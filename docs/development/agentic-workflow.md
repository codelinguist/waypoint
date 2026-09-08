# Agentic development workflow

The workflow is user-initiated and command-driven. The user selects a task and
stage; the agent retrieves context and completes that stage with the structure
in [the collaboration workflow](../../agent/collaboration-workflow.md).

Use /prime to load context and /codex frame, design, review, or accept to
request a Product Owner stage in Claude Code. Implementation, revision, and
shipping can be requested in plain language with a task/Jira key or PR.
Dedicated commands for these stages can be introduced as small follow-ups.

The Frame stage's concrete output is now a Jira issue: Codex writes the
outcome, scope, non-goals, and acceptance criteria directly into the issue
and moves it to To Do, rather than into a repository product brief and
numbered task file. Durable, cross-feature documentation — the roadmap,
decisions log, and other product/domain docs required by AGENTS.md — is
unchanged and is what Codex reads to decide what to frame next. Moving an
issue to Ready does not start an agent. PR updates do not start review,
findings do not start a repair loop, and acceptance/CI do not merge a PR
without a user ship request.

The former scheduled orchestrator and Jira preview receiver/admission tooling
have been removed. No durable coordinator, event inbox, tunnel, launch ownership
system, or unattended retry service is needed for this workflow. CI and local
deterministic verification remain required.

Retired local state/log directories remain ignored to protect credentials and
historical evidence. Removing repository files does not disable external Jira
Automation rules, tunnels, services, or schedulers in other checkouts. Inspect
and retire any such installation separately before reusing it; do not restore
old unattended execution from history as part of an ordinary stage request.
