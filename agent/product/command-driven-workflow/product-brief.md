# Command-driven development workflow

Status: IMPLEMENTED — independent review pending

## Problem and outcome

Ralph requested removing the machinery that attempts to automate the whole
Jira-to-merge lifecycle. Short user requests should select stages while agents
supply structure and finish the work within each selected stage.

## Approved scope

Explicitly requested on 2026-09-09: start from main, remove unattended
orchestration, and align active agent instructions. Preserve existing work,
financial application behavior, historical evidence, and verification gates.

## Acceptance criteria

- Remove the scheduled orchestrator, Jira preview receiver/admission code,
  worker/review dispatch prompts, and tests exclusive to retired tooling.
- Active agent instructions no longer authorize automatic pickup, dispatch,
  review/fix loops, or merge.
- Preserve user-invoked context/review commands, briefs, UI review templates,
  independent acceptance, ./verify.sh, and required CI.
- Document user-initiated stages and record this decision in the decision log.
- Preserve pre-existing uncommitted coordinator work and local ignored state.

## Non-goals

No replacement coordinator, new slash-command suite, application changes,
external Jira edits, deployment, or automatic merge.

## Delivery handoff

Implemented on codex/command-driven-workflow from main. Existing coordinator
work is saved in a named Git stash on its original branch. Retired compiled
Python caches were removed; ignored state/log directories were retained.
Whitespace and active-reference checks pass. Full backend verification was
attempted; local Java 26 and unavailable Docker prevent a green result.
Crontab inspection returned empty, and no matching orchestrator, receiver,
coordinator, or cloudflared tunnel process was found. External Jira rules and
other installations were not changed or independently verified.

Independent review and green required CI remain prerequisites for shipping.
