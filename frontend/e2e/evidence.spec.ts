import { expect, test } from '@playwright/test';
import {
  HOUSEHOLD_ID,
  emptyHouseholdFixture,
  mixedCurrencyFixture,
  mockConfig,
  mockFinancialPosition,
  zeroAndFutureDatedFixture,
} from './fixtures';

// Not a correctness suite — captures representative wide/narrow screenshots
// of the real implementation as durable evidence under
// agent/ui/financial-position/evidence/implementation/, per the Implement
// stage's requirement to capture wide/narrow UI evidence for UI work.
const OUT_DIR = '../agent/ui/financial-position/evidence/implementation';
const WIDE = { width: 1440, height: 900 };
const NARROW = { width: 390, height: 844 };

test('collapsed default — wide and narrow', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: mixedCurrencyFixture }));

  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/populated-collapsed-wide.png`, fullPage: true });

  await page.setViewportSize(NARROW);
  await page.screenshot({ path: `${OUT_DIR}/populated-collapsed-narrow.png`, fullPage: true });
});

test('expanded records — wide and narrow', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: mixedCurrencyFixture }));

  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  for (const currency of ['PHP', 'JPY', 'EUR']) {
    await page.getByText(currency, { exact: true }).locator('xpath=ancestor::section').getByRole('button').click();
  }
  await page.screenshot({ path: `${OUT_DIR}/populated-expanded-wide.png`, fullPage: true });

  await page.setViewportSize(NARROW);
  await page.screenshot({ path: `${OUT_DIR}/populated-expanded-narrow.png`, fullPage: true });
});

test('empty household — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: emptyHouseholdFixture }));
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByText(/no assets or liabilities are recorded/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/empty-wide.png`, fullPage: true });
});

test('first-load failure — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await page.route(`**/api/households/${HOUSEHOLD_ID}/financial-position`, (route) => route.abort('failed'));
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByText(/could not load financial position/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/error-first-load-wide.png`, fullPage: true });
});

test('refresh failure retained — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, (callIndex) =>
    callIndex === 0
      ? { status: 200, body: mixedCurrencyFixture }
      : { status: 500, body: { error: 'INTERNAL', message: 'boom', details: [] } }
  );
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  await page.getByRole('button', { name: /^refresh$/i }).click();
  await expect(page.getByRole('alert')).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/error-refresh-failed-wide.png`, fullPage: true });
});

test('missing configuration — wide', async ({ page }) => {
  await mockConfig(page, null);
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByText(/no household is configured/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/configuration-missing-wide.png`, fullPage: true });
});

test('invalid configuration — wide', async ({ page }) => {
  await mockConfig(page, 'not-a-uuid');
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByText(/configured household id is malformed/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/configuration-invalid-wide.png`, fullPage: true });
});

test('zero-valued and future-dated records — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: zeroAndFutureDatedFixture }));
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  await page.getByText('PHP', { exact: true }).locator('xpath=ancestor::section').getByRole('button').click();
  await page.screenshot({ path: `${OUT_DIR}/zero-and-future-dated-wide.png`, fullPage: true });
});

test('configured household not found — wide', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({
    status: 404,
    body: { error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] },
  }));
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByText(/no household was found for the configured id/i)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/configuration-not-found-wide.png`, fullPage: true });
});

test('keyboard focus — wide and narrow', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: mixedCurrencyFixture }));

  await page.setViewportSize(WIDE);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await page.screenshot({ path: `${OUT_DIR}/keyboard-focus-wide.png`, fullPage: true });

  await page.setViewportSize(NARROW);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await page.screenshot({ path: `${OUT_DIR}/keyboard-focus-narrow.png`, fullPage: true });
});

test('200% zoom reflow simulation — 640px viewport', async ({ page }) => {
  await mockConfig(page, HOUSEHOLD_ID);
  await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: mixedCurrencyFixture }));
  await page.setViewportSize({ width: 640, height: 900 });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
  for (const currency of ['PHP', 'JPY', 'EUR']) {
    await page.getByText(currency, { exact: true }).locator('xpath=ancestor::section').getByRole('button').click();
  }
  await page.screenshot({ path: `${OUT_DIR}/reflow-200pct-640.png`, fullPage: true });
});
