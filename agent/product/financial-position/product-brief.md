# Product Brief: First Financial Position Experience

## Status

`READY` — product scope authorized; UI design and implementation remain pending.

## Ownership

- Product Owner Agent: Codex
- Users: Ralph and his wife
- Created at / updated at: 2026-09-06

## User input

Ralph explicitly requested framing the first financial-position experience, exploring and approving its design, and building a React/TypeScript interface connected to the APIs with desktop/mobile verification. This brings a narrow Phase 9 slice forward; it does not require completion of Tasks 012–019.

## Product framing

- Problem: Household records and calculations exist behind APIs, but the household cannot inspect what it owns, what it owes and net worth in an everyday interface.
- Outcome: Open a private, configured household page, understand its recorded financial position by currency, and inspect the records and dates behind each total.
- Success: Every displayed total reconciles to the displayed source rows, currencies remain separate, and empty/error/stale-date semantics are understandable on desktop and mobile.
- Priority: First usable frontend slice; start design and the small missing read model independently.

## Knowledge classification

### Confirmed inputs

- Existing household, asset and liability APIs support retrieval; asset records distinguish estimatedValue from planningValue and carry liquidity, valuation date and provenance.
- Existing snapshots provide per-currency totals but are persisted historical captures. There is no current-position summary endpoint.
- The household API has no list endpoint; this first page needs a configured existing household ID.
- No household records or financial preferences are authorized to be created by this work.

### Product assumptions to validate

- A read-only view is the smallest useful first experience. Existing records are populated separately through existing APIs; onboarding and data editing are follow-up work.
- Net worth uses asset planningValue minus outstanding liability balance, matching snapshot convention. Estimated values remain separately labeled, never substituted silently.
- This page describes the latest recorded rows, not independently verified present-day values.

### Open questions

- Feedback after first use should decide the next slice: entering/correcting balances, historical comparison or cash flow. None blocks this read-only experience.

## Scope

### In scope

- React/TypeScript single-page financial position for one existing configured household.
- Household name, per-currency assets/liabilities/net-worth summaries, and inspectable asset and liability lists.
- Asset planning value, separately labeled estimated value, type, liquidity, currency, valuation date and provenance; liability outstanding balance, type, currency, balance date and provenance.
- Server-calculated totals returned with their source rows by a new read-only endpoint, defined in the current-financial-position brief.
- Plain explanation of planning values and dated records; liquidity must not imply all wealth is spendable cash.
- Accessible desktop/mobile layouts, refresh, deliberate loading/empty/error states, and reproducible private local startup.

### Out of scope

- Record creation/editing, household creation/selection, authentication, public deployment, charts/history, goals, cash flow, scenarios, AI chat, FX, banking connections and calculated liquidity/runway advice.
- No automatic snapshot creation, demo records in the household database or financial data in browser persistent storage/logs.

## User flow

1. The local app uses an operator-configured household ID. Missing configuration gives a readable setup message, not fictional financial data or a UUID entry form in the ordinary household flow.
2. The household opens Financial position and sees its name and recorded totals grouped by currency.
3. It inspects what contributes to each total and sees values, classifications and source dates.
4. It refreshes to retrieve a new complete response; a failure cannot masquerade as zero wealth or silently mix old rows with new totals.

## Acceptance criteria

- [ ] Design exploration compares at least two materially different information/interaction structures, covers wide/narrow layouts and all required states, and records an explicit Product Owner selection before production UI coding.
- [ ] The page works against the real Spring API for an explicitly configured household without depending on unfinished planning or scenario tasks.
- [ ] Per-currency totals exactly match server totals; no combined FX total, browser financial arithmetic or lossy monetary formatting. Mixed currencies and values beyond JavaScript safe-integer precision retain exact cents from the decimal-string API contract.
- [ ] Source rows explain all totals. Planning values, estimates, source dates and liquidity retain their meaning; negative net worth is displayed accurately without prescribing household action.
- [ ] Empty existing households, assets-only/liabilities-only currencies, missing/invalid household configuration, household 404, network/server failure, loading and refresh failure have explicit tested behavior.
- [ ] No stale response from an earlier request replaces a newer response. Refresh errors either clear results or explicitly retain and label the last successful complete result; the approved design chooses one.
- [ ] At 390px and 1440px widths, primary content has no page-level horizontal overflow; long names and large amounts remain usable. Keyboard access, visible focus, semantic headings/tables or lists, accessible status messages, sufficient contrast and 200% zoom are verified.
- [ ] Local Compose startup serves the frontend and API privately, and the canonical ./verify.sh includes meaningful frontend type/build/tests alongside the unchanged complete backend suite. Required CI verify runs the same command and provisions its needed runtime/browser dependencies.
- [ ] Browser tests cover the primary read/refresh flow, mixed currencies, precise large amounts and failures; at least one synthetic integration smoke flow uses the real backend rather than only mocked responses.
- [ ] Wide/narrow screenshots and visual review are recorded with synthetic data; Product Owner acceptance is pending until the implemented flow and required checks are reviewed.

## Risks and safeguards

- Financial authority: Read-only; no financial decision, threshold, target or household record is approved.
- Privacy: Local/private delivery only. Synthetic evidence and isolated disposable database; never seed or reset the shared household volume.
- Accuracy: Current records can have different source dates; never label them all as valued today. Retrieval time is distinct from source valuation/balance dates.
- Accessibility: Required above; color alone must not carry meaning.

## Product decisions

### PD-001 — Bring forward one Phase 9 slice

- Decision: Start this experience before completing the full scenario engine.
- Evidence: Ralph explicitly requested this framing on 2026-09-06.
- Alternative: Continue strict phase ordering.
- Rationale: Existing record APIs support a useful household feedback loop; broader dashboard views stay deferred.
- User input required: NO — explicitly authorized.

### PD-002 — Add an authoritative current-position read model

- Decision: Return current source rows and deterministic decimal totals together; no snapshot side effect and no client-side net-worth arithmetic.
- Evidence: Existing totals are only on saved snapshots, and D002 keeps important math deterministic and tested.
- Alternative: Use an old snapshot as current state or sum separate API responses in the UI.
- Rationale: The page needs a coherent, auditable read with exact monetary transport.
- User input required: NO — follows the existing planning-value convention.

## Delivery handoff

1. Task 020 financial-position-design: explore and obtain design approval; no application implementation.
2. Task 021 current-financial-position: implement the read model independently of design.
3. After both are accepted/merged, Product Owner creates the next unused QUEUED UI task from implementation-task-draft.md. That contract is deliberately outside agent/tasks until ready: the orchestrator has no documented dependency scheduler.

- Design artifact: agent/ui/financial-position/design-brief.md (to be authored by Task 020).
- UI implementation owner: Claude Code, fresh conversation seeded only with accepted artifacts.
- Review: agent/ui/financial-position/visual-review.md and evidence/.

## Feature acceptance

- Acceptance status: PENDING
- Evidence: Framing only; no design approval or application implementation claimed.
- Unmet criteria: Design, API and frontend delivery/verification.
- Follow-up opportunities: Data entry/correction and household feedback-driven prioritization.
