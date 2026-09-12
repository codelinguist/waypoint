import { expect, test, type Page } from '@playwright/test';

// Real-backend counterpart to the WAP-25 data-entry unit/component tests
// (which mock fetch): renders the actual React app against the real
// household e2e/real-backend-data-entry-smoke.sh just seeded (deliberately
// with NO assets/liabilities/etc. — this flow's whole point is creating
// them through the UI), through the real nginx-to-Spring path. Not runnable
// standalone — see README.md "Frontend E2E tests" and
// playwright.data-entry-smoke.config.ts.
const HOUSEHOLD_ID = process.env.SMOKE_HOUSEHOLD_ID;
const HOUSEHOLD_NAME = process.env.SMOKE_HOUSEHOLD_NAME;

if (!HOUSEHOLD_ID || !HOUSEHOLD_NAME) {
  throw new Error(
    'data-entry-smoke.spec.ts requires SMOKE_HOUSEHOLD_ID and SMOKE_HOUSEHOLD_NAME (set by e2e/real-backend-data-entry-smoke.sh)'
  );
}

async function goToNav(page: Page, name: string) {
  await page.getByRole('button', { name, exact: true }).click();
}

test('creating and correcting every WAP-25 record type through the real UI reflects on the real read-only views', async ({
  page,
  request,
}) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: HOUSEHOLD_NAME })).toBeVisible();
  await expect(page.getByText(/no assets or liabilities are recorded/i)).toBeVisible();

  // --- Asset ---
  await page.getByRole('button', { name: 'Add asset' }).click();
  await page.getByLabel('Name').fill('Smoke Checking Account');
  await page.getByLabel('Currency').fill('PHP');
  await page.getByLabel('Estimated value').fill('185000.00');
  await page.getByLabel('Planning value').fill('185000.00');
  await page.getByLabel('Valued as of').fill('2026-09-08');
  await page.getByRole('button', { name: 'Add asset' }).click();
  // The currency card starts collapsed by design (approved financial-position
  // brief) — the new record exists but isn't visible until expanded.
  await expect(page.getByText('Smoke Checking Account')).toBeAttached();

  // --- Liability ---
  await page.getByRole('button', { name: 'Add liability' }).click();
  await page.getByLabel('Name').fill('Smoke Credit Card');
  await page.getByLabel('Currency').fill('PHP');
  await page.getByLabel('Outstanding balance').fill('8000.00');
  await page.getByLabel('Balance as of').fill('2026-09-02');
  await page.getByRole('button', { name: 'Add liability' }).click();
  await expect(page.getByText('Smoke Credit Card')).toBeAttached();

  // Expand the (single, PHP) currency card to inspect both new records.
  await page.getByRole('button', { name: /records$/ }).first().click();
  await expect(page.getByText('Smoke Checking Account')).toBeVisible();
  await expect(page.getByText('Smoke Credit Card')).toBeVisible();

  await page.screenshot({ path: 'e2e-evidence/data-entry-position-populated-wide.png', fullPage: true });

  // --- Asset correction (D021/D022) ---
  await page.getByRole('button', { name: 'Correct' }).first().click();
  await expect(page.getByLabel('Estimated value')).toHaveValue('185000.00');
  await page.getByLabel('Estimated value').fill('190000.00');
  await page.getByLabel('Reason for correction').fill('Smoke test correction');
  await page.getByRole('button', { name: 'Save correction' }).click();
  await expect(page.getByText('190,000.00')).toBeVisible();

  const historyResponse = await request.get(`/api/households/${HOUSEHOLD_ID}/assets`);
  const assets = (await historyResponse.json()) as { id: string; name: string }[];
  const correctedAssetId = assets.find((asset) => asset.name === 'Smoke Checking Account')!.id;
  const historyBody = (await (
    await request.get(`/api/households/${HOUSEHOLD_ID}/assets/${correctedAssetId}/valuations/history`)
  ).json()) as { previousEstimatedValue: string; newEstimatedValue: string }[];
  // D021/D022: the correction is an append-only audit, not an overwrite — the
  // prior value must still be recoverable, alongside the new one.
  expect(historyBody.some((entry) => entry.previousEstimatedValue === '185000.00')).toBe(true);
  expect(historyBody.some((entry) => entry.newEstimatedValue === '190000.00')).toBe(true);

  // --- Liability correction (D021/D022) --- the liability row's own "Correct" action.
  // Unlike the asset valuation state (formatted server-side to two decimals),
  // LiabilityResponse.outstandingBalance is a plain JSON number, so a whole
  // value round-trips as "8000", not "8000.00" — expected, not a bug.
  await page.getByRole('button', { name: 'Correct' }).last().click();
  await expect(page.getByLabel('Outstanding balance')).toHaveValue('8000');
  await page.getByLabel('Outstanding balance').fill('7500.00');
  await page.getByLabel('Reason for correction').fill('Smoke test liability correction');
  await page.getByRole('button', { name: 'Save correction' }).click();
  // Scoped to the liability's own table cell: the currency card's net-worth
  // headline and per-currency totals row can coincidentally show the same
  // formatted figure.
  await expect(page.getByRole('cell', { name: '7,500.00' })).toBeVisible();

  const liabilitiesResponse = await request.get(`/api/households/${HOUSEHOLD_ID}/liabilities`);
  const liabilities = (await liabilitiesResponse.json()) as { id: string; name: string }[];
  const correctedLiabilityId = liabilities.find((liability) => liability.name === 'Smoke Credit Card')!.id;
  const liabilityHistoryBody = (await (
    await request.get(`/api/households/${HOUSEHOLD_ID}/liabilities/${correctedLiabilityId}/balances`)
  ).json()) as { previousBalance: string; newBalance: string }[];
  expect(liabilityHistoryBody.some((entry) => entry.previousBalance === '8000.00')).toBe(true);
  expect(liabilityHistoryBody.some((entry) => entry.newBalance === '7500.00')).toBe(true);

  // --- Income & obligations ---
  await goToNav(page, 'Income & obligations');
  await page.getByRole('button', { name: 'Add income stream' }).click();
  await page.getByLabel('Name').fill('Smoke Salary');
  await page.getByLabel('Currency').fill('PHP');
  await page.getByLabel('Amount').fill('50000.00');
  await page.getByLabel('Start date').fill('2026-01-01');
  await page.getByRole('button', { name: 'Add income stream' }).click();
  await expect(page.getByText('Smoke Salary')).toBeVisible();

  await page.getByRole('button', { name: 'Add obligation' }).click();
  await page.getByLabel('Name').fill('Smoke Rent');
  await page.getByLabel('Currency').fill('PHP');
  await page.getByLabel('Amount').fill('15000.00');
  await page.getByLabel('Start date').fill('2026-01-01');
  await page.getByRole('button', { name: 'Add obligation' }).click();
  await expect(page.getByText('Smoke Rent')).toBeVisible();

  // --- Goals ---
  // Scoped to the Add-goal panel itself: the page's own contribution
  // calculator (always rendered, not a disclosure) shares field labels
  // (Currency, Target amount, Current amount).
  await goToNav(page, 'Goals');
  await page.getByRole('button', { name: 'Add goal' }).click();
  const goalPanelId = await page.getByRole('button', { name: 'Cancel' }).getAttribute('aria-controls');
  const goalForm = page.locator(`#${goalPanelId}`);
  await goalForm.getByLabel('Name').fill('Smoke Emergency Fund');
  await goalForm.getByLabel('Currency').fill('PHP');
  await goalForm.getByLabel('Target amount').fill('300000.00');
  await goalForm.getByLabel('Current amount').fill('0.00');
  await goalForm.getByLabel('Target date').fill('2027-01-01');
  await goalForm.getByLabel('Priority').fill('1');
  await goalForm.getByRole('button', { name: 'Add goal' }).click();
  // Cell, not option: the contribution calculator's "Prefill from goal"
  // select now also has an option with this same text.
  await expect(page.getByRole('cell', { name: 'Smoke Emergency Fund' })).toBeVisible();

  // --- Snapshot ---
  await goToNav(page, 'Snapshots');
  await page.getByRole('button', { name: 'Create snapshot' }).click();
  await page.getByLabel('As of date').fill('2026-09-10');
  await page.getByRole('button', { name: 'Create snapshot' }).click();
  // Scoped to the snapshot list: the comparison view's snapshot pickers also
  // have options with this same date text.
  await expect(page.getByRole('region', { name: /financial snapshots/i }).getByText('Sep 10, 2026')).toBeVisible();

  // --- Planning assumptions: create, then supersede ---
  await goToNav(page, 'Planning assumptions');
  await page.getByRole('button', { name: 'Add assumption' }).click();
  await page.getByLabel('Name').fill('Smoke Inflation Rate');
  await page.getByLabel('Value type').fill('percentage');
  await page.getByLabel('Value', { exact: true }).fill('3.5');
  await page.getByLabel('Effective from').fill('2026-01-01');
  await page.getByLabel('Review date').fill('2027-01-01');
  await page.getByRole('button', { name: 'Add assumption' }).click();
  await expect(page.getByRole('cell', { name: 'Smoke Inflation Rate' })).toBeVisible();
  await expect(page.getByText('Current')).toBeVisible();

  await page.getByRole('button', { name: 'Supersede' }).click();
  await page.getByLabel('Value', { exact: true }).fill('4.0');
  await page.getByRole('button', { name: 'Save as new version' }).click();
  await expect(page.getByText('Superseded')).toBeVisible();

  await page.screenshot({ path: 'e2e-evidence/data-entry-assumptions-wide.png', fullPage: true });
});
