# Review prompt template

`agent/automation/orchestrator.sh` fills in `{{PR_NUMBER}}`, `{{BRANCH}}`,
`{{TASK_FILE}}`, and `{{FEATURE_SLUG}}` and passes the result to an unattended
`codex exec` invocation.

Known CLI limitation (confirmed empirically on codex-cli 0.152.1): `codex
review` / `codex exec review` reject combining `--base`/`--commit` with a
custom prompt (`error: the argument '--base <BRANCH>' cannot be used with
'[PROMPT]'`). This template is therefore run through plain `codex exec`, and
Codex is told to diff the branch itself with `gh`/`git` rather than relying on
the `review` subcommand's built-in diff scoping. If a future codex-cli version
fixes this, prefer `codex exec review --base main` again and drop the
diff-yourself step below.

The final line of Codex's response MUST be exactly one of:

```
REVIEW_VERDICT: ACCEPTED
REVIEW_VERDICT: BLOCKING
```

The orchestrator greps for that literal line in `--output-last-message`; it
does not otherwise parse Codex's reasoning. Everything else Codex writes goes
into the durable record (the product brief / implementation log), not into
orchestrator control flow.

---

Read AGENTS.md, agent/collaboration-workflow.md, {{TASK_FILE}},
and agent/product/{{FEATURE_SLUG}}/product-brief.md — nothing else. Do not
read any other conversation history; you have none for this feature beyond
what these files say.

Get the actual diff yourself: run `gh pr diff {{PR_NUMBER}}` (branch
`{{BRANCH}}` against `main`). Review it against the product brief's
acceptance criteria and `agent/collaboration-workflow.md`'s review criteria.
Classify every finding `BLOCKING` (prevents correct, accessible, or usable
completion), `RECOMMENDED` (meaningful improvement within approved intent), or
`OPTIONAL` (polish with a weak cost-benefit case). A preference alone is not a
defect — every finding needs visible evidence and a concrete acceptance
condition.

Append your findings to `agent/product/{{FEATURE_SLUG}}/product-brief.md`
under a new dated "Review findings" entry (and to
`agent/ui/{{FEATURE_SLUG}}/visual-review.md` too, if that file exists for
this feature). Mark each finding `ACCEPTED`, `REJECTED`, or `DEFERRED` per the
same criteria.

If there are no unresolved `BLOCKING` findings, compare the verified result
against the product brief's acceptance criteria as a whole. If every
criterion is satisfied, record feature acceptance in the product brief
(status `ACCEPTED`, with evidence) — this authorizes the orchestrator to
merge automatically, per `agent/collaboration-workflow.md` -> "Automated
pipeline"; there is no separate human merge step in this pipeline. If a
criterion is unmet, record precisely which one and why.

Commit your changes (the product brief, and visual-review.md if touched) to
branch `{{BRANCH}}` and push. Do not edit application code — that stays
Claude Code's job, in a fix round if you found `BLOCKING` issues.

End your final message with exactly one verdict line and nothing after it:
`REVIEW_VERDICT: ACCEPTED` if there is no unresolved `BLOCKING` finding and
the brief now records feature acceptance; `REVIEW_VERDICT: BLOCKING`
otherwise.
