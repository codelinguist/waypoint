# Draft execution contract: Financial Position React UI

This is not a queued task and has no lifecycle status. Do not place it in agent/tasks until Tasks 020 and 021 are accepted and merged, the design is APPROVED, and its endpoint contract is checked against the merged code. The Product Owner then assigns the next unused number and normal task/<NNN>-financial-position-ui branch. Do not assume a task number or dependency syntax is reserved/supported.

## Goal

Implement the parent financial-position product brief and approved design as the first usable React/TypeScript page, consuming GET /api/households/{householdId}/financial-position.

## Required deliverables

- Start a fresh implementation conversation from checked-in approved artifacts; no design exploration conversation as context.
- Add a minimal frontend/ with pinned package manifest/lockfile, typed API contract, responsive page and meaningful tests. No broad navigation shell with nonfunctional destinations.
- Render exact decimal strings without converting large monetary values through Number; use presentation-only exact formatting. All financial totals come from the server.
- Configure existing household identity outside the ordinary product flow. A missing/invalid setup or absent household has an actionable state; do not create household records automatically.
- Integrate local startup with docker compose up --build, using the existing app/database and a minimal frontend serving/proxy arrangement. Keep API access local/private and avoid broad CORS enablement. Document the URL and household configuration.
- Extend root ./verify.sh to run frontend clean install, type checks, tests and production build, plus the complete existing Java/PostgreSQL suite. Update required verify CI runtime/browser setup to execute that same command; never remove or weaken existing tests. Pin compatible Node/toolchain versions at implementation time.
- Implement parent acceptance criteria for currency separation, metadata, precision, states, request races and refresh behavior.
- Browser verification at 390px and 1440px, keyboard and 200% zoom; synthetic screenshots under agent/ui/financial-position/evidence/. Exercise at least one real API flow using isolated disposable fixtures without touching the shared household volume.
- Record visual review with agent/templates/ui-visual-review.md, verification commands, deviations and limitations. Resolve blocking findings before acceptance.

## Ownership and concurrency

Own frontend/**, the minimal Compose/Docker/ignore/runtime configuration changes needed to run it, verify.sh, its required GitHub Actions verify workflow, README startup instructions, agent/ui/financial-position/**, and agent/product/financial-position/**. Read backend position API without edits. This task owns shared frontend/startup/verification surfaces exclusively; do not dispatch another task editing them concurrently. Append the central implementation log and record any new durable architecture decision explicitly; preserve unrelated entries.

## Definition of Done

The complete parent product brief is evidenced against the approved design and merged backend API. Local and required CI verify pass, real desktop/mobile flows work, and independent Product Owner acceptance is recorded before merge. No public hosting or household financial-data writes are authorized.
