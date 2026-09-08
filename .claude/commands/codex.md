---
description: Invoke Codex (the Product Owner Agent) non-interactively for the current workflow step
argument-hint: frame | design | review | accept | resume "<answer>"
---

# Invoke Codex: $ARGUMENTS

Invoke the Product Owner for the one user-requested stage described in
agent/collaboration-workflow.md. Resolve the supplied Jira key or PR (or, for
work predating this workflow, its legacy task file). Complete this stage and
report back; never start the next stage automatically.

If `$ARGUMENTS` doesn't name a mode below, ask the user which one they mean
instead of guessing.

## Before every invocation

1. Read the requested Jira issue (or legacy task file/brief for pre-existing
   work).
2. Build the prompt from checked-in files and the live Jira issue only —
   `AGENTS.md`, `agent/collaboration-workflow.md`, `docs/product/roadmap.md`,
   the field checklist being filled in for `frame`. **Never paste this
   conversation into a `codex` prompt.** If Codex needs context beyond what a
   file or the issue already says, add it there first, then point Codex at it.
3. Use normal workspace-write sandboxing. Never bypass approvals or sandboxing.
4. Create/edit any repository artifacts (e.g. a UI design brief) only on a
   task branch, not main. Jira issue creation and comments are not
   branch-scoped.

## Mode: `frame`

Problem framing: define the feature and hand it off as a Jira issue in To Do.

```
codex exec -s workspace-write "Read docs/product/roadmap.md, \
docs/decisions/decisions.md, AGENTS.md, and agent/collaboration-workflow.md. \
<the user's problem statement, or the specific framing instruction for this \
call>. Define the outcome, primary user, success measure, priority and \
rationale, in-scope and out-of-scope boundaries, testable acceptance \
criteria, and risks/safeguards using the checklist in \
agent/templates/product-brief.md. Create a Jira issue with this content and \
move it to the To Do column. If anything material is missing or ambiguous, \
stop and ask instead of guessing. Report the created issue key."
```

## Mode: `design`

Approving a design direction after Claude Code has written
`agent/ui/<slug>/design-brief.md`.

```
codex exec -s workspace-write "Read agent/ui/<slug>/design-brief.md and its \
linked Jira issue. Select a direction, record the decision, amendments, \
and rationale, set status to APPROVED, and commit the change."
```

## Mode: `review`

Review the requested PR independently against its Jira issue and verification
evidence. Inspect relevant code and tests as necessary. Record the reviewed
revision. Do not invoke fixes or merge. Use plain codex exec with an explicit
request to inspect the diff:

```
codex exec -s workspace-write "Read Jira issue <key>, and \
agent/ui/<slug>/design-brief.md if it exists, plus relevant code and tests. \
Get the diff yourself: run \`gh pr diff <number>\`. Classify findings \
BLOCKING, RECOMMENDED, or OPTIONAL per agent/collaboration-workflow.md and \
post them as a comment on the Jira issue (and commit visual-review.md for \
UI work)."
```

## Mode: `accept`

Feature acceptance, after fixes are applied and verified.

```
codex exec -s workspace-write "Read Jira issue <key> and the current state \
of the PR. Compare the verified result against the defined outcome and \
acceptance criteria. Post a comment on the Jira issue recording the \
reviewed revision and ACCEPTED with evidence, or RETURNED with the specific \
unmet criteria, and update the issue's status accordingly."
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
