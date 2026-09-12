# UI Design Brief: Household Data Entry

## Status

`APPROVED`

## Task and user outcome

- Product Owner Agent: Claude Code, directly with Ralph (D020)
- Priority: Blocking — every read-only view shipped so far (WAP-17 through WAP-24) is unusable with real data until this exists
- Current task: WAP-25
- User problem: every household financial record can only be created via raw backend API calls today; the frontend is entirely read-only, so none of the shipped views can show real data
- Primary user: Ralph, populating and correcting his own household's real records
- User outcome: create assets, liabilities, income streams, obligations, goals, financial snapshots, and planning assumptions from the browser, with corrections to asset/liability values preserving audit history; every read-only view immediately reflects what was entered
- Success measure: Ralph can, unaided, populate his real household data and see it on the existing read-only pages, with the backend's own validation errors surfaced (never re-implemented or swallowed)
- Out of scope: household/person creation UI, authentication/multi-user, AI-assisted or conversational entry, bank/document import, deleting records, multi-household support, any provenance value other than manual entry (see Source context)

## Source context

- Product requirements: WAP-25 Jira description (Frame session, 2026-09-12)
- Domain constraints: `docs/domain/financial-model.md` (provenance concept lists six possible source kinds, but the implemented `SourceType` enum — `backend/src/main/java/com/waypoint/household/SourceType.java` — currently has only `MANUAL_ENTRY`, and no create endpoint accepts a source-type field at all: the service layer hardcodes it). This means the Jira acceptance criterion "every persisted value carries a provenance tag" is already met by the backend automatically; **no provenance picker belongs in any of these forms**. The financial-position design brief's implementation handoff already reached the same conclusion for the read side.
- Existing decisions: D002 (server computes, browser/LLM never do arithmetic), D021/D022 (conditional-revision correction pattern for asset valuations and liability balances — the correction forms must carry `expectedRevision` and handle a 409 conflict as a distinct, structured case, not a generic error)
- Existing frontend conventions (all reused verbatim, not reinvented):
  - Every page (`FinancialPositionPage`, `GoalsPage`, `IncomeObligationsPage`, `SnapshotsPage`, `PlanVersusActualPage` in `frontend/src/App.tsx` / `GoalsPage.tsx` / `components/*.tsx`) follows one shape: `.page` > `.app-header` (`<h1>` + a `.refresh-btn`) > a `status-line`/`banner` region > list-or-empty-state content.
  - A working create-and-validate form pattern already ships today in `GoalContributionCalculator.tsx` (and the scenario-tool sections): `.field` blocks (`<label>` + `<input>`/`<select>`), a submit button, and on failure a `.banner[role=alert]` with a bold message plus a `<ul>` of the backend's own detail strings. This is the exact shape of `ErrorResponse` (`backend/src/main/java/com/waypoint/web/ApiExceptionHandler.java`): `{ code, message, details: string[] }`, where `details` is already human-readable (`"fieldName: message"`) — display it as-is, do not re-parse or re-map per field.
  - CSS already defines everything a plain form needs: `.field`, `.field input`/`.field select` (+ focus-visible rings), `.card`, `.banner`, `.refresh-btn`/`.submit-btn`, `.toggle-btn`/`aria-expanded` (used today by `CurrencyCard` for expand/collapse — the same mechanism fits an inline "Add" disclosure).
