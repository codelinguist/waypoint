# AGENTS.md

Waypoint is a private household financial operating system for Ralph and his
wife. The repository and application data—not model memory—are authoritative.

## Invariants for every task

- Keep facts, assumptions, goals, recommendations, and household decisions
  distinct. Never present an inference as a confirmed financial fact.
- Important financial arithmetic is deterministic, callable without an LLM,
  and tested. The LLM may interpret, explain, run tools, and draft proposals;
  it is not the calculation engine or financial datastore.
- AI-proposed material financial changes require explicit household approval
  before persistence. Do not execute investment, banking, or payment
  transactions.
- Keep domain logic separate from transport and UI. Prefer explicit types,
  timestamps, provenance, small vertical increments, and a modular monolith.
  Do not add infrastructure without a concrete need.
- The user initiates each meaningful workflow stage. Do not infer a new stage
  from Jira, CI, or repository state; do not launch automatic review/fix loops
  or merge without an explicit Ship request.
- Preserve unrelated and historical work. Do not silently expand task scope.

## Activate only the current stage

Classify the request before loading process documentation:

| Request | Stage file to read |
| --- | --- |
| explain, inspect, diagnose, or answer an architecture question | `agent/workflow/investigate.md` |
| define a product problem or create acceptance criteria | `agent/workflow/frame.md` |
| explore or approve a material UI direction | `agent/workflow/design.md` |
| change code or repository documentation | `agent/workflow/implement.md` |
| independently assess a PR | `agent/workflow/review.md` |
| address recorded review findings | `agent/workflow/revise.md` |
| merge/deliver accepted work | `agent/workflow/ship.md` |

Read `agent/collaboration-workflow.md` only when the stage is ambiguous or a
cross-stage boundary matters. Read exactly one stage file initially. Load
another only when the user explicitly changes stage or a concrete boundary
question requires it. For Jira work, read the issue description in full.

## Context discovery

Start with named files and direct code/test dependencies. Prefer targeted
`rg`, bounded line reads, and indexes before bodies. Do not print whole large
documents or repeat already-read material when a narrower read answers the
question.

Scan `docs/decisions/index.md` when architecture, domain rules, persistence,
AI authority, verification, or workflow policy may be affected. Read only
matching Accepted decision bodies from `docs/decisions/decisions.md`; read a
Superseded body only for relevant history. Open product, domain, and
architecture documents only when the stage file, issue, decision trigger, or
a concrete finding requires them.

Current source, contracts, decisions, and the Jira issue outrank historical
artifacts. `agent/tasks/`, legacy `agent/product/` briefs and implementation
logs, and `agent/archive/` are historical; exclude them from broad searches and
do not read them unless the task explicitly concerns history or a current
artifact points to one. Never create an implementation log. Put durable
feature contracts in `agent/product/<slug>/api.md` (or equivalent).

Record a new long-lived product or architecture decision in the decision
index and body store. Correct a discovered workflow/template rule in its
canonical file. Put unresolved material household questions on the Jira issue.
