# Waypoint Codex token/context post-optimization benchmark

Date: 2026-09-11 (Asia/Manila)  
Optimized workflow revision: `c1bec565ca0e36cd2ac3dd74f883ae2166dc4611`  
Application baseline parent: `60831642df0f38e6a57c1008e4edfab6fb5c77b1`  
Frontend revision: `b37384d1fc3d0221853beb0b151b0bb088a943d9` in the existing `wap-15` worktree  
Runner: `codex-cli 0.153.4`, `gpt-5.6-sol`, reasoning effort `low`, priority service tier  
Execution: eight fresh `codex exec --ephemeral --json --color never -s read-only` sessions, in baseline order

## Executive summary

All eight optimized-workflow tasks passed the baseline quality rubric and all
eight correctly activated `agent/workflow/investigate.md`. No task loaded the
full workflow, another stage file, Ship/PR procedure, or a historical
implementation log. Progressive stage routing therefore worked as designed.

Runtime token efficiency did not improve at the suite level. Median total usage
was effectively unchanged (132,647 versus 132,601; +0.03%), aggregate
non-cached input was also effectively unchanged (259,484 versus 259,588;
-0.04%), and total usage increased 12.3%. The increase was dominated by the
test-repair task, which used six tool calls rather than two and rose 215.5% in
total tokens. Two tasks achieved meaningful reductions and two regressed
materially. Under the benchmark's interpretation thresholds, the overall result
is **Inconclusive**, not evidence of runtime success.

## Method and environment

The eight prompts from the baseline report were used verbatim, including the
`Read-only benchmark. Do not modify files.` prefix and original answer limits.
Sessions were fresh, ephemeral, sequential, read-only, and not resumed or
forked. No sub-agents or skills were used inside benchmark sessions. The CLI,
model, reasoning effort, task order, application revision, and frontend
revision match the baseline.

One initial task-3 attempt exhausted the account usage window before a final
answer. It is preserved as
`runs/2026-09-11-post-optimization/03-frontend-label-failed-usage-limit.jsonl`
and excluded under the baseline rule for unsuccessful harness attempts. Task 3
was rerun unchanged in a new ephemeral session after capacity became available.
Task 4 experienced a transient stream disconnect; the CLI retried within the
same session and completed. These are the only material runtime events.

`Tokens` below is `input_tokens + output_tokens`; reasoning output is reported
but not added again. Cache-write input was zero for every valid run.

## Direct comparison

| Task | Baseline Total | Optimized Total | Delta | Delta % | Baseline Tool Calls | Optimized Tool Calls | Success Before | Success After |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Freshness bug/test-gap | 93,565 | 78,565 | -15,000 | -16.03% | 3 | 3 | Yes | Yes |
| Debt amortization `asOfDate` | 128,499 | 99,473 | -29,026 | -22.59% | 3 | 3 | Yes | Yes |
| Financial-position label | 252,608 | 259,559 | +6,951 | +2.75% | 7 | 7 | Yes | Yes |
| Deterministic test repair | 73,587 | 232,164 | +158,577 | +215.50% | 2 | 6 | Yes | Yes |
| Scenario refactor | 154,614 | 145,858 | -8,756 | -5.66% | 4 | 4 | Yes | Yes |
| Snapshot queue architecture | 136,703 | 119,435 | -17,268 | -12.63% | 4 | 4 | Yes | Yes |
| README verification docs | 100,363 | 97,810 | -2,553 | -2.54% | 3 | 3 | Yes | Yes |
| Currency trace | 218,693 | 268,371 | +49,678 | +22.72% | 5 | 6 | Yes | Yes |

## Aggregate comparison

