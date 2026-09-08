---
description: Invoke Codex (the Product Owner Agent) non-interactively for the current workflow step
argument-hint: frame | design | review | accept | resume "<answer>"
---

# Invoke Codex: $ARGUMENTS

Invoke the Product Owner for the one user-requested stage described in
agent/collaboration-workflow.md. Resolve the supplied task/Jira key or PR and
its checked-in brief. Complete this stage and report back; never start the
next stage automatically.

If `$ARGUMENTS` doesn't name a mode below, ask the user which one they mean
instead of guessing.

## Before every invocation

1. Read the requested task file or Jira-linked brief (or prepare to create
   a brief for `frame`).
2. Build the prompt from checked-in files only — the relevant brief,
   `AGENTS.md`, `agent/collaboration-workflow.md`, the template being filled
   in. **Never paste this conversation into a `codex` prompt.** If Codex
   needs context beyond what a file already says, add it to the file first,
   then point Codex at the file.
3. Use normal workspace-write sandboxing. Never bypass approvals or sandboxing.
4. Create/edit artifacts only on a task branch, not main. For Jira work, use
   the linked brief instead of creating a duplicate numbered task record.

## Mode: `frame`

Problem framing, brief-writing, or queuing a new task.

```
codex exec -s workspace-write "Read agent/product/<slug>/product-brief.md \
(create it from agent/templates/product-brief.md if it doesn't exist yet), \
AGENTS.md, and agent/collaboration-workflow.md. <the user's problem \
statement, or the specific framing instruction for this call>. Define the \
outcome, priority, scope, risks, out-of-scope boundaries, and testable \
acceptance criteria. Write or update the brief. For numbered tasks only, write a file at \
agent/tasks/<NNN>-<slug>.md following agent/tasks/README.md's format with \
status: QUEUED (or update the matching existing task file if one already \
covers this work), then commit the change to the current branch with a \
clear message. If anything material is missing or ambiguous, stop and ask \
instead of guessing."
```

## Mode: `design`

Approving a design direction after Claude Code has written
`agent/ui/<slug>/design-brief.md`.

```
codex exec -s workspace-write "Read agent/ui/<slug>/design-brief.md and its \
linked product brief. Select a direction, record the decision, amendments, \
and rationale, set status to APPROVED, and commit the change."
```

## Mode: `review`

Review the requested PR independently against its brief and verification
evidence. Inspect relevant code and tests as necessary. Record the reviewed
revision. Do not invoke fixes or merge. Use plain codex exec with an explicit
request to inspect the diff:

```
codex exec -s workspace-write "Read agent/product/<slug>/product-brief.md, \
agent/tasks/<NNN>-<slug>.md, and agent/ui/<slug>/design-brief.md if it \
exists, plus relevant code and tests. Get the diff yourself: run \`gh pr diff <number>\`. \
Classify findings BLOCKING, RECOMMENDED, or OPTIONAL per \
agent/collaboration-workflow.md and append them to the product brief (and \
visual-review.md for UI work). Commit the change to the task branch."
```

## Mode: `accept`

Feature acceptance, after fixes are applied and verified.

```
codex exec -s workspace-write "Read agent/product/<slug>/product-brief.md \
and the current state of the PR. Compare the verified result against the \
defined outcome and acceptance criteria. Record the reviewed revision and ACCEPTED with evidence, or \
RETURNED with the specific unmet criteria, and commit the change."
```

## Mode: `resume "<answer>"`

Continuing Codex's own back-and-forth after it asked a clarifying question in
a prior `frame`, `design`, `review`, or `accept` call.

```
codex exec resume <session-id-from-the-prior-call> "<answer>"
```

Get `<session-id>` from the `session id:` line the prior call printed. If it
wasn't captured, fall back to `codex exec resume --last` — but only when no
other `codex` call has run since, or it may resume the wrong session.

## After every invocation

1. Read back whatever file Codex wrote or updated.
2. Report its findings, decision, or questions to the user directly in this
   conversation — no tool switch required.
3. If Codex ended by asking a clarifying question rather than finishing,
   relay the question to the user, then call this command again in `resume`
   mode with their answer.