- API contracts (fields only; all monetary/decimal fields are strings or `BigDecimal` — send as decimal strings, never `Number`):
  - `POST /api/households/{id}/assets` — name, assetType (enum: CASH/BANK_ACCOUNT/PROPERTY/INVESTMENT/BUSINESS_OWNERSHIP/OTHER), estimatedValue, planningValue (≤ estimatedValue, enforced server-side), currency (3-letter), valuedAt (≤ today), liquidity (enum: LIQUID/RESTRICTED/ILLIQUID)
  - `POST /api/households/{id}/assets/{assetId}/valuations` (correction) — estimatedValue, planningValue, valuedAt, reason (required, ≤500 chars), expectedRevision. Current revision comes only from `GET .../assets/{assetId}/valuations` (list/get `AssetResponse` does **not** include revision) — the correction form must fetch that first.
  - `POST /api/households/{id}/liabilities` — name, liabilityType (enum: CREDIT_CARD/MORTGAGE/PERSONAL_LOAN/BUSINESS_LOAN/OTHER), outstandingBalance, currency, balanceAsOf (≤ today)
  - `POST /api/households/{id}/liabilities/{liabilityId}/balances` (correction) — outstandingBalance, balanceAsOf, reason (required, ≤500 chars), expectedRevision. Current revision **is** already present on `LiabilityResponse` from the existing list — no extra fetch needed (an asymmetry with assets, noted so implementation doesn't assume the two correction flows are identical).
  - `POST /api/households/{id}/income-streams` — name, incomeType (SALARY/HOURLY_CONTRACT/BUSINESS_DISTRIBUTION/OTHER), amount, frequency (HOURLY/WEEKLY/BIWEEKLY/MONTHLY/ANNUAL), currency, compensationClassification (GROSS/NET/UNKNOWN), certainty (CONFIRMED/EXPECTED/VARIABLE), startDate, endDate (optional)
  - `POST /api/households/{id}/obligations` — name, obligationType (HOUSEHOLD_BASELINE/MORTGAGE/LOAN_PAYMENT/INSURANCE/TUITION/TRAVEL_SINKING_FUND/DISCRETIONARY/OTHER), amount, frequency, currency, startDate, endDate (optional)
  - `POST /api/households/{id}/goals` — name, targetAmount (> 0), currency, targetDate (≥ today), priority (positive integer), currentAmount (≥ 0)
  - `POST /api/households/{id}/financial-snapshots` — asOfDate (≤ today) only; no other fields — this is a one-field form, not a record form
  - `POST /api/households/{id}/assumptions` — name, value (freeform string, ≤1000 chars), valueType (freeform string, ≤100 chars — not an enum), notes (optional, ≤2000), effectiveFrom, effectiveUntil (optional), reviewDate
  - `POST /api/households/{id}/assumptions/{id}/supersede` — same fields as create; not revision-guarded (D021/D022 pattern does not apply here — supersession is its own service-level check, `AssumptionAlreadySupersededException`), so no `expectedRevision` field
  - Validation failures: 400 `VALIDATION_FAILED` with `details[]`; not-found: 404 with an entity-specific code; correction conflicts: 409 `ASSET_REVISION_CONFLICT` / `LIABILITY_REVISION_CONFLICT`
- Reference images or products: `GoalContributionCalculator.tsx` is the literal reference implementation for form structure and error display — not a mockup, a shipped pattern.

## Information and actions

- Information the user must understand: which page/list a new record will appear on before they submit (so results feel predictable, not magical); that a correction preserves history rather than silently overwriting (D021/D022 — the form must say so); that a stale correction (409) means the value changed since it was loaded, not that the input was invalid
- Primary action: create a record for a given entity
- Secondary actions: correct an asset's valuation or a liability's balance; supersede a planning assumption
- Financial classifications that must remain explicit: asset vs. liability vs. income vs. obligation (never a shared generic "record" form — one form per entity type, matching its own fields exactly); planning value vs. estimated value on assets (both fields shown, never derived or hidden); manual-entry provenance is automatic and not user-selectable (see Source context)

## Directions explored

### Direction A — Inline "Add"/"Correct" actions on each existing page, one new minimal tab for assumptions

- Structure: each existing page's `.app-header` gains one button per entity type it already lists (`FinancialPositionPage`: "Add asset", "Add liability"; `IncomeObligationsPage`: "Add income stream", "Add obligation"; `GoalsPage`: "Add goal"; `SnapshotsPage`: "Create snapshot"). Each button toggles an inline form card, reusing the `CurrencyCard` expand/collapse mechanism, positioned above that page's list. Each asset/liability table row gains a "Correct" action opening the same kind of inline form pre-filled with current values. Planning assumptions have no existing host page (`PlanVersusActualPage` is a stateless calculator that does not read `PlanningAssumption` records at all), so this direction adds exactly one new nav entry, "Planning assumptions" (list + "Add" + per-row "Supersede"), built from the identical page shape as every other tab.
- Interaction: `aria-expanded`/`aria-controls` toggle buttons (proven pattern, already shipped); on success the form collapses, a brief confirmation appears in the existing `status-line`/`aria-live` region, and the page's own data hook refreshes so the new record appears in place immediately.
- Strengths: zero new interaction paradigms — every piece (header button, toggle-disclosure, field/banner form, list refresh) already exists and works today; a created record appears exactly where the user was already looking, satisfying "each read-only view reflects the new record" with no navigation; matches the Jira user flow's own first option; adds the one new tab only where the domain genuinely has no home for it, keeping new navigation truly minimal.
- Tradeoffs: a household populating everything for the first time must visit five different tabs in sequence rather than working through one checklist; two entity types share one page in two places (position, income/obligations), so those two headers need two buttons instead of one, which is slightly busier than the other headers.

### Direction B — Single new "Add records" hub tab

- Structure: one new nav tab with an entity-type selector (tabs or a `<select>`) and one form area; submitting shows a confirmation with a link to the entity's real page.
- Interaction: no per-page disclosure; the hub renders the shared field/banner form for whichever entity is selected.
- Strengths: one place for the "populate everything now" first-run flow — a natural checklist.
- Tradeoffs: breaks the pattern every other page in this app uses (action co-located with its result); the user never sees the record land next to its existing siblings without navigating away and back, weakening the "reconciles immediately" property the financial-position brief established as a core value; corrections (which are inherently per-record and need the row's current value/revision in front of the user) fit awkwardly in a page that has no record list to correct from — would need to duplicate lookups the existing tables already do for free; still needs the same new-tab cost as Direction A's one addition, but for all seven entities' worth of UI instead of one.

## Proposed direction

- Recommendation: **Direction A.**
- Reasoning: it costs the least new interaction surface (reuses the exact toggle/form/banner primitives already shipped and proven in `GoalContributionCalculator`), keeps the "result appears where you were looking" property every existing page already has, and adds new navigation only for the one entity (planning assumptions) that genuinely has nowhere else to live. Direction B's single-hub convenience for first-time bulk entry doesn't outweigh breaking that established co-location pattern across every other page, and it complicates the two correction flows for no benefit.
- Wide layout: header button(s) sit to the left of the existing `.refresh-btn`; the inline form card renders full-width above the list content, same as `.card` today.
- Narrow layout: header buttons wrap under the `<h1>` the same way `.app-nav` already wraps at narrow widths; `.field` blocks are already single-column/full-width, so no new narrow-width work is needed for the form body itself.
- Loading state: a correction form for an asset must show its own brief "Loading current value…" line while it fetches `GET .../assets/{id}/valuations` for the revision, before rendering fields pre-filled — this is the one place data entry has its own loading state (liability corrections and all create forms open instantly since they need no prior fetch).
- Empty state: the four existing empty-state messages that currently read "Add records through the existing household data tools, then refresh this page" (in `App.tsx`'s `FinancialPositionPage`/`SnapshotsPage`... — actually present on `FinancialPositionPage`, `GoalsPage`, `IncomeObligationsPage`) become literally actionable once this ships and must be reworded to name the real action now on the same page (e.g. "Add asset above to record one."), since that copy currently describes a tool that doesn't exist yet (the empty-state copy problem the Jira issue itself names).
- Validation and error states: 400 → `.banner[role=alert]` with the bold message plus the `details[]` list verbatim, form stays populated for correction (never clear a failed submission); 409 (correction only) → a distinctly worded banner ("This value has changed since it was loaded.") with a "Reload current value" action that re-fetches and re-populates the form rather than letting the user resubmit against a stale revision; network/not-found errors reuse the existing `ConfigNotFound`/generic-error conventions already on every page.
- Accessibility considerations: toggle buttons keep the existing `aria-expanded`/`aria-controls` contract; opening a form moves focus to its first field; a completed submit (success, validation failure, or conflict) moves focus to the resulting status/banner region so keyboard and screen-reader users aren't stranded on a now-hidden control; all fields keep real `<label htmlFor>` pairing per the existing `.field` convention; no color-only state signaling (409 vs. 400 are distinguished by wording, not just color, consistent with the rest of the app).

## Acceptance criteria

- [ ] Ralph can create an asset and a liability with required fields, visible on Financial Position without leaving the page.
- [ ] Ralph can correct an asset value or liability balance via a form pre-filled with the current value; a stale submission (409) is distinguished from an invalid one (400) and offers a reload path; prior values remain visible in existing history views.
- [ ] Ralph can create an income stream and an obligation, visible on Income & Obligations without leaving the page.
- [ ] Ralph can create a goal, visible on Goals without leaving the page.
- [ ] Ralph can create a financial snapshot from a one-field form, visible on Snapshots.
- [ ] Ralph can create or supersede a planning assumption from the new Planning assumptions tab.
- [ ] No form includes a provenance/source-type picker (backend has exactly one value and does not accept it as input).
- [ ] Every form surfaces `details[]` from the backend's `ErrorResponse` verbatim; no client-side re-implementation of a backend validation rule.
- [ ] The four existing "add records through the existing household data tools" empty-state strings are reworded to point at the real, now-present action.

## Decision

- Selected direction: **Direction A — inline Add/Correct actions on each existing page, plus one new minimal "Planning assumptions" tab.**
- Amendments: None; approved as proposed.
- Approved by Product Owner Agent: **Ralph, directly with Claude Code standing in as Product Owner Agent (D020)**
- Approved at: **2026-09-12**

Changing status to `APPROVED` confirms the selection above.

## Implementation handoff

- Implemented by: Claude Code, WAP-25, `task/wap-25-household-data-entry`
- Automated checks and results: `./verify.sh` — backend `BUILD SUCCESS`, `Tests run: 829, Failures: 0, Errors: 0, Skipped: 0`; frontend clean install, `tsc -b --noEmit` clean, `oxlint` clean, Vitest `Test Files 22 passed, Tests 115 passed` (includes new unit/component coverage for the shared submit hook, `AddAssetForm`, `CorrectAssetValuationForm` — load-then-correct plus the 409-conflict/reload path — and the new `PlanningAssumptionsPage`), production build succeeded. Additionally: `frontend/e2e/real-backend-data-entry-smoke.sh` (isolated disposable stack — real Postgres/backend/nginx, no pre-seeded records) `PASS`: every entity created through the real UI, an asset valuation corrected with the prior value confirmed still present via the real `valuations/history` endpoint (D021/D022), and a planning assumption superseded — all via real Chromium — torn down afterward with the shared `waypoint-postgres-data` volume left untouched.
- Evidence: `evidence/implementation/data-entry-position-populated-wide.png` (asset + liability created, expanded currency card), `evidence/implementation/data-entry-assumptions-wide.png` (one current + one superseded planning assumption), both captured by the real-backend smoke spec above (real React app, real Chromium).
- Deviations from approved design: (1) the brief's liability-correction note assumed `LiabilityResponse`'s `revision` field would already be in hand from the page's existing data; in fact the Financial Position page reads the revision-less `PositionLiability` (position-package DTO), so the liability correction form fetches `GET /liabilities/{id}` fresh on open, exactly like the asset flow — both corrections now have a brief "Loading current value…" step, not just the asset one. (2) `Disclosure` (the shared add/correct toggle) unmounts its panel while closed rather than using `hidden` like `CurrencyCard`'s expand/collapse: an unopened form has no state worth keeping, and keeping same-labeled fields ("Name", "Currency", ...) for every entity's form simultaneously mounted-but-hidden on one page caused real ambiguity for assistive tech and tests alike.
- Known limitations: no provenance picker anywhere, by design — see the brief's Source context; the backend's `SourceType` enum has only `MANUAL_ENTRY` and no create endpoint accepts one as input, so every record this feature creates is already correctly tagged without any UI for it. Planning-assumption supersession has no revision/conflict handling (matches the backend: `AssumptionAlreadySupersededException` is a plain 400, not a 409 — different from the D021/D022 pattern the asset/liability corrections use). No dedicated UI renders the asset/liability correction history list itself; the audit trail's existence and correctness is verified through the real backend history endpoints (see Automated checks above), consistent with the ticket's "preserving audit history" scope rather than a new history-browsing view.

Feature acceptance (met/unmet criteria, ACCEPTED/RETURNED, follow-up work) is
recorded as a comment on the Jira issue, not here — see
`agent/collaboration-workflow.md`.
