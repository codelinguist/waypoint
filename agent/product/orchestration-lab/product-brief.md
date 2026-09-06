# Product brief: LAB-01 execution contract

Status: READY
Product owner: Codex
User: Ralph
Created: 2026-09-06

## User input and framing

Ralph requested hands-on learning of orchestration architectures and explicitly
started the saved implementation plan. Initiation is manual while moving toward
events. The immediate problem is ambiguous task/attempt ownership, demonstrated
by the current fix dispatch path and multiple blocked fix sessions for the same
tasks. Success is an implementable contract that later architectures can share.

Ralph clarified on 2026-09-06 that Waypoint will serve real users with real impact.
The learning program must advance a usable, reliable application. Experimental
workflow infrastructure does not lower product acceptance, financial correctness,
privacy, or security requirements. Continue delivering useful product increments;
run destructive failure experiments on synthetic work in isolated environments.

## Scope and acceptance criteria

- [x] Define field ownership and separate immutable task/attempt identities.
- [x] Specify legal transitions, dispatch identity, and unknown-outcome handling.
- [x] Specify cancellation, leases, fencing limitations, and revision-bound merge gates.
- [x] Define manual initiation outside the executable cron queue.
- [x] Provide failure walkthroughs as implementation acceptance scenarios.

Evidence: [LAB-01 contract](lab-01-execution-contract.md).
Out of scope: runtime code, cron changes, worker termination, Jira mutations,
storage/engine selection, and guarantees not yet implemented.

## Decisions and assumptions

Design direction: one coordinator owns lifecycle; workers report results;
uncertain dispatch blocks instead of blindly relaunching. Leases alone cannot
fence workers with direct mutation access. Initial attempts are serialized per
task, including reviews. These are specification decisions for implementation,
not a migration of existing runtime behavior.

No household preference or financial decision is introduced. Jira destination and
unattended authentication are deferred to setup/integration tasks. No new external
notification or automatic worker dispatch is enabled.

## Delivery and acceptance

User design approval: Ralph approved the execution contract on 2026-09-06
("execution contract looks good to me"). This approves LAB-01's specification,
not unimplemented runtime behavior. PR review and required CI remain outstanding.

Local verification attempt on 2026-09-06: `./verify.sh` failed with 150 errors
out of 247 tests (zero assertion failures), including Mockito MockMaker
initialization and a Testcontainers PostgreSQL image fetch error. Do not merge
until the required verification gate is green.

Branch: `task/022-orchestration-lab-plan` (plan and initial contract documentation).
Verification: documentation diff/whitespace review; no runtime changes.
Feature acceptance: PENDING independent PR review; checked criteria indicate
document coverage, not tested runtime guarantees.
Next task: manually prepare and implement LAB-02 against this contract.
