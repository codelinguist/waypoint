# Decision index

This is the canonical status and trigger index. Read a decision body in
`docs/decisions/decisions.md` only when its trigger matches current work or a
concrete finding. Locate the body with `rg -n '^## DNNN'` and read only through
the next decision heading. Frame reads every Accepted body. Superseded bodies
are historical and loaded only when directly relevant.

| ID | Status | Constraint | Read body when work touches |
| --- | --- | --- | --- |
| D001 | Accepted | Structured financial state is canonical | persistence, imports, AI memory, financial state |
| D002 | Accepted | LLM is not the calculation engine | calculations, projections, scenarios, AI arithmetic |
| D003 | Accepted | Facts and assumptions are distinct | financial values, provenance, certainty, planning inputs |
| D004 | Accepted | Recommendations and decisions are distinct | AI proposals, approvals, recommendations, household policy |
| D005 | Accepted | Scenario state is non-destructive | scenarios, previews, simulations, canonical writes |
| D006 | Accepted | Product is private and household-specific | tenancy, generalization, internationalization, product scope |
| D007 | Accepted | Modular monolith first | services, deployment boundaries, queues, distributed infrastructure |
| D008 | Superseded by D011/D012 | Python was the initial backend | historical Python-backend rationale only |
| D009 | Accepted | PostgreSQL is canonical persistence | databases, persistence, migrations, storage |
| D010 | Accepted | Preserve historical plans and snapshots | plans, snapshots, overwrites, historical comparison |
| D011 | Accepted | Java owns core application and REST API | backend ownership, APIs, transactions, core calculations |
| D012 | Accepted | Python is for specialized analytics | Python, forecasting, optimization, ML, NLP, service boundaries |
| D013 | Accepted | Docker Compose is standard locally | local startup, containers, runtime topology |
| D014 | Accepted | `./verify.sh` and CI gate merges | verification, CI, checks, PR acceptance, merge readiness |
| D015 | Accepted | Early financial-position frontend slice | financial-position API/UI, net worth, monetary transport |
| D016 | Accepted | Development stages are user-initiated | automation, transitions, dispatch, review loops, merging |
| D017 | Accepted | Jira is per-feature specification | briefs, issue lifecycle, review records, task artifacts |
| D018 | Superseded by D020 | Codex stages run directly with user | historical Codex-only rationale only |
| D019 | Accepted | Agent context uses stage activation | agent instructions, context loading, workflow docs, decision discovery |
| D020 | Accepted | Claude Code is a standing Product Owner alternate | Codex/Claude ownership, commands, stage invocation, self-review |
| D021 | Accepted | Conditional bulk-update revisions guard corrections | optimistic concurrency, revision/version fields, before/after audit trails |
| D022 | Accepted | Bounded balance replacement is an append-only audit, not event sourcing | balance/value updates, revision concurrency, audit history on canonical records |
