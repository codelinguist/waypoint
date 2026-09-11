# Waypoint workflow index

The user selects work and explicitly starts each meaningful stage. An agent
completes only that stage and reports the result; readiness, Jira status, PR
creation, and green CI never trigger the next stage.

| Stage | Activate when | Canonical instructions |
| --- | --- | --- |
| Investigate | explain, inspect, diagnose, architecture analysis | `agent/workflow/investigate.md` |
| Frame | define the problem, scope, and acceptance criteria | `agent/workflow/frame.md` |
| Design | explore or approve material UI direction | `agent/workflow/design.md` |
| Implement | change code or repository documentation | `agent/workflow/implement.md` |
| Review / Accept | independently assess a PR | `agent/workflow/review.md` |
| Revise | resolve recorded review findings | `agent/workflow/revise.md` |
| Ship | explicitly requested merge and closeout | `agent/workflow/ship.md` |

Do not read every stage. `AGENTS.md` contains universal invariants and context
activation rules; each file above is canonical for that stage's procedure and
delivery authority. A user request is required to cross to another stage.

For feature work, the Jira issue, checked-in code, and recorded evidence are
the handoff boundary. Codex is the preferred agent for Frame, design
approval, Review, and Accept; Claude Code may perform any of these directly
with the user on request, not only when Codex is unavailable (D020). Claude
Code normally owns Implement, Revise, and Ship regardless. An explicit
request may override these defaults, but not the household approval or Ship
gates in `AGENTS.md`.
