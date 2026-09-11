import { expect, test } from '@playwright/test';
import { formatSignedMoney } from '../src/money';

// Real-backend counterpart to financial-position.spec.ts (which mocks the
// API): renders the actual React dashboard against the real
// GET /api/households/{id}/financial-position endpoint, through the real
// nginx-to-Spring path, for the synthetic household
// e2e/real-backend-smoke.sh just seeded via the real REST API. Not runnable
// standalone — see README.md "Frontend E2E tests" and
// playwright.smoke.config.ts.
const HOUSEHOLD_ID = process.env.SMOKE_HOUSEHOLD_ID;
const HOUSEHOLD_NAME = process.env.SMOKE_HOUSEHOLD_NAME;

if (!HOUSEHOLD_ID || !HOUSEHOLD_NAME) {
  throw new Error(
    'real-backend-smoke.spec.ts requires SMOKE_HOUSEHOLD_ID and SMOKE_HOUSEHOLD_NAME (set by e2e/real-backend-smoke.sh)'
  );
}

test('renders the seeded household through the real nginx-to-Spring path, and a refresh reconfirms it', async ({
  page,
  request,
}) => {
  // Read the same real endpoint the page will call, to assert against the
  // real response's own totals rather than duplicating expected values.
  const response = await request.get(`/api/households/${HOUSEHOLD_ID}/financial-position`);
  expect(response.ok()).toBe(true);
  const body = (await response.json()) as { totalsByCurrency: { currency: string; netWorth: string }[] };
  expect(body.totalsByCurrency.length).toBeGreaterThan(0);

  await page.goto('/');
  await expect(page.getByRole('heading', { name: HOUSEHOLD_NAME })).toBeVisible();

  for (const totals of body.totalsByCurrency) {
    const { formatted } = formatSignedMoney(totals.netWorth);
    await expect(page.getByText(`${formatted} ${totals.currency}`)).toBeVisible();
  }

  await page.getByRole('button', { name: /^refresh$/i }).click();
  await expect(page.getByRole('heading', { name: HOUSEHOLD_NAME })).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);
});
