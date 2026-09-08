# Command-driven collaboration workflow

The user selects work and initiates each meaningful stage with a short command
or request. The agent supplies context and structure, completes that stage, and
reports the result. Readiness, task status, Jira events, PR creation, and green CI
never start another stage. There is no scheduler, receiver, dispatcher, automatic
repair loop, or automatic merge.

## Context and ownership

Read AGENTS.md and its required documents. Resolve the requested task or Jira
issue and its linked repository brief. Retrieve available issue context when
asked to work on it; do not scan for work to start. If access is unavailable,
report the missing context without inventing requirements.

Repository briefs, code, and evidence are the handoff boundary. Jira identifies
and summarizes work; its column does not authorize execution. Existing files in
agent/tasks remain usable records. Do not create a second task for a Jira issue.

Codex owns product framing, design approval, and independent acceptance. Claude
Code normally owns technical research, design exploration, implementation, and
tests. An explicit task can override implementation ownership. Research agents
may investigate; code stays in the main implementation conversation.

Use separate conversations for planning, implementation, and independent review.
Load checked-in artifacts rather than another agent's reasoning transcript.

## Stages

1. **Frame:** investigate the user problem and write a brief using
   agent/templates/product-brief.md. Define scope, non-goals, and testable
   acceptance criteria. Record readiness, then hand back to the user.
2. **Design (material UI work):** explore distinct directions using
   agent/templates/ui-design-brief.md. Obtain Product Owner approval in a
   separately requested review before implementation. Small mechanical UI
   changes and backend tasks do not require this stage.
3. **Implement:** in a fresh session, load the approved brief, create a task
   branch/worktree as needed, and complete the smallest scoped increment.
   Use agent/templates/implementation-plan.md when complexity warrants it.
   Fix implementation and test failures within this stage. Run ./verify.sh,
   exercise the primary flow, and capture wide/narrow UI evidence when relevant.
   Update the implementation log and delivery handoff, push, and open the PR.
   Report the PR and evidence; do not invoke review automatically.
4. **Review:** in an independent session, inspect the current PR diff, relevant
   code, tests, brief, and evidence. Record BLOCKING, RECOMMENDED, or OPTIONAL
   findings with concrete evidence and acceptance conditions. Use
   agent/templates/ui-visual-review.md for UI evidence. Record ACCEPTED only
   when all acceptance criteria are supported, otherwise RETURNED with unmet
   criteria. Record the reviewed revision. Commit and push review artifacts;
   do not edit implementation code or launch fixes.
5. **Revise:** when requested, resolve accepted review findings on the same
   branch, verify affected behavior and ./verify.sh, update evidence, and push.
   Return for another user-requested independent review. Material scope/design
   changes need a revised approved brief.
6. **Ship:** only on an explicit user request, verify acceptance applies to the
   current implementation and the required verify check is green on the current
   PR head. Merge the intended PR and record completion. A later code change
   invalidates stale acceptance. Do not bypass missing, pending, or failed checks.

## Commands

Existing Claude Code commands are /prime (load context) and /codex frame,
/codex design, /codex review, /codex accept, or /codex resume (invoke the Product
Owner for one stage). Include the task, Jira key, or PR in the request. Plain
requests such as “implement WAP-5” or “ship PR 30” also select a single stage.
Additional short commands can be added as needed; these names do not imply
that /implement or /ship slash commands already exist.

For a requested independent Codex invocation, use codex exec with normal
workspace-write sandboxing and a prompt referencing checked-in artifacts.
Have the reviewer inspect the PR diff with gh pr diff or git diff; do not
rely on parsing a magic verdict line. Never bypass permissions for convenience.
Report the result or any required question to the user when the stage ends.

## Delivery rules

Every task ships on its own branch and PR from main. Use
task/<NNN>-<feature-slug> for numbered tasks, or
codex/<feature-slug> for explicit repository changes. Preserve existing dirty
work. Only one agent edits a feature; parallel work requires isolated worktrees
and explicit ownership. Before handoff, resolve conflicts and check migration
version collisions against main when applicable.

The implementation stage includes authorization to push and open/update a PR.
Review includes authorization to commit and push findings. Neither includes
merge authorization. The brief's evidence-based acceptance is the durable
independent-review record because the agents share a GitHub account.

UI completion requires the approved flow, accessible keyboard and narrow-width
behavior, deliberate loading/empty/error states, and representative screenshots.
Facts, assumptions, goals, recommendations, and decisions remain distinct.
Product acceptance cannot approve household data changes or financial decisions.

## System evolution

Update agent/implementation-log.md with changes, verification, decisions,
assumptions, unresolved questions, and the next useful task. When a finding
reveals a missing rule or template, record and make a small explicit correction.
Historical logs describe prior workflows; this document governs current work.
