# Implementation Log: Financial Position Design Exploration (Task 020 / WAP-13)

Feature-local implementation record, per this task's shared-prose exception
(no edits to the central `agent/implementation-log.md`).

## Changed

- Added `agent/ui/financial-position/design-brief.md` (status `DRAFT`) using
  `agent/templates/ui-design-brief.md`, exploring three materially different
  directions against the API contract in the
  [current-financial-position product brief](../current-financial-position/product-brief.md):
  - Direction A — currency-first summary cards, expandable holdings
    (recommended).
  - Direction B — inventory-first split view (persistent two-column
    assets/liabilities with a top chip summary).
  - Direction C — unified reconciling ledger (single grouped/subtotaled
    table).
- Added `agent/ui/financial-position/evidence/`: a shared `shared.css`, five
  static HTML states for Direction A (`populated`, `loading`, `empty`,
  `error`, `configuration`), one populated-state HTML file each for
  Directions B and C, and 14 PNG screenshots
  (`screenshots/direction-{a,b,c}-*-{wide,narrow}.png`) rendered at 1440px
  and 390px with headless Chromium (Playwright 1.63.0).
- No application code, dependency, migration, or household data was added or
  changed. This task owns only `agent/product/financial-position-design/**`
  and `agent/ui/financial-position/**`.

## Synthetic evidence dataset

One consistent dataset is used across all three directions' populated-state
evidence, chosen to exercise the parent brief's required distinctions in a
single screenshot set:

- **PHP** (negative net worth, `−4,935,000.00`): checking account, a
  long-named real-estate lot (`Urban Residential Lot — Riverside Heights
  Subdivision Phase 2 Block 14 Lot 9`, long-name wrap case), and an illiquid
  equity holding with `planningValue 0.00` against `estimatedValue
  500,000.00` (conservative-valuation / planning-vs-estimated divergence
  case, principle 9); credit card, mortgage, and business loan liabilities.
- **USD** (positive net worth, `+4,500.50`): one brokerage asset, one
  personal-loan liability.
- **JPY** (assets-only currency group, extreme-magnitude case): a
  condominium plus a synthetic "Precision Reserve Test Holding" of
  `99,999,999,999,999,999.99`, giving a JPY net worth of
  `100,000,000,041,999,999.99` — exercises the read model's exact-decimal
  transport at a scale that would lose precision through `Number`, and has
  no liability rows (assets-only currency group).

## Verification

- `./verify.sh` — run from the repository root; `BUILD SUCCESS`, `Tests run:
  574, Failures: 0, Errors: 0, Skipped: 0` (unchanged from `main`, since this
  task changed no application code, build file, or migration).
- Manual: rendered all 7 HTML evidence files at 1440px and 390px with a
  disposable local Playwright/Chromium install (not a repository
  dependency — installed only into a scratch npm project outside the
  repository, nothing added to this repo's dependency tree) and asserted
  `document.documentElement.scrollWidth` equals the viewport width for each
  file, confirming no page-level horizontal overflow at either width.

## Decisions

- **Refresh-failure retention: keep and label, don't clear.** The parent
  brief requires the design to explicitly choose between clearing results on
  a failed refresh or retaining and labeling the last successful complete
  result. Chose retain-and-label (`evidence/direction-a-currency-cards/error.html`):
  a failed refresh dims the existing cards, adds a `role="alert"` banner
  naming the failure time and stating the figures are not current, and
  offers a retry action. Rationale: clearing the screen on a transient
  network failure would either show nothing (worse for a page whose whole
  purpose is showing current recorded position) or risk being misread as
  "zero wealth" during the gap — both are worse than a clearly labeled stale
  view, and matches the parent brief's "a failure cannot masquerade as zero
  wealth" acceptance criterion directly.
- **Recommend Direction A.** See the design brief's "Proposed direction"
  section for the full reasoning; in short, only Direction A's default
  (collapsed) view already matches the parent brief's stated flow — totals
  by currency first, source rows second — without an extra disconnected
  summary bar (Direction B) or a buried subtotal row (Direction C), and it
  degrades most cleanly to 390px.
- **Screenshot tooling is scratch-only, not a new repository dependency.**
  Playwright/Chromium were installed into a throwaway npm project under the
  session's scratch directory (outside the repository) purely to render and
  measure the static evidence HTML; nothing was added to any
  `package.json`/lockfile in this repository, and no `frontend/` directory
  exists yet. The actual frontend implementation task chooses its own
  testing/screenshot tooling independently.

## Fix found during evidence rendering

While rendering Direction A's populated state at 390px, an automated
`scrollWidth` check (not a visual read of a downscaled screenshot preview,
which was ambiguous) caught genuine page-level horizontal overflow
(`scrollWidth 411` vs. a 390px viewport): the JPY card's net-worth headline,
`100,000,000,041,999,999.99 JPY`, is a single long token that could not wrap
within itself and pushed its card (and therefore the page) wider than the
viewport, even though every data table was already correctly contained in
its own `overflow-x: auto` box and did not leak. Fixed by adding
`overflow-wrap: anywhere` to the `.net-worth` headline style. Re-verified
`scrollWidth === 390` for all 7 evidence files after the fix, then
re-rendered all 14 screenshots. Recorded here because it is a concrete,
general lesson for the eventual implementation task: a large exact-decimal
total rendered as a large, bold, space-free headline needs explicit wrap
handling at narrow widths — it will not be caught by checking a downscaled
screenshot image by eye, only by measuring `document.documentElement
.scrollWidth` against the viewport width directly.

## Assumptions

- Synthetic household name ("Sample Household (Design Evidence)"), IDs, and
  all record values are fabricated for this evidence only; none reference or
  resemble any real household data (per this task's "use synthetic evidence
  only" constraint).
- Designed strictly against the current-financial-position brief's
  documented contract fields, not against any implemented code, since Task
  021/WAP-14 is explicitly independent of this task and may not exist yet.
- No household preference or financial decision was required — layout
  selection is a reversible Product Owner decision per the design-only
  product brief, not an undiscoverable household preference.

## Open questions

None blocking. Product Owner selection among the three explored directions
is the only remaining step before this brief can move to `APPROVED`.

## Recommended next task

None from this task directly — the parent brief's delivery handoff already
queues the React/TypeScript implementation (outside the executable queue
until this design and WAP-14 are both accepted/merged) from
`agent/product/financial-position/implementation-task-draft.md`.

## System-evolution candidate

Not proposing an `AGENTS.md` or template change from this task. Worth
surfacing informally: the `scrollWidth`-vs-viewport check used here to catch
the narrow-width overflow bug (see "Fix found during evidence rendering")
is a reusable pattern for any future design-evidence task producing static
HTML prototypes — cheaper and more reliable than eyeballing a downscaled
screenshot — but codifying it as a required step felt premature from a
single occurrence.
