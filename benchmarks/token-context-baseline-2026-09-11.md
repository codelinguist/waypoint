# Waypoint Codex token/context baseline

Date: 2026-09-11 (Asia/Manila)  
Repository revision: `60831642df0f38e6a57c1008e4edfab6fb5c77b1` on `main`; working tree clean before measurement, with only this benchmark directory added afterward  
Frontend benchmark revision: `b37384d1fc3d0221853beb0b151b0bb088a943d9` on `origin/task/wap-15-financial-position-dashboard`  
Runner: `codex-cli 0.153.4`, `gpt-5.6-sol`, reasoning effort `low`  
Execution: eight fresh `codex exec --ephemeral --json --color never -s read-only` sessions

## 1. Executive summary

The repository's *repository-authored* context policy is directionally healthy: there is one root `AGENTS.md`, it explicitly defines stage-sensitive context loading, and most large product/feature documents are meant to be loaded only when relevant. The measured executions nevertheless show substantial total input amplification. Eight successful read-only tasks used a median 132,601 total tokens and a mean 144,829, even though the final answers were short and task-relevant source material was usually only a few thousand tokens.

The clearest repeatable repository-level overhead is the combination of root `AGENTS.md` (about 1,962 estimated source tokens) plus the mandatory full read of `agent/collaboration-workflow.md` (about 2,421). Every benchmark run read the entire workflow, including detailed Frame, Implement, Review, Revise, Ship, Jira, worktree, and PR guidance, even for narrow read-only investigations. This is about 4,383 source tokens before domain/code context, and the workflow is replayed in later model requests through tool output.

The much larger measured input totals are not attributable to repository documents alone. They also include the Codex harness instructions, tool schemas, skill catalog, conversation/event history, and repeated tool outputs. The CLI reports cumulative input and cache use for the turn but does not break usage down by source. Therefore this report does not claim that reducing repository documents would save the full measured totals.

Verdict: **Needs Improvement**, primarily because stage-wide workflow detail is compulsorily loaded for every task and then amplified through multi-step tool use. The existing progressive-disclosure policy prevents much worse loading of historical feature files, so this is not rated Poor.

## 2. Current context architecture

### Automatic or persistent

- Codex platform/developer instructions, tool definitions, enabled plugin/tool metadata, and the available-skill catalog are supplied by the host. Their exact token contribution is not exposed by the CLI event stream.
- Root `AGENTS.md` is discovered for work under the repository. No nested project `AGENTS.md` exists in the active checkout. Copies under `.claude/worktrees/*` belong to separate worktree roots and are not additive when operating in the main checkout.
- User exec-policy rules exist at `/Users/rj/.codex/rules/default.rules` (3,122 bytes, about 781 chars/4 tokens). These affect command authorization; the benchmark could not verify whether their text is placed in model context.
- The installed skills catalog exposed 37 `SKILL.md` files. The host supplies skill names/descriptions, but skill bodies are loaded only when triggered. This repository defines no local Codex skill.

### Instruction-driven

- `AGENTS.md` says to always read the applicable stage in `agent/collaboration-workflow.md`. In practice every benchmark agent read the whole file with `sed -n '1,240p'` or `1,260p`.
- Frame requires the full six-document product set, domain model, architecture, and every Accepted decision.
- Implement/Revise, Review/Accept, and Ship start from narrower task artifacts and use the decision index to activate deeper context.
- `CLAUDE.md` and `.claude/commands/*` are Claude Code instructions/commands, not automatically loaded by Codex in these runs. They are relevant when benchmarking the Claude-owned workflow separately.
- Templates, product briefs, API contracts, historical task files, implementation logs, UI evidence, and research documents are available but are not automatically loaded merely because they exist.

## 3. Context inventory

Token estimates use `ceil(bytes / 4)`. This is a reproducible size proxy, not tokenizer telemetry. Actual model tokenization will differ.

