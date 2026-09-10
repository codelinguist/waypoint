# Command-driven collaboration workflow

The user selects work and initiates each meaningful stage with a short command
or request. The agent supplies context and structure, completes that stage, and
reports the result. Readiness, task status, Jira events, PR creation, and green CI
never start another stage. There is no scheduler, receiver, dispatcher, automatic
repair loop, or automatic merge.

## Context and ownership

Read AGENTS.md and its required documents. Resolve the requested Jira issue
(or, for work predating this workflow, its legacy `agent/tasks` file).
Retrieve available issue context when asked to work on it; do not scan for
work to start. If access is unavailable, report the missing context without
inventing requirements.

The Jira issue, code, and evidence are the handoff boundary for feature work.
Its column does not authorize execution — the user still initiates each
stage. Durable cross-feature documentation (`docs/product/roadmap.md`,
`docs/decisions/decisions.md`, and the other docs required by AGENTS.md)
stays in the repository and is what Codex reads to decide what to frame next;
only the per-feature specification moved to Jira. Existing `agent/product/`
briefs and `agent/tasks/` files are retained as historical records; do not
create new ones.

Codex owns product framing, design approval, and independent acceptance. Claude
Code normally owns technical research, design exploration, implementation, and
tests. An explicit task can override implementation ownership. Research agents
may investigate; code stays in the main implementation conversation.

Use separate conversations for planning, implementation, and independent review.
Load checked-in artifacts rather than another agent's reasoning transcript.

## Stages

Frame, Design approval, Review, and Accept are Codex's stages and happen
directly between the user and Codex in Codex's own session — Claude Code is
not invoked for them and does not run `codex exec` on the user's behalf.
Claude Code picks up work from whatever the Jira issue and repository
evidence say once the user brings a stage to it.

1. **Frame (Codex, direct):** investigate the user problem against
   docs/product/roadmap.md, docs/decisions/decisions.md, and the other
   product docs required by AGENTS.md. Define the outcome, scope, non-goals,
   and testable acceptance criteria using the checklist in
   agent/templates/product-brief.md, create a Jira issue with that content,
   and move it to To Do.
2. **Design (material UI work):** Claude Code explores distinct directions
   from the Jira issue using agent/templates/ui-design-brief.md. Product
   Owner approval happens directly between the user and Codex. Small
   mechanical UI changes and backend tasks do not require this stage.
3. **Implement (Claude Code):** in a fresh session (`/implement <issue-key>`),
   transition the Jira issue from To Do to In Progress, create an isolated
   worktree with the EnterWorktree tool on branch
   `task/<issue-key>-<feature-slug>`, and load the issue (and approved design
   brief, if any). Complete the smallest scoped increment. Use
   agent/templates/implementation-plan.md when complexity warrants it.
   Fix implementation and test failures within this stage. Run ./verify.sh,
   exercise the primary flow, and capture wide/narrow UI evidence when relevant.
   Update the implementation log, push, and open the PR, then transition the
   issue from In Progress to Review — the PR is what needs attention now, not
   the coding. Report the PR and evidence; do not invoke review automatically.
4. **Review (Codex, direct):** inspect the current PR diff, relevant code,
   tests, the Jira issue, and evidence. This is read-only — no code edits, no
   re-running the app, since verification already happened in Implement — so
   it needs no worktree or local branch checkout: get the diff with
   `gh pr diff <number>` and read specific files at that revision with
   `git show origin/<branch>:<path>` (or `gh api`) directly from the main
   clone, without switching what branch it has checked out. That makes
   reviewing several issues in parallel safe by default — each review just
   targets a different PR/branch with no shared mutable state to collide
   over; nothing needs isolating the way Implement's worktree isolates
   concurrent code changes. Record BLOCKING, RECOMMENDED, or OPTIONAL
   findings with concrete evidence and acceptance conditions as a comment on
   the Jira issue. Use agent/templates/ui-visual-review.md for UI evidence.
   Record ACCEPTED only when all acceptance criteria are supported and move
   the issue to Acceptance — it's approved but not yet merged; Ship is still
   the only stage that moves it to Done. Otherwise record RETURNED with
   unmet criteria and move the issue back to In Progress, since it needs
   more implementation work before it's reviewable again. Either way, record
   the reviewed revision.
