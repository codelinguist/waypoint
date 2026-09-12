# UI Design Brief: Household Onboarding Wizard

## Status

`APPROVED`

## Task and user outcome

- Product Owner Agent: Claude Code, directly with Ralph (D020)
- Priority: Sequenced after WAP-25 (reuses its entry forms as step bodies); not blocking WAP-25
- Current task: WAP-26
- User problem: a brand-new household has no in-browser path to exist at all — `Household`/`Person` creation has no UI anywhere — and even once WAP-25 ships, a first-time household would still have to discover and visit five separate pages to populate enough data to see a real financial position.
- Primary user: Ralph, on a brand-new household's very first session.
- User outcome: create the household and its people, then walk through assets, liabilities, income streams, obligations, and goals, ending with the household's first financial snapshot — all in one guided sequence.
- Success measure: Ralph completes the wizard unaided and lands on Financial Position (and Goals, Income & Obligations, Snapshots) showing real, non-empty data, with no manual `HOUSEHOLD_ID`/container-restart step in between.
- Out of scope: planning assumptions, bank/document import, AI-assisted entry, edit/correction flows (stay WAP-25's), multi-household support, authentication.

## Source context

- Product requirements: WAP-26 Jira description (Frame session, 2026-09-12)
- Domain constraints: `docs/domain/financial-model.md`; PD-002/PD-003 in `agent/product/household-foundation/product-brief.md` (no auto-seeding from `user-zero.md`; person identity stays minimal — name and role only, no birth date/contact/ID fields)
- Existing decisions: D002 (server computes, browser never does arithmetic), D006 (private, single-household product — this wizard does not add multi-household support), D021/D022 (not directly touched — corrections stay WAP-25's, out of scope here)
- **Feasibility finding (2026-09-12, from reading the WAP-25 branch, `task/wap-25-household-data-entry`, PR #57)**: the running frontend has no browser-side way to start using a newly created household. `frontend/src/config.ts` reads `window.__WAYPOINT_CONFIG__.householdId`, which `frontend/docker/docker-entrypoint.sh` injects into `config.js` from the `HOUSEHOLD_ID` environment variable **at container startup only**. `App.tsx`'s only response to a missing/invalid/not-found config is static text telling the operator to set `HOUSEHOLD_ID` and restart the container (`ConfigMissing`/`ConfigInvalid`/`ConfigNotFound` in `frontend/src/components/ConfigProblem.tsx`) — there is no in-browser fallback today, and no `createHousehold`/`createPerson` calls exist in `frontend/src/api/client.ts` at all.
  - **Resolved with Ralph (2026-09-12)**: `config.ts`'s resolution order gains a browser-local override, checked before `window.__WAYPOINT_CONFIG__`. The wizard's household-creation step writes the newly created household ID there on success, so the wizard and the rest of the app work immediately in that browser — this session and later ones — without a restart. The `HOUSEHOLD_ID` env var is unchanged as the durable/production way to pin a deployment to a household; this override only bridges "just created it" to "the app is already running."
- Existing frontend conventions (all reused verbatim, not reinvented):
  - WAP-25's entry forms (`frontend/src/components/entry/AddAssetForm.tsx`, `AddLiabilityForm.tsx`, `AddIncomeStreamForm.tsx`, `AddObligationForm.tsx`, `AddGoalForm.tsx`, `CreateSnapshotForm.tsx`) are the literal step bodies for their respective wizard steps — not references, the actual components, reused as-is. Their `useEntrySubmit` hook (`frontend/src/hooks/useEntrySubmit.ts`) and `Disclosure` open/close pattern (`frontend/src/components/Disclosure.tsx`) are the established submit/error/focus conventions.
  - `.page` > `.app-header` (`<h1>` + action) > `.status-line`/`.banner` > content is every page's shape (`App.tsx`); `.field`, `.calc-form`, `.submit-btn`, `.toggle-btn` cover form styling (`frontend/src/index.css`).
  - `AppNav` in `App.tsx` is the only navigation mechanism today (button row, `aria-current="page"`), wrapping under the heading below 700px (`@media (max-width: 700px)`, `index.css`).
- Reference images or products: `AddAssetForm.tsx` is the literal reference implementation for step-body form structure (fields, submit state, error banner) — a shipped pattern, not a mockup.

## Information and actions

- Information the user must understand: which step they're on and how many remain; that "Next" requires at least one record for that entity type (comprehensive scope, decided at Frame); that finishing the wizard is what makes the rest of the app usable, not a separate action afterward.
- Primary action: advance through the sequence by creating at least one record per step.
- Secondary actions: add more than one record within a step before advancing; go back a step; (for an already-configured household) opt into the wizard from an empty-state banner instead of being forced into it.
- Financial classifications that must remain explicit: identical to WAP-25 — asset vs. liability vs. income vs. obligation vs. goal never collapse into one generic step form; each step is that entity's real create form, unmodified.

## Directions explored

### Direction A — Replace the config-missing screen with the wizard; an optional, explicitly-launched flow afterward

- Structure: `App()`'s existing `config.status === 'missing'` branch (today renders static `ConfigMissing` text) instead renders a new `OnboardingWizard` component in place of the whole app shell — there's no household yet, so there's nothing for `AppNav` to navigate between. The wizard steps through Household + Person(s) → Assets → Liabilities → Income streams → Obligations → Goals → Snapshot, each step's body being the actual existing WAP-25 form for that entity, unmodified, plus a read-only list of records already added at that step (reusing `AssetTable`/`LiabilityTable`/etc.) so "add another" has visible feedback. Step 1 is a net-new `AddHouseholdForm`/`AddPersonForm`, built to the exact same field/button/banner conventions as every WAP-25 form. On successful household creation, the wizard writes the new ID to the browser-local override (see Source context) and every later step operates on that ID like any other `householdId` prop. After the closing snapshot step, the wizard unmounts and `App()` re-renders normally (`AppNav` + `FinancialPositionPage`). For an already-configured household with empty data, a small banner ("New here? Run guided setup") on Financial Position — beside its existing empty-state text — opens the same wizard component pre-seeded to skip the household/person step; dismissing it or navigating away leaves WAP-25's ordinary per-page "Add" forms as the normal path.
- Interaction: single-page step-through (no tabs), a step-progress line in the header ("Step 3 of 7: Liabilities"), "Next" (enabled once the step has at least one record) and, from step 2 on, "Back"; validation/success behavior is whatever the reused form already does.
- Strengths: reuses WAP-25's form components and their tests/behavior verbatim — this is wiring and sequencing, not new form logic; "not forced back through the wizard" falls out naturally, since it only intercepts the genuine first-run case (`config.status === 'missing'`) — everything else is opt-in; extends the existing pattern of `App()` branching on top-level state (config status) rather than introducing a router.
- Tradeoffs: the wizard owns step-sequencing state that has no precedent elsewhere in the app; the "resume for an already-configured empty household" path is a secondary, banner-triggered entry rather than the primary one, so it gets less natural exercise.

### Direction B — Permanent "Get started" nav tab, always present

- Structure: add "Get started" as a permanent `AppNav` entry in every config state. When `config.status === 'missing'`, every other nav button is disabled (`aria-disabled` + explanatory status line) until the household step completes; once configured, "Get started" stays clickable at any time and reopens the same wizard, with steps that already have data showing "already recorded — add another or continue" instead of assuming empty.
- Interaction: ordinary tab switch, consistent with every other view; disabled nav buttons are new — none exist today.
- Strengths: one mental model — onboarding is just another page, always reachable, supporting revisits without a special first-run condition.
- Tradeoffs: needs new disabled-nav-button styling/behavior with no precedent in `index.css`/`AppNav`; an 8th permanent nav item that's only genuinely useful once, unless later hidden — and hiding it conditionally reintroduces the same top-level branch Direction A already uses, just relocated into `AppNav`; still needs the same `config.status === 'missing'` special case for the very first load (no household means nothing else is clickable anyway), so it adds a second mechanism (disabled buttons) without removing the first.

## Proposed direction

- Recommendation: **Direction A.**
- Reasoning: it fully reuses the `config.status` branch `App()` already has instead of inventing a parallel disabled-nav-button mechanism; it reuses every WAP-25 form verbatim; and Direction B's "always visible" tab doesn't actually remove the need for that same first-run branch — it just adds complexity without a real benefit, since once a household exists, WAP-25's ordinary per-page "Add" forms are already the primary path per WAP-26's Frame decision.
- Wide layout: wizard content matches existing `.page`/`.card` width; the step-progress line sits where `.app-header`'s `<h1>` normally does (e.g. "Set up your household — Step 3 of 7").
- Narrow layout: step-progress line wraps under a shorter heading below 700px, matching how `.app-nav` already wraps; Back/Next buttons stack full-width, matching `.submit-btn` conventions.
- Loading state: household creation uses the same `submitting` pattern as every WAP-25 form ("Adding…", disabled submit); no skeleton needed elsewhere in the wizard — there's nothing to fetch, only submit.
- Empty state: each step's "already added" list starts with a one-line prompt ("No assets added yet — add one above."), matching WAP-25's existing empty-state phrasing; "Next" stays disabled until the step has at least one record, per WAP-26's comprehensive-scope decision.
- Validation and error states: identical to the reused forms — 400 banner with `details[]` shown verbatim, form stays populated on failure; a household-creation failure keeps the user on step 1 (nothing else is reachable without a household).
- Accessibility considerations: a step change moves focus to the new step's heading (matching `Disclosure`'s existing focus-on-open behavior); the step-progress line is in an `aria-live="polite"` region; Back/Next are plain labeled buttons, never icon-only; every reused form's existing `<label htmlFor>` pairing carries over unchanged.

## Acceptance criteria

- [ ] On a fresh deployment with no `HOUSEHOLD_ID` configured, the app shows the wizard instead of the current "set `HOUSEHOLD_ID` and restart" message.
- [ ] Completing the household/person step immediately unlocks the rest of the wizard in the same browser session, with no page reload or container restart required.
- [ ] Each entity step (assets, liabilities, income streams, obligations, goals) reuses its WAP-25 form and record-list components unmodified; "Next" is disabled until that step has at least one record.
- [ ] Finishing the snapshot step returns the user to the normal app (`AppNav` + Financial Position) showing the data just entered, with the new household ID usable on subsequent loads in that browser.
- [ ] An already-configured household with empty data can reach the same wizard via an explicit banner/link on Financial Position; the banner never blocks normal navigation to other pages.
- [ ] All loading/empty/error/validation states above are implemented; no wizard-specific code reimplements what `useEntrySubmit` or the underlying WAP-25 forms already do.
- [ ] No form in the wizard includes a provenance/source-type picker, matching WAP-25 (backend has exactly one value and does not accept it as input).

## Decision

- Selected direction: **Direction A — replaces the config-missing screen for first run; an explicit, dismissible banner on Financial Position for an already-configured empty household.**
- Amendments: None; approved as proposed.
- Approved by Product Owner Agent: **Ralph, directly with Claude Code standing in as Product Owner Agent (D020)**
- Approved at: **2026-09-12**

Changing status to `APPROVED` confirms the selection above.

## Implementation handoff

- Implemented by: Claude Code (Sonnet 5), directly with Ralph (D020)
- Automated checks and results: `./verify.sh` green (backend `./mvnw test` —
  all suites including Testcontainers integration tests; frontend `npm ci`,
  `npm run typecheck`, `npm test` — 119/119 passing including new
  `OnboardingWizard.test.tsx` covering the full 7-step fresh-household flow
  end to end and the 6-step resume-from-banner flow, `npm run build`).
  `npm run lint` (oxlint) clean. Two pre-existing Playwright e2e specs
  (`e2e/financial-position.spec.ts`, `e2e/evidence.spec.ts`) that asserted
  the old static "no household is configured" text were updated to assert
  the wizard's heading instead — e2e is not part of the `verify` CI gate but
  was fixed rather than left broken.
- Evidence: `frontend/src/components/OnboardingWizard.test.tsx` (jsdom,
  scripted through `<App/>`) is the primary evidence — a full step-by-step
  walkthrough asserting each step's gating, count feedback, and the final
  populated Financial position. No manual screenshots were captured this
  round (no new visual direction beyond reusing existing card/form/button
  conventions verbatim).
- Deviations from approved design:
  - **Per-step "already added" feedback is a count, not the itemized
    `AssetTable`/`LiabilityTable`/etc. lists the brief describes.** The
    brief's own "no skeleton needed — there's nothing to fetch, only submit"
    loading-state decision rules out re-fetching from the server to build
    those lists (which is how those tables normally get correctly-typed
    data), and the reused WAP-25 forms' `onCreated` callbacks are
    intentionally zero-argument (`() => void`) — reusing them **unmodified**
    (also required by the brief) means the wizard never sees the created
    record's data. A plain count ("2 assets added.") satisfies every
    acceptance criterion (visible progress feedback, "Next" gated on ≥1
    record) without fetching or touching the WAP-25 forms.
  - **AppNav is not hidden during the fresh-onboarding wizard.** The brief
    states "there's no household yet, so there's nothing for AppNav to
    navigate between," but Forecasting and Scenarios are stateless
    calculators already reachable with no household configured at all
    (pre-dating this ticket — see their own passing navigation tests,
    `ForecastingPage.test.tsx`/`ScenariosPage.test.tsx`). Hiding AppNav would
    have silently regressed that existing, tested behavior, so AppNav stays
    in every branch, matching every other config-status branch in `App.tsx`.
  - Household base currency and person role inputs are plain text (no
    reused select/enum), matching the backend contract discovered during
    implementation: `CreateHouseholdRequest.baseCurrency` is a 3-letter
    pattern-validated string and `CreatePersonRequest.role` is deliberately
    free text (PD-003), neither is a backend enum.
- Known limitations: refreshing the browser mid-wizard (after the household
  step but before finishing the entity steps) drops the user into the
  ordinary app with an empty-state Financial position, since the household
  id override is written as soon as the household is created — they resume
  the remaining steps via the "New here? Run guided setup" banner rather
  than mid-wizard. This is consistent with the brief's own resume design
  (banner-triggered, skips household/person), not a gap the brief called out
  explicitly.

Feature acceptance (met/unmet criteria, ACCEPTED/RETURNED, follow-up work) is
recorded as a comment on the Jira issue, not here — see
`agent/collaboration-workflow.md`.