| Context source | Est. tokens | Loading scope | Likely frequency | Necessary? | Progressive-disclosure issue |
|---|---:|---|---|---|---|
| `AGENTS.md` | 1,962 | ALWAYS LOAD in this repo | Every task | Yes | Long mission, implementation, AI, delivery, and collaboration policy share one global file |
| `agent/collaboration-workflow.md` | 2,421 | ALWAYS READ by instruction | Every task | Partly | Full six-stage operational detail reaches narrow investigations |
| `docs/decisions/decisions.md` | 3,318 | Index first; full Accepted set in Frame | Medium | Yes | Single file makes index-only reading easy in theory, but full reads occurred in some runs |
| `docs/architecture/architecture.md` | 1,617 | Domain/architecture relevant | Low-medium | Yes | Healthy when activated; should not be global |
| `docs/domain/financial-model.md` | 977 | Financial-domain relevant | Medium | Yes | Healthy when activated |
| Six `docs/product/*.md` files | 4,017 total | Full set in Frame; otherwise relevant only | Low outside Frame | Yes | Frame activation is about 4k before domain/architecture/decisions |
| `agent/roles/product-owner.md` | 1,377 | Codex product-owner stages | Low-medium | Yes | Repeats some stage/context/autonomy rules from AGENTS/workflow |
| Four `agent/templates/*.md` files | 1,208 total | Explicit stage need | Low | Yes | Healthy scoped references |
| `CLAUDE.md` | 685 | Claude Code sessions | High for Claude, none observed for Codex | Yes | Duplicates/pointers to shared workflow rules |
| Three `.claude/commands/*.md` files | 2,238 total | Explicit Claude command | Stage-dependent | Yes | Implement/Ship details overlap the central workflow |
| `.claude/settings*.json` | 42 total | Claude runtime | Every Claude session | Yes | Negligible |
| Historical `agent/product/**/*.md` and `agent/tasks/*.md` | about 128,413 total | Explicit relevance only | Low | Mixed | Large but not an always-loaded problem; implementation logs are historical context |
| Historical `implementation-log.md` files | about 27,153 | Explicit need only | Very low | Usually no | Retained history should not enter normal task context |
| `docs/research/founder-financial-planning-session.md` | about 756 | Explicit need | Very low | Sometimes | Healthy if kept out of routine work |
| Root `README.md` | about 4,104 | Explicit repository/setup need | Low-medium | Yes | Not automatic |
| User exec-policy rules | about 781 | Runtime policy; model-loading unknown | Every command session | Yes | Token loading cannot be verified |
| Installed skill bodies (37 files) | Not inventoried as repo context | LOAD ONLY WHEN TRIGGERED | Task-dependent | Mixed | Bodies are not evidence of static overhead |

Classification of agent-facing document families:

- **ALWAYS LOAD:** root `AGENTS.md`.
- **LOAD BY DIRECTORY/SCOPE:** each worktree's own root `AGENTS.md`; `CLAUDE.md`; `.claude/settings*.json`; explicit `.claude/commands/*`.
- **LOAD WHEN DOMAIN IS RELEVANT:** product docs, financial model, architecture, Accepted decisions selected through the index, active feature API contract/design brief.
- **LOAD ONLY ON EXPLICIT NEED:** templates, product-owner role, research, historical task/product artifacts, UI evidence, skill bodies.
- **SHOULD NOT BE AGENT CONTEXT for routine work:** historical implementation logs and obsolete task narratives, except forensic/history investigations.

### Estimated activation costs

- Automatic repository source: root `AGENTS.md`, about **1,962 tokens**.
- Effective pre-task repository overhead under current instructions: `AGENTS.md` plus full workflow, about **4,383 tokens**.
- Frame activation: six product docs + domain + architecture + full decisions, about **9,929 additional tokens**, plus product-owner role/template if used (about 1,657).
- Narrow backend investigation: workflow plus affected Java/tests/contracts; observed source selection was roughly **3k-12k**, depending on broad searches and full-file reads.
- Frontend activation: no frontend on `main`; the benchmark inspected the WAP-15 remote branch and loaded component, CSS, hook, test, fixture, and evidence files. Estimated task source was **8k-15k**.
- Review/architecture activation: decision index/full file + architecture + affected code/tests, roughly **7k-15k**.

These are source-material estimates, not billed input-token counts.

## 4. Progressive disclosure findings

### High

1. **The entire workflow is effectively mandatory for every task.** All eight runs read all 1,396 words, including unrelated stage mechanics. This is the strongest measured repository-specific offender.
2. **Workflow text is amplified as tool output.** The mandatory read is returned in a command result, then included in subsequent model requests. A 2.4k-token source can therefore contribute repeatedly to cumulative input.
3. **Global `AGENTS.md` mixes invariant product rules with stage/process detail.** The file is a reasonable size in isolation, but its 1,962 estimated tokens are injected into backend, frontend, docs, architecture, and narrow test investigations alike.

