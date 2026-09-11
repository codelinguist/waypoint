---
description: Load the minimum authoritative context for the current Waypoint stage
---

# Prime Waypoint context

Read `AGENTS.md`, classify the user's current request, and load only the linked
stage file. Do not start work merely because Jira or repository state suggests
it is ready. If the task is Jira-backed, read the issue description and current
round comments in full; skim older rounds by outcome unless their reasoning is
needed now.

Follow the stage file's activation rules, scan `docs/decisions/index.md` when
triggered, and inspect named code/contracts plus direct dependencies. Check
targeted recent git history and status without reading historical logs.

Report the task, stage, activated constraints, relevant recent activity, and
genuine open questions concisely.