| Aggregate Metric | Baseline | Optimized | Change |
| --- | ---: | ---: | ---: |
| Median total tokens | 132,601 | 132,647 | +46 (+0.03%) |
| Mean total tokens | 144,829 | 162,654 | +17,825 (+12.31%) |
| Min | 73,587 | 78,565 | +4,978 |
| Max | 252,608 | 268,371 | +15,763 |
| Total tokens | 1,158,632 | 1,301,235 | +142,603 (+12.31%) |
| Success rate | 100% | 100% | unchanged |
| Median input | 131,046 | 131,367 | +321 (+0.24%) |
| Cached input share | 77.4% | 79.85% | +2.45 points |
| Total tool calls | 31 | 36 | +5 (+16.13%) |
| Median tool calls | 3.5 | 4.0 | +0.5 |
| Effective always-read repo context estimate | ~4,383 | ~1,082 for these Investigate runs | -75.3% static estimate |

The optimized median input is the midpoint of 118,251 and 144,483. The
effective repository-context figure combines the approximately 880-token root
`AGENTS.md` with the approximately 202-token Investigate stage file. It is a
source-size estimate, not CLI telemetry.

## Per-task usage and non-cached input

| Task | Input | Cached | Non-cached | Baseline non-cached | Non-cached delta | Output | Reasoning output |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Freshness bug/test-gap | 77,380 | 65,024 | 12,356 | 45,001 | -32,645 (-72.54%) | 1,185 | 362 |
| Debt amortization `asOfDate` | 97,616 | 64,384 | 33,232 | 30,190 | +3,042 (+10.08%) | 1,857 | 471 |
| Financial-position label | 257,561 | 196,224 | 61,337 | 36,480 | +24,857 (+68.14%) | 1,998 | 423 |
| Deterministic test repair | 230,373 | 198,528 | 31,845 | 29,126 | +2,719 (+9.34%) | 1,791 | 388 |
| Scenario refactor | 144,483 | 122,112 | 22,371 | 24,869 | -2,498 (-10.04%) | 1,375 | 151 |
| Snapshot queue architecture | 118,251 | 85,760 | 32,491 | 22,110 | +10,381 (+46.95%) | 1,184 | 124 |
| README verification docs | 96,360 | 82,560 | 13,800 | 37,019 | -23,219 (-62.72%) | 1,450 | 411 |
| Currency trace | 265,684 | 213,632 | 52,052 | 34,793 | +17,259 (+49.60%) | 2,687 | 474 |
| **Total** | **1,287,708** | **1,028,224** | **259,484** | **259,588** | **-104 (-0.04%)** | **13,527** | **2,804** |

Median non-cached input decreased from 32,492 to 32,168, approximately 1.0%.
Cache behavior materially affected total-input differences between individual
runs, but aggregate non-cached usage still shows no meaningful improvement.

## Stage routing and workflow activation

| Task | Expected stage | Actual stage | Correct? | Workflow files read | Unexpected activation |
| --- | --- | --- | --- | --- | --- |
| Freshness bug/test-gap | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Debt amortization `asOfDate` | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Financial-position label | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Deterministic test repair | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Scenario refactor | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Snapshot queue architecture | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| README verification docs | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |
| Currency trace | Investigate | Investigate | Yes | `agent/workflow/investigate.md` | None |

Stage-routing accuracy was 8/8. Full `agent/collaboration-workflow.md` reads
fell from 8/8 to 0/8. Stage-specific reads were 8/8, all correctly scoped.
No unrelated stage file was opened.

## Decision activation and context leakage

| Task | Index read? | Decision bodies inspected | Full body store read? | Historical artifacts | Leakage/overread |
| --- | --- | --- | --- | --- | --- |
| Freshness bug/test-gap | No | None | No | None | None |
| Debt amortization `asOfDate` | Yes | D002, D011 | No | None; current API contract only | None material |
| Financial-position label | No | None | No | None | Broad code/worktree discovery, but no unrelated stage or history body |
| Deterministic test repair | No | None | No | None | Broad test inventory and six tool calls |
| Scenario refactor | Yes | None | No | None | Index scan was defensible; no body loaded |
| Snapshot queue architecture | Yes | D006–D012 range | No | None | Overread adjacent D006, D008–D010, D012; D007/D011 were relevant |
| README verification docs | No | None | No | None | None |
| Currency trace | Yes | D001–D002, D009–D012, D015–D016 ranges | No | None | Overread adjacent D002, D010, D012, D016 |

