# Agentic workflow: what you might be missing

Notes from a 2026-09-09 conversation, ranked by how much it'd actually help
this project versus just being "a feature that exists." Not committed —
untracked scratch file, delete or commit it yourself if you want to keep it.

## Worth adopting

1. **Hooks — turn written policy into enforcement.**
   Right now every safety rule (never bypass a failing check, never merge
   without acceptance, no code edits during Review) is prose an agent has to
   remember to follow. `.claude/settings.json` hooks can enforce some of it
   mechanically — e.g. a `PreToolUse` hook that blocks `git push --force` or
   a direct commit to `main`, or one that runs `./verify.sh` before certain
   actions complete. Given how much of `AGENTS.md` is safety-rail prose for
   a financial app, this is the highest-value gap — it's the difference
   between a rule and a guardrail.

2. **`/security-review`.**
   You have a financial app handling real personal data, and this skill
   (already available, unused so far) audits pending changes for
   OWASP-style issues. Distinct from Codex's product-focused Review — worth
   running before `/ship` on anything touching auth, data access, or input
   handling.

3. **A read-only Postgres MCP.**
   `AGENTS.md` leans hard on deterministic, auditable state. Letting Claude
   query the dev/test database directly during implementation (instead of
   only through the API) would speed up verification — genuinely useful the
   moment debugging data state becomes a recurring friction point, which it
   will once Phase 4+ features have real data flowing through them.

4. **Plan Mode, for the actual feature work.**
   Today leaned on `AskUserQuestion` because it was all workflow decisions.
   For real implementation, `EnterPlanMode` is the better fit — Claude
   proposes an approach, you approve it, *then* code gets written. More
   rigorous than discussing in chat and forgetting to check back before the
   first edit.

## Know it exists, use if it comes up

5. **Fork subagents** for research-heavy sub-questions ("how does the
   scenario engine represent X") — keeps exploration noise out of your main
   session, which matters given how closely context budget got watched
   today (the implementation-log.md / decisions.md audit).

6. **Custom project subagents** (`.claude/agents/*.md`) — could define a
   narrow, read-only "financial-domain-reviewer" scoped specifically to
   `AGENTS.md`'s determinism/audit principles, as a pre-Codex sanity pass on
   new calculation code.

7. **Permission allowlisting** (`fewer-permission-prompts` skill) —
   mechanical quality-of-life fix for repeated approval prompts on routine
   `gh`/`git` reads.

## Skip, deliberately

8. **Background/scheduled agents, cron loops, multi-agent fleets.**
   These exist (`ScheduleWakeup`, `CronCreate`, teammates) and are exactly
   the shape of thing D016 tore down. Don't reach for them here — the whole
   arc of this project's workflow redesign has been walking away from
   standing automation; re-introducing it through a different door defeats
   the point.

## If you only do one thing

Set up the hooks. Everything else is additive; hooks are the one thing that
changes a rule from "the agent should follow this" to "the tool won't let it
not."