### Medium

1. **Collaboration and delivery policy is duplicated across `AGENTS.md`, `agent/collaboration-workflow.md`, `CLAUDE.md`, `.claude/commands/*`, and `agent/roles/product-owner.md`.** The expressions are mostly consistent, but create drift and repeated-reading risk.
2. **The decisions index and decisions share one 3.3k-token file.** Agents sometimes read the whole file rather than only index entries and matched decisions. The policy is progressive; the physical layout makes over-reading easy.
3. **Frame deliberately loads about 9.9k tokens of product/domain/architecture/decisions.** This may be justified for product framing, but it should be tested rather than assumed necessary for every Frame task.
4. **Broad repository searches pull historical artifacts into results.** One run's initial search returned matches from product briefs and implementation logs before the agent narrowed to code/tests.

### Low

1. Historical feature documents occupy about 128k estimated tokens but are not auto-loaded. Size alone is not an efficiency defect.
2. Worktree copies of `AGENTS.md` look duplicative in a filesystem inventory but are independent roots, not simultaneous context.
3. Skill bodies are numerous, but no benchmark run loaded a skill body. Only catalog metadata is plausibly static, and its token size was not exposed.

No Critical finding is supported: tasks completed correctly, the repository does have an explicit progressive-disclosure policy, and most large artifacts remained dormant.

## 5. Benchmark suite

Run every prompt in a fresh session, preserving punctuation and output limit. Prefix each with: `Read-only benchmark. Do not modify files.`

1. **Small bug/test-gap:** “Inspect the implementation and tests for FinancialDataFreshnessCalculator. Identify one plausible edge case not explicitly covered by tests, cite exact files and lines, and state whether it is a confirmed bug or only a test-gap hypothesis. Keep the final answer under 250 words.”
2. **Backend feature analysis:** “Determine the smallest code and test changes needed to add an optional asOfDate query parameter to the debt amortization endpoint while preserving current behavior when omitted. Cite exact files and lines, identify relevant contracts or decisions consulted, and give a concise implementation plan under 350 words.”
3. **Frontend feature analysis:** “Inspect the current financial-position UI and identify the exact components, state branches, and tests that would need updates to add a non-interactive 'Last refreshed' label beside the existing refresh control. Do not design a new feature. Cite exact files and lines; answer under 350 words.”
4. **Test repair:** “Find one currently disabled, flaky-looking, or insufficiently deterministic automated test in this repository. If none exists, say so and instead identify one specific deterministic test gap with the narrowest proposed test. Cite exact files and lines; answer under 300 words.”
5. **Refactoring:** “Inspect the backend scenario calculators and identify one concrete duplicated validation or result-building pattern that could be safely extracted without changing financial behavior. Name the exact files, describe the narrow refactor boundary, and list regression tests needed. If no worthwhile duplication exists, say so. Answer under 350 words.”
6. **Architecture:** “Based on the current repository architecture and accepted decisions, determine whether adding a message queue for financial snapshot creation is justified now. Cite only the directly relevant repository documents and code evidence, distinguish fact from inference, and answer under 350 words.”
7. **Documentation:** “Inspect README.md and verify.sh and identify up to three concrete places where setup or verification instructions are stale, incomplete, or inconsistent with the current repository. Cite exact files and lines, separate confirmed mismatches from suggestions, and answer under 350 words.”
8. **Cross-cutting investigation:** “Trace how currency is validated and propagated from persisted household assets and liabilities through the current financial-position API. Identify every layer that would need consideration to support lowercase currency input while continuing to emit uppercase ISO codes. Cite exact files and lines, distinguish required changes from already-correct behavior, and answer under 400 words.”

The suite uses analysis-only forms of implementation categories to preserve the repository. It deliberately includes backend, frontend-on-feature-branch, tests, refactoring, review-style reasoning, architecture, docs, and a multi-layer trace.

## 6. Baseline measurements

`Tokens` is measured CLI `input_tokens + output_tokens`. The CLI's `output_tokens` is reported alongside a reasoning-token subfield, so reasoning tokens are not added again. Turns are one user turn per fresh session. Tool calls count completed shell-command items. Context sources list material explicitly read, not host-supplied static instructions. Tests were not run because sessions were repository-read-only.