No session read the entire decision body store. Three tasks needed bodies; one
read exact bodies and two used ranges broad enough to include adjacent,
unrelated decisions. This is a smaller leakage mode than the baseline's full
decision-file reads, but remains avoidable.

No valid run read a historical implementation log. No backend task activated
frontend instructions, and no investigation activated Implement, Review, or
Ship procedures. The frontend run used the exact baseline feature revision,
although it required several discovery calls before locating the existing
worktree.

## Context sources and conservative file counts

Counts are conservative unique explicit path operands inferred from command
text; broad `rg`, `find`, and directory operands mean actual exposure can be
higher.

| Task | Conservative explicit files | Principal context sources |
| --- | ---: | --- |
| Freshness bug/test-gap | 7 | Investigate stage; calculator, records, unit tests; targeted domain search |
| Debt amortization `asOfDate` | 12 | Investigate stage; controller/request/calculator/API tests; current API contract; D002/D011; domain slice |
| Financial-position label | 7 | Investigate stage; pinned worktree UI, hook, formatter, styles, component and browser tests |
| Deterministic test repair | 8 | Investigate stage; test inventory; snapshot integration test/entity/repository/service |
| Scenario refactor | 9 | Investigate stage; three calculators and direct tests; decision/domain searches |
| Snapshot queue architecture | 8 | Investigate stage; decision index/body range; architecture/domain; snapshot service/controller/test |
| README verification docs | 3 | Investigate stage; README; verify script; build/CI/test inventory |
| Currency trace | 30 | Investigate stage; decision/domain ranges; DTO/controller/service/entity/schema/repository/calculator/response/tests |

## Quality assessment

All eight final answers:

- answered the requested question;
- cited exact repository files and lines;
- stayed inside their word limits;
- remained read-only;
- respected financial and repository constraints; and
- required no material factual correction.

The architecture and currency tasks explicitly distinguished repository facts
from inference or recommendations. The freshness answer correctly classified a
direct-call null-kind failure as a confirmed input-handling bug. The frontend
answer inspected the exact pinned revision and covered ready, loading, error,
refreshing, and refresh-failure behavior. Quality therefore remained 8/8 even
though runtime efficiency did not improve overall.

## Interpretation

The optimized structure materially changed *what workflow context was read*:
the all-stage workflow disappeared from every run, stage routing was perfect,
and historical/workflow leakage was eliminated. Static repository context fell
by roughly 75% for these tasks.

That improvement was too small relative to total harness context, code-search
output, and iterative tool replay to produce a stable runtime-token reduction
in this single suite. Tool calls increased 16%, and high-variance repository
exploration—especially the test inventory, frontend discovery, and currency
trace—dominated the measured totals. Per the predeclared threshold, a 0.03%
median change and 0.04% aggregate non-cached reduction are noise-level results.

The architecture should be kept because it preserved quality and demonstrably
improved context discipline, but further claims require repeated identical runs
and investigation of broad search/tool-output behavior. No workflow changes
were made in response to these results.

Runtime workflow result: INCONCLUSIVE

Median token reduction: -0.03% (a 0.03% increase)

Non-cached input reduction: 0.04% aggregate (1.0% median)

Success rate before: 100%

Success rate after: 100%

Stage-routing accuracy: 100%

Unnecessary workflow activations: 0

Biggest measured improvement: Full-workflow reads fell from 8/8 to 0/8 while the freshness task used 16.0% fewer total and 72.5% fewer non-cached tokens.

Biggest remaining context inefficiency: Broad code/test discovery and replay dominate usage, most visibly in the test-repair task's six calls and 215.5% total-token increase.

Recommendation: KEEP AND ITERATE
