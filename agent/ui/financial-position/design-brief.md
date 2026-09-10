# UI Design Brief: Financial Position

## Status

`DRAFT`

## Task and user outcome

- Product Owner Agent: Codex
- Priority: First usable frontend slice (Phase 9 early slice, D015)
- Current task: WAP-13 (design exploration and approval); parent outcome owned by the [financial-position product brief](../../product/financial-position/product-brief.md)
- User problem: household records and calculations exist behind APIs, but the household cannot inspect what it owns, what it owes, and net worth in an everyday interface
- Primary user: Ralph and his wife, viewing one already-configured household
- User outcome: open a private, configured household page; understand recorded financial position by currency; inspect the records and dates behind each total
- Success measure: every displayed total reconciles to its displayed source rows, currencies stay separate (no FX aggregate), and empty/error/stale-date semantics are understandable on desktop and mobile
- Out of scope: record creation/editing, household creation/selection, authentication, charts/history, goals, cash flow, scenarios, AI chat, FX conversion, and any production code — this task produces design evidence only (per WAP-13's Definition of Done)

## Source context

- Product requirements: [financial-position product brief](../../product/financial-position/product-brief.md) (parent outcome and acceptance criteria), [financial-position-design product brief](../../product/financial-position-design/product-brief.md) (this task's own scope)
- Domain constraints: `docs/domain/financial-model.md` (Asset/Liability provenance, liquidity, planning vs. estimated value), `docs/product/principles.md` (explicit uncertainty, conservative planning — principles 3 and 9)
- Existing decisions: D002 (LLM/browser never do the arithmetic — server totals only), D015 (this slice's scope and sequencing)
- API contract (from the [current-financial-position product brief](../../product/current-financial-position/product-brief.md); Task 021/WAP-14 implements it independently — this design does not wait for or depend on its code):
  - `GET /api/households/{householdId}/financial-position` → `{ householdId, householdName, baseCurrency, retrievedAt, assets[], liabilities[], totalsByCurrency[] }`
  - `assets[]`: `id, name, assetType, estimatedValue, planningValue, currency, valuedAt, liquidity, sourceType`
  - `liabilities[]`: `id, name, liabilityType, outstandingBalance, currency, balanceAsOf, sourceType`
  - `totalsByCurrency[]`: `currency, assetTotal, liabilityTotal, netWorth` — one entry per currency present in either list, ordered by currency; `netWorth = assetTotal - liabilityTotal`; all monetary fields are exact decimal strings, never JavaScript `Number`
- Reference images or products: none; no existing frontend or design system in this repository yet (this is the first UI feature)

## Information and actions

- Information the user must understand: household name; net worth, assets total and liabilities total per currency (currencies never combined); for each record — name, type, currency, source date, liquidity (assets) or nothing implying liquidity (liabilities), and whether it came from manual entry or import; that `planningValue` (used in every total) can differ from `estimatedValue`, and why (conservative treatment of illiquid/uncertain holdings, principle 9); that `retrievedAt` is when the page fetched data, not a valuation date for every row (rows carry their own `valuedAt`/`balanceAsOf`)
- Primary action: understand current position (headline totals) by currency
- Secondary actions: inspect the source rows behind a total; refresh
- Financial classifications that must remain explicit: planning value vs. estimated value (never silently substituted, principle 3/9); liquidity (must not imply all wealth is spendable cash, per the parent brief); asset vs. liability (never merged into one undifferentiated "record"); per-record source date vs. page retrieval time; manual-entry vs. imported provenance

## Directions explored

### Direction A — Currency-first summary cards, expandable holdings

- Structure: one card per currency, each headlined by that currency's net worth (assets total / liabilities total shown beneath), with an expand/collapse control that reveals two source-row tables (assets, liabilities) inside the card. Progressive disclosure: the headline is the default view; records are one action away.
- Interaction: a labeled toggle button (`aria-expanded`/`aria-controls`) per card shows/hides its record tables; each table lives in its own horizontally-scrollable container so a wide table never forces page-level horizontal scroll.
- Strengths: directly matches the parent brief's stated flow ("sees recorded totals grouped by currency" first, "inspects what contributes to each total" second); each total sits immediately beside the rows that produce it, so reconciliation needs no cross-referencing; degrades cleanly to narrow width (cards already stack; collapsed-by-default cards keep the initial screen short on mobile); a household with many currencies scales by adding cards, not by widening anything.
- Tradeoffs: a household that wants to see every record at once must expand every card; on first load, only headline totals are visible without the extra click/tap.

### Direction B — Inventory-first split view

- Structure: two persistent columns, Assets and Liabilities, each internally grouped by currency; every record is always visible as a compact list row. A single summary bar of per-currency net-worth "chips" sits above the columns.
- Interaction: no expand/collapse — everything is always rendered; the chip row scrolls horizontally if it doesn't fit.
- Strengths: nothing is ever hidden behind an interaction; a household that wants to audit every record at a glance can, without clicking anything.
- Tradeoffs: totals live only in the chip row, disconnected from the rows that produce them — matching a chip back to "why is this number what it is" means scanning two separate, currency-grouped lists rather than reading one adjacent block; below ~720px the two columns must stack (Assets, then Liabilities), turning the page into two long lists with the chip summary now far above the liabilities the user is currently scrolled past; weakest fit for the parent brief's "totals first, sources second" flow, since sources are already fully unfolded.

### Direction C — Unified reconciling ledger

- Structure: a single dense table containing every asset and liability row together, grouped by currency with a subtotal (net-worth) row closing each currency group; a `Kind` column (Asset/Liability, icon + label, not color alone) distinguishes row types; toolbar filters for currency and kind.
- Interaction: filter selects narrow the visible rows; the whole table sits in one horizontally-scrollable container.
- Strengths: most information-dense single view; appeals to a systems-thinking, spreadsheet-comfortable user (matches Ralph's stated preference in `docs/product/user-zero.md`); filtering is a natural fit for a household with many records.
- Tradeoffs: the headline "what's my net worth right now" answer is buried inside the table (a subtotal row reached only after scanning past every row in that currency group) rather than presented immediately; weakest narrow-width story of the three — a genuinely tabular structure with six columns has no good stacked-card fallback without abandoning the structure the direction is built around, so it stays a horizontally-scrolling table even at 390px, which is more friction than Directions A or B for the mobile-first ordinary case.

## Proposed direction

- Recommendation: **Direction A — currency-first summary cards, expandable holdings.**
- Reasoning: it is the only direction whose default (unexpanded) view already matches the parent brief's stated two-step flow — see totals by currency, then inspect sources — without requiring the user to scan across a disconnected summary bar (Direction B) or hunt through a dense table for the subtotal row (Direction C). It also has the cleanest narrow-width behavior of the three: cards were already a stacking layout before any responsive work is added, and starting each card collapsed keeps the first mobile screen to three short headlines instead of six-plus columns of detail. Direction C's density is real but optimizes for a power-user audit action this task's parent brief does not ask for (browsing/filtering every record at once); that is better served as a later, explicit power view than as the default first screen.
- Wide layout: see `evidence/direction-a-currency-cards/populated.html` and `evidence/screenshots/direction-a-populated-wide.png` (1440px). One card per currency stacked vertically; PHP card shown expanded (records visible), USD and JPY collapsed, to demonstrate both states in one screenshot.
- Narrow layout: `evidence/screenshots/direction-a-populated-narrow.png` (390px). Same structure; each record table lives in its own `overflow-x: auto` container so only the table scrolls horizontally, never the page (verified: `document.documentElement.scrollWidth === 390` at a 390px viewport for every evidence file — see Decision below on the one bug this caught).
- Loading state: `evidence/direction-a-currency-cards/loading.html` / `screenshots/direction-a-loading-wide.png`. Skeleton cards, `aria-hidden`; the status line reads "Loading financial position…" via `role="status" aria-live="polite"` for screen readers. No number, zero or blank total is ever rendered before real data arrives.
- Empty state: `evidence/direction-a-currency-cards/empty.html` / `screenshots/direction-a-empty-wide.png`. A valid, reachable household with zero recorded rows gets an explicit "no assets or liabilities are recorded yet" message — never a fabricated zero-currency group and never treated as an error.
- Validation and error states: `evidence/direction-a-currency-cards/error.html` / `screenshots/direction-a-error-wide.png` demonstrates the **refresh-failure retention decision** this design makes explicitly (see Decision below): a failed refresh keeps and visibly labels the last successful complete result (dimmed cards, `role="alert"` banner naming the failure time and stating the data is not current) rather than clearing the screen or silently mixing old rows with a partial new response. `evidence/direction-a-currency-cards/configuration.html` / `screenshots/direction-a-configuration-wide.png` covers missing/invalid `HOUSEHOLD_ID` configuration with an actionable, non-card message shown before any household name is known; the same pattern covers an explicitly configured but unknown (404) household, naming the configured ID instead of the generic message shown here.
- Accessibility considerations: every amount uses `font-variant-numeric: tabular-nums`; negative/positive net worth is marked with both an explicit `−`/`+` glyph and screen-reader-only text ("Net worth: negative/positive"), not color alone; expand/collapse uses real `<button>` elements with `aria-expanded`/`aria-controls`, keyboard-operable with no custom key handling needed; tables use `<caption>` (visually hidden where the card heading already states the same context), `<th scope="col">`, and `<time datetime="...">` for dates; the status/refresh-failure line is an `aria-live` region so screen-reader users hear load/refresh outcomes without navigating to find them; visible focus rings (`:focus-visible`, 3px, not removed) on every interactive element; no fixed pixel width on any container that would break 200% browser zoom — the evidence's own responsive card layout is the only layout mechanism, not a mobile-specific alternate template.

## Acceptance criteria

- [x] At least two distinct directions have reviewable wide/narrow evidence and articulated tradeoffs against the parent outcome — three directions, `evidence/screenshots/direction-{a,b,c}-populated-{wide,narrow}.png`.
- [x] All information, states and financial distinctions in the parent brief appear in the design specification, including large exact amounts (JPY `100,000,000,041,999,999.99` net worth, exercising the read model's exact-decimal transport at extreme scale), multiple currencies (PHP/USD/JPY, including an assets-only JPY group), negative net worth (PHP `−4,935,000.00`), and long names (the Riverside Heights lot).
- [x] Interaction and accessibility behavior is concrete enough for a fresh implementation conversation; error/refresh retention behavior is chosen explicitly (see Decision below and `evidence/direction-a-currency-cards/error.html`).
- [ ] Product Owner records the chosen direction, rationale, amendments and APPROVED status in this brief. **Pending — Codex has not yet reviewed this brief.**
- [x] No production UI or backend code, dependencies or household data changes are introduced — only static HTML/CSS evidence files and this markdown brief.
- [x] Design PR and brief contain evidence and canonical `./verify.sh` results (see `implementation-log.md`); design-task acceptance is distinct from final UI acceptance and is recorded on the Jira issue, not claimed here.

## Decision

- Selected direction: *(pending Product Owner review)*
- Amendments: *(pending)*
- Approved by Product Owner Agent: *(pending)*
- Approved at: *(pending)*

Changing status to `APPROVED` confirms the selection above. Claude Code does not self-approve this brief; per `CLAUDE.md`, a draft design only becomes approved directly between the user and Codex.

## Implementation handoff

- Implemented by: *(not yet — implementation is a later, separately queued task per the parent brief's delivery handoff, after this design and WAP-14/Task 021 are both accepted and merged)*
- Automated checks and results: see `implementation-log.md` for this task's own `./verify.sh` run (no application code changed by this task)
- Evidence: `evidence/direction-a-currency-cards/*.html` (5 states), `evidence/direction-b-split-view/populated.html`, `evidence/direction-c-unified-ledger/populated.html`, `evidence/shared.css`, `evidence/screenshots/*.png` (14 screenshots, 1440px and 390px)
- Deviations from approved design: none yet (not yet approved)
- Known limitations: evidence is static/disposable HTML, not a frontend scaffold — no framework, build step, or reusable components are implied by these files; the implementation task chooses its own component structure. Synthetic data only; no real household, asset, or liability records were read or written.

Feature acceptance (met/unmet criteria, ACCEPTED/RETURNED, follow-up work) is
recorded as a comment on the Jira issue, not here — see
`agent/collaboration-workflow.md`.
