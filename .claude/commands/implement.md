---
description: Pick up a Jira issue and implement it in an isolated worktree
argument-hint: <issue-key>
---

# Implement: $ARGUMENTS

Run the Implement stage described in `agent/collaboration-workflow.md` for
the given Jira issue. Frame, design approval, review, and accept already
happened directly between the user and Codex — this command only starts
implementation.

If `$ARGUMENTS` isn't a Jira issue key, ask which issue this session is for
instead of guessing.

## Steps

1. **Fetch the issue.** Use the Atlassian MCP tools to read the issue's
   summary and description in full — outcome, scope, non-goals, acceptance
   criteria, and any linked design brief
   (`agent/ui/<feature-slug>/design-brief.md`) mentioned there. Since this
   command only picks up a To Do issue (step 2 stops otherwise), there's
   normally no review history yet; if comments already exist, read the most
   recent round in full and skim earlier ones for their outcome only. If the
   Atlassian MCP tools aren't available or aren't authenticated, stop and
   tell the user rather than guessing at the spec.
2. **Check status.** If the issue isn't in To Do, report its actual status
   and ask before proceeding — don't silently re-pick up in-progress or done
   work.
3. **Transition it.** Move the issue from To Do to In Progress.
4. **Create the worktree.** Derive `<feature-slug>` from the issue summary
   (short, kebab-case). If this session is already inside a worktree, ask
   before switching. Otherwise use EnterWorktree with `name: <issue-key>`
   (lowercase) to create one at `.claude/worktrees/<issue-key>` on branch
   `task/<issue-key>-<feature-slug>` (lowercase the issue key), branched from
   `main` — the deterministic path is what lets Revise find it again later
   without a lookup.
5. **Load context.** Read `AGENTS.md` and use its Implement-stage progressive
   disclosure policy (or run `/prime`) before writing any code.
6. **Implement.** Complete the smallest complete vertical increment scoped by
   the issue's acceptance criteria. Use
   `agent/templates/implementation-plan.md` when the task is complex enough
   to leave real ambiguity about where things go.
7. **Verify.** Run `./verify.sh`, exercise the primary flow, and capture
   wide/narrow UI evidence when the issue includes UI work.
8. **Ship the increment.** Push the branch and open the PR — reference the
   issue key in the PR title and link it in the description.
9. **Move the issue to Review.** The PR is now what needs attention, not the
   coding — transition the issue from In Progress to Review.
10. **Report.** Tell the user the PR, the evidence gathered, and the issue's
    new status. Do not invoke review — Review and Accept happen directly
    between the user and Codex in Codex's own session.

## After implementing

Leave the worktree in place (don't call ExitWorktree) — Revise re-enters it
at `.claude/worktrees/<issue-key>` when the user brings back review findings.
