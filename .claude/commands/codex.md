---
description: Invoke Codex (the Product Owner Agent) non-interactively for the current workflow step
argument-hint: frame | design | review | accept | resume "<answer>"
---

# Invoke Codex: $ARGUMENTS

Mechanics for `agent/collaboration-workflow.md` -> "Invoking the Product
Owner Agent". This command standardizes *how* Codex gets called; it does not
decide what Codex should do — that comes from `agent/current-task.md` and the
relevant brief at the time of the call.

If `$ARGUMENTS` doesn't name a mode below, ask the user which one they mean
instead of guessing.

## Before every invocation

1. Read `agent/current-task.md` to get the active feature slug.
2. Build the prompt from checked-in files only — the relevant brief,
   `AGENTS.md`, `agent/collaboration-workflow.md`, the template being filled
   in. **Never paste this conversation into a `codex` prompt.** If Codex
   needs context beyond what a file already says, add it to the file first,
   then point Codex at the file.
3. Never pass `--dangerously-bypass-approvals-and-sandbox`.

## Mode: `frame`

Problem framing, brief-writing, or updating `agent/current-task.md`.

```
codex exec -s workspace-write "Read agent/product/<slug>/product-brief.md \
(create it from agent/templates/product-brief.md if it doesn't exist yet), \
AGENTS.md, and agent/collaboration-workflow.md. <the user's problem \
statement, or the specific framing instruction for this call>. Define the \
outcome, priority, scope, risks, out-of-scope boundaries, and testable \
acceptance criteria. Write or update the brief and agent/current-task.md \
directly, then commit the change to the current branch with a clear message. \
If anything material is missing or ambiguous, stop and ask instead of \
guessing."
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

Reviewing an open PR (step 5, or a later fix round).

```
codex review --base main "Read agent/product/<slug>/product-brief.md, \
agent/current-task.md, and agent/ui/<slug>/design-brief.md if it exists — \
nothing else. Classify findings BLOCKING, RECOMMENDED, or OPTIONAL per \
agent/collaboration-workflow.md and append them to the product brief (and \
visual-review.md for UI work). Commit the change to the task branch."
```

## Mode: `accept`

Feature acceptance (step 7), after fixes are applied and verified.

```
codex exec -s workspace-write "Read agent/product/<slug>/product-brief.md \
and the current state of the PR. Compare the verified result against the \
defined outcome and acceptance criteria. Record ACCEPTED with evidence, or \
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