5. **Revise (Claude Code):** when requested, re-enter the issue's worktree
   with `EnterWorktree path: .claude/worktrees/<issue-key>` (lowercase)
   before doing anything else, then resolve accepted review findings (read
   from the Jira issue's comments) on the same branch — the issue should
   already be In Progress from Review's RETURNED verdict; move it there if
   it somehow isn't. Verify affected behavior and ./verify.sh,
   update evidence, push, and transition the issue back to Review. Return for
   another user-requested independent review. Material scope/design changes
   need the Jira issue updated and re-approved by Codex.
6. **Ship (Claude Code):** only on an explicit user request, verify acceptance
   applies to the current implementation and the required verify check is
   green on the current PR head. Merge the intended PR, move the Jira issue
   from Acceptance to Done, and record completion. A later code change
   invalidates stale acceptance. Do not bypass missing, pending, or failed
   checks. Afterward, remove the issue's worktree (if one exists, isn't
   locked, and has no uncommitted changes) and its now-merged local branch —
   Ship is the point nothing will reuse them, since Revise no longer applies
   once the issue is Done.

## Commands

Claude Code commands are /prime (load context), /implement <issue-key> (run
the Implement stage: transition the issue, create the worktree, and
implement it — see .claude/commands/implement.md), and
/ship <issue-key or PR> (run the Ship stage: verify acceptance and the
required check, merge, and close out the issue — see
.claude/commands/ship.md). A plain request such as “revise WAP-5” selects
the Revise stage instead; include the Jira key or PR in the request.
Additional short commands can be added as needed. Frame, Design approval,
Review, and Accept are requested by the user directly in Codex's own
session, not through a Claude Code command.
Report the result or any required question to the user when a stage ends.

## Delivery rules

Every task ships on its own branch and PR from main. Use
task/<issue-key>-<feature-slug> (lowercase issue key, e.g.
task/wap-123-emergency-fund-runway) for Jira-driven feature work,
codex/<feature-slug> for explicit repository changes not tied to an issue, or
the legacy task/<NNN>-<feature-slug> only for pre-existing numbered tasks.
The issue key in the branch name is load-bearing, not cosmetic: the GitHub
for Jira app scans branch names, commits, and PR titles for it and uses that
to link the branch/commits/PR on the issue's Development panel automatically
— don't drop it when naming a branch, and also put the issue key in the PR
title (per the Implement stage) so the PR itself links even if the app's
branch-matching doesn't catch it. Preserve existing dirty work. Only one
agent edits a feature; parallel work requires isolated worktrees and
explicit ownership. Before handoff, resolve conflicts and check migration
version collisions against main when applicable.

The implementation stage includes authorization to push and open/update a PR.
Review includes authorization to comment on the Jira issue and push any
repository review evidence. Neither includes merge authorization. The Jira
issue's comment history is the durable independent-review record because the
agents share a GitHub account.

UI completion requires the approved flow, accessible keyboard and narrow-width
behavior, deliberate loading/empty/error states, and representative screenshots.
Facts, assumptions, goals, recommendations, and decisions remain distinct.
Product acceptance cannot approve household data changes or financial decisions.

## System evolution

When a finding reveals a missing or wrong rule or template, make a small
explicit correction directly to the affected doc (this file, AGENTS.md, a
role file, or a template) rather than logging it for later. A new long-lived
architectural or product decision goes in `docs/decisions/decisions.md`.
Historical logs under `agent/archive/` describe prior workflows; this
document governs current work.