| Task | Input | Cached input | Output | Total | Turns | Tool calls | Context sources used | Result | Notes |
|---|---:|---:|---:|---:|---:|---:|---|---|---|
| Freshness bug/test-gap | 92,489 | 47,488 | 1,076 | 93,565 | 1 | 3 | Workflow; calculator/service/records; unit/API tests; feature brief grep | Success | Correctly separated direct-call bug from HTTP risk |
| Debt amortization asOfDate | 126,830 | 96,640 | 1,669 | 128,499 | 1 | 3 | Workflow; endpoint/DTO/calculator/tests; API/brief; decisions; domain | Success | Flagged larger semantic interpretation as unspecified |
| Financial-position label | 250,880 | 214,400 | 1,728 | 252,608 | 1 | 7 | Workflow; WAP-15 branch UI, CSS, hook, types, unit/E2E/evidence | Success | `main` lacked frontend; branch inspection was necessary |
| Deterministic test repair | 72,774 | 43,648 | 813 | 73,587 | 1 | 2 | Workflow; test-suite search; snapshot integration test | Success | One search command exited 2 from absent paths, but answer was supported |
| Scenario refactor | 153,381 | 128,512 | 1,233 | 154,614 | 1 | 4 | Workflow; scenario calculators/tests/result | Success | Kept refactor within one calculator |
| Snapshot queue architecture | 135,262 | 113,152 | 1,441 | 136,703 | 1 | 4 | Workflow; architecture; decisions; snapshot service/controller/tests/brief/config | Success | Fact/inference distinction present |
| README verification docs | 98,971 | 61,952 | 1,392 | 100,363 | 1 | 3 | README; verify script; CI/config/pom/compose/test inventory | Success | Three confirmed mismatches |
| Currency cross-cutting trace | 216,297 | 181,504 | 2,396 | 218,693 | 1 | 5 | Workflow; DTO/controllers/services/entities/schema/repositories/calculator/API/tests | Success | Covered validation, persistence, grouping, serialization, tests |

Files inspected cannot be measured perfectly because several commands concatenate many files and some output is truncated. A defensible lower bound from explicit command targets is approximately 5, 8, 8, 2, 4, 7, 6, and 17 files respectively; broad `rg` matches make actual exposure higher.

Sub-agents: **0**. Skills loaded inside benchmark sessions: **0 observed**. Manual correction: **none required for the eight valid runs**. An earlier orchestration attempt for task 2 was excluded because shell backticks stripped the requested parameter name before Codex received the prompt; it is not part of the baseline.

## 7. Aggregate metrics

- Successful tasks: **8/8 (100%)**.
- Median total tokens per successful task: **132,601**.
- Mean: **144,829**.
- Minimum: **73,587**.
- Maximum: **252,608**.
- Sum: **1,158,632**.
- Median measured input: **131,046**.
- Cached input share: **887,296 / 1,146,884 = 77.4%**.
- Output total: **11,748**; mean **1,469**.
- Estimated automatic repository context: **about 1,962 source tokens** (`AGENTS.md`).
- Estimated effective always-read repository context: **about 4,383 source tokens** (`AGENTS.md` + workflow).
- Largest measured run: frontend branch investigation, **252,608 total tokens**, driven by seven tool calls and multi-file/branch inspection.
- Largest repository-authored static offender: full workflow, about **2,421 source tokens**, because it is required and observed on all tasks.

### Context utilization ratio

The CLI does not expose per-source token attribution or a snapshot of exactly what the host supplied. A rough source-based estimate is possible: task-relevant files/sections were generally about 3k-15k source tokens, while cumulative input was 72.8k-250.9k. That suggests a broad **5%-20% task-relevant-source / cumulative-input ratio**. This is not a precision metric: repeated cached prefixes, tool schemas, and repeated conversation history inflate the denominator, while some host instructions are relevant but cannot be allocated.

### Context amplification ratio

Using the same estimated 3k-15k task-source range, measured total tokens imply roughly **8x-30x amplification** for most runs, with the frontend and cross-cutting traces near the high end. The ratio mixes necessary iterative reasoning with avoidable repeated context and should only be compared under an identical harness.

### Telemetry unavailable or not defensibly measurable

- Initial/static host context tokens as a separate field.
- Per-document or per-message token attribution.
- Exact context-window utilization; model window and instantaneous occupancy were not emitted.
- Cache-write tokens were emitted and were zero; why they were zero is not established.
- Reasoning quality beyond human inspection of the answer.
- Exact files whose contents entered context after command-output truncation.

