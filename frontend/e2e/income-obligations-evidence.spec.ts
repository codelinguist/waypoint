import { expect, test } from '@playwright/test';
import {
  HOUSEHOLD_ID,
  incomeStreamsFixture,
  mockConfig,
  mockIncomeStreams,
  mockObligations,
  obligationsFixture,
} from './fixtures';

// Not a correctness suite — captures representative wide/narrow screenshots
// of the real implementation as durable evidence under
// agent/ui/income-obligations/evidence/implementation/, per the Implement
// stage's requirement to capture wide/narrow UI evidence for UI work.
const OUT_DIR = '../agent/ui/income-obligations/evidence/implementation';
const WIDE = { width: 1440, height: 900 };
const NARROW = { width: 390, height: 844 };

async function gotoIncomeObligationsTab(page: import('@playwright/test').Page) {
  await page.goto('/');
  await page.getByRole('tab', { name: /income & obligations/i }).click();
}

test('populated income streams and obligations — wide and narrow', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockIncomeStreams(page, HOUSEHOLD_ID, () => ({ status: 200, body: incomeStreamsFixture }));
  await mockObligations(page, HOUSEHOLD_ID, () => ({ status: 200, body: obligationsFixture }));

  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByRole('heading', { name: /income & obligations/i })).toBeVisible();
  await expect(page.getByText('New Job Salary')).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/populated-wide.png`, fullPage: true });

  await page.setViewportSize(NARROW);
  await page.screenshot({ path: `${OUT_DIR}/populated-narrow.png`, fullPage: true });
});

test('empty household — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockIncomeStreams(page, HOUSEHOLD_ID, () => ({ status: 200, body: [] }));
  await mockObligations(page, HOUSEHOLD_ID, () => ({ status: 200, body: [] }));
  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByText(/no income streams or obligations are recorded/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/empty-wide.png`, fullPage: true });
});

test('first-load failure — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await page.route(`**/api/households/${HOUSEHOLD_ID}/income-streams`, (route) => route.abort('failed'));
  await page.route(`**/api/households/${HOUSEHOLD_ID}/obligations`, (route) => route.abort('failed'));
  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByText(/could not load income streams and obligations/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/error-first-load-wide.png`, fullPage: true });
});

test('refresh failure retained — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockIncomeStreams(page, HOUSEHOLD_ID, (callIndex) =>
    callIndex === 0
      ? { status: 200, body: incomeStreamsFixture }
      : { status: 500, body: { error: 'INTERNAL', message: 'boom', details: [] } }
  );
  await mockObligations(page, HOUSEHOLD_ID, () => ({ status: 200, body: obligationsFixture }));
  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByText('New Job Salary')).toBeVisible();
  await page.getByRole('button', { name: /^refresh$/i }).click();
  await expect(page.getByRole('alert')).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/error-refresh-failed-wide.png`, fullPage: true });
});

test('configured household not found — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  const notFound = () => ({ status: 404, body: { error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] } });
  await mockIncomeStreams(page, HOUSEHOLD_ID, notFound);
  await mockObligations(page, HOUSEHOLD_ID, notFound);
  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByText(/no household was found for the configured id/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/configuration-not-found-wide.png`, fullPage: true });
});

test('keyboard focus on nav tabs — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockIncomeStreams(page, HOUSEHOLD_ID, () => ({ status: 200, body: incomeStreamsFixture }));
  await mockObligations(page, HOUSEHOLD_ID, () => ({ status: 200, body: obligationsFixture }));
  await page.setViewportSize(WIDE);
  await gotoIncomeObligationsTab(page);
  await expect(page.getByRole('heading', { name: /income & obligations/i })).toBeVisible();
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await page.screenshot({ path: `${OUT_DIR}/keyboard-focus-nav-wide.png`, fullPage: true });
});
