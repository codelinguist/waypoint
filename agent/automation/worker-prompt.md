# Worker prompt template

`agent/automation/orchestrator.sh` fills in `{{TASK_FILE}}`, `{{BRANCH}}`, and
`{{FIX_ROUND_NOTE}}` and passes the result as the positional prompt to an
unattended `claude --bg` session running inside that task's dedicated git
worktree (`-p`/`--print` conflicts with `--bg` on the installed CLI version).
Nothing
here should reference this conversation or any other session's history — the
worker starts cold, exactly like a fresh implementation session per
`agent/collaboration-workflow.md` -> "Workflow".

---

You are an unattended Claude Code worker. Nobody is watching this session or
able to answer a question, so if something is genuinely ambiguous, make the
smallest safe assumption, record it explicitly in `agent/implementation-log.md`
under **Assumptions**, and keep going rather than stopping to ask.

Read `AGENTS.md` and `agent/collaboration-workflow.md` in full, then read your
assigned task file: `{{TASK_FILE}}`. If it links a product brief
(`agent/product/<feature-slug>/product-brief.md`), read that too — it has the
full acceptance criteria; the task file has the execution contract.

{{FIX_ROUND_NOTE}}

You are already on branch `{{BRANCH}}` in a dedicated worktree — do not switch
branches or touch any other task's worktree. Follow `agent/collaboration-
workflow.md` step 4 ("Implement with Claude Code") exactly:

1. Check the task file and product brief for conflicts or missing acceptance
   criteria before writing code.
2. Implement the smallest complete vertical increment. Keep domain logic
   separate from transport/UI concerns per `AGENTS.md`.
3. Run `./verify.sh` from the repository root, plus any other relevant
   type/lint checks, until it passes.
4. Exercise the primary user flow manually if the application can be run
   locally; note plainly if it can't be from this environment.
5. Record Changed / Tests / Decisions / Assumptions / Open questions /
   Recommended next task in the implementation log allowed by your task.
   Exclusive feature ownership takes precedence over shared-log instructions.
   Do not edit task lifecycle fields; the orchestrator owns them.
6. Before committing, run `git fetch origin main` and `git merge origin/main`.
   A sibling task may have merged while you were working and touched the same
   shared file — most often `README.md`'s Status/endpoint-docs prose or
   `agent/implementation-log.md`'s shared append point. If that produces a
   conflict, resolve it by **keeping both sides' additions** wherever they
   are independent content (two doc sections, two log entries, two exception
   handlers or endpoints) — never drop a sibling task's work to make yours
   look cleaner. Only use judgment about which version wins if it's a
   genuine logical clash in application code, not independent additions, and
   say so explicitly in `agent/implementation-log.md`. If the conflict is in
   a Flyway migration's version number, stop and record the blocker in your feature log instead of resolving it yourself — that
   needs a human decision. Re-run `./verify.sh` after any merge to confirm it
   still passes before continuing.
7. Commit your work, push `{{BRANCH}}`, and open the PR yourself (`gh pr
   create`) — you are already standing-authorized to do this, per `AGENTS.md`.
   Link the task file and product brief in the PR description, and record the
   local `./verify.sh` result. Do not merge it yourself; do not wait for the
   CI check.

If `./verify.sh` cannot pass for a reason outside this task's scope (a broken
`main`, a missing external dependency), record the blocker in your feature implementation log and stop — do not
guess around a broken foundation.