[Official OpenAI API documentation](https://developers.openai.com/api/reference/cli/resources/responses/methods/create) describes usage fields for input, cached input, output, reasoning, and total usage. This CLI emitted input, cached input, cache-write input, output, and reasoning-output fields, but not a per-source breakdown.

## 8. Optimization candidates (do not implement yet)

| Candidate | Expected savings | Effectiveness risk | Complexity | Priority |
|---|---|---|---|---|
| Replace mandatory full-workflow read with a short stage index and load one stage section on demand | 1.5k-2.3k source tokens initially; larger cumulative savings through less replay | Low-medium: stage selection must remain reliable | Medium | 1 |
| Split invariant global rules from stage/role/delivery procedures in `AGENTS.md` | 600-1.2k static source tokens | Medium: accidentally hiding financial safety rules would be harmful | Medium | 2 |
| Make the decision index independently readable from full decision bodies | 2k-3k on tasks needing only trigger selection | Low if statuses/pointers remain canonical | Low-medium | 3 |
| Deduplicate collaboration/branch/PR policy across AGENTS, workflow, role, and Claude commands using concise pointers | 0.5k-2k per activated stage | Medium: command files must stay self-sufficient enough | Medium | 4 |
| Add narrowly scoped backend/frontend/testing AGENTS files only if they replace, rather than add to, global detail | Potential 0.5k-2k per unrelated task | Medium-high: nested files can increase context if additive | Medium-high | 5 |
| Keep historical implementation logs outside normal search scope or add search guidance | Variable; reduces noisy matches/tool output | Low | Low | 6 |
| Shorten tool commands to targeted ranges instead of printing full workflow/docs | Often tens of thousands of cumulative measured input tokens | Low | Low, behavioral/prompt change | 1 alongside workflow change |

Savings are source-token estimates, not guaranteed billed-token reductions. The benchmark should verify equal or better success before accepting any change.

## 9. Before/after procedure

1. Record exact git commit, Codex CLI version, model, reasoning effort, operating system, date, and active user config. Pin all settings that can be pinned.
2. Start from a clean checkout. Do not include `.claude/worktrees` as part of the main repository inventory; treat a feature branch needed by task 3 as a named external revision and record its commit.
3. Run the eight prompts above verbatim with the command shape:
   `codex exec --ephemeral --json --color never -s read-only -C ABSOLUTE_REPO_PATH PROMPT`
4. Use fresh sessions and do not resume/fork. Run tasks in the same order and, to reduce cache-order effects, repeat the whole suite once in reverse order if budget permits.
5. Save JSONL outside normal source directories or under `benchmarks/runs/YYYY-MM-DD/{task}.jsonl`. Record unsuccessful harness/orchestration attempts separately and exclude them by a predeclared rule.
6. From each `turn.completed`, record input, cached input, cache-write input, output, and reasoning-output. Count `command_execution` and other tool items. Do not double-count reasoning tokens if they are already a subfield of output.
7. Parse explicit file operands and `rg` results for a conservative file count. Mark counts as lower bounds when output truncates or broad searches are used.
8. Judge success against the stable rubric: answers the requested question, cites exact evidence, respects fact/inference distinctions and repository principles, stays read-only, and requires no factual correction. Have the same human reviewer apply the rubric before comparing tokens.
9. Report median/mean/min/max only across successful tasks, plus success rate. A token reduction accompanied by a failed or materially weaker answer is a regression.
10. Compare both raw input and non-cached input (`input - cached`) because cache behavior can dominate cost/latency. Also compare tool calls, workflow/doc reads, output length, and context-utilization estimate.
11. Treat changes under 10% as inconclusive without at least three repetitions per prompt; model nondeterminism, cache state, and repository revision can exceed small differences.
12. Run one real implementation task and one real PR review as a secondary validation after the read-only suite. Keep these out of the primary eight-task baseline unless identical isolated worktrees and revisions can be recreated.

## Concise verdict

Current workflow token efficiency: **Needs Improvement**

Primary bottleneck: The full multi-stage collaboration workflow is required and observably re-read on every task, then replayed through later tool-driven model calls.

Best optimization opportunity: Replace the mandatory full workflow read with a compact stage index plus one on-demand stage section, while keeping invariant financial safety rules global.

Baseline confidence: **Medium**

Confidence is Medium because the CLI supplied reliable aggregate token telemetry for eight fresh runs, but did not expose initial static-context or per-document attribution, and one frontend task depended on a feature branch rather than `main`.
