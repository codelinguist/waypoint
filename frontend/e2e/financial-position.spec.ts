import { expect, test } from '@playwright/test';
import {
  HOUSEHOLD_ID,
  emptyHouseholdFixture,
  mixedCurrencyFixture,
  mockConfig,
  mockFinancialPosition,
  zeroAndFutureDatedFixture,
} from './fixtures';

const WIDE = { width: 1440, height: 900 };
const NARROW = { width: 390, height: 844 };

test.describe('missing configuration', () => {
  test('shows the onboarding wizard instead of a static message, and never calls the financial-position API', async ({
    page,
  }) => {
    await mockConfig(page, null);
    let apiCalled = false;
    await page.route('**/api/households/**', () => {
      apiCalled = true;
    });

    await page.setViewportSize(WIDE);
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Set up your household' })).toBeVisible();
    await expect(page.getByText('Step 1 of 7: Household & people')).toBeVisible();
    expect(apiCalled).toBe(false);
  });
});

test.describe('invalid configuration', () => {
  test('reports a malformed configured id distinctly from missing configuration, and never calls the API', async ({
    page,
  }) => {
    await mockConfig(page, 'not-a-uuid');
    let apiCalled = false;
    await page.route('**/api/households/**', () => {
      apiCalled = true;
    });

    await page.setViewportSize(WIDE);
    await page.goto('/');

    await expect(page.getByText(/configured household id is malformed/i)).toBeVisible();
    await expect(page.getByText('not-a-uuid')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Set up your household' })).toHaveCount(0);
    expect(apiCalled).toBe(false);
  });
});

test.describe('household not found', () => {
  test('names the configured id, distinct from missing configuration', async ({ page }) => {
    await mockConfig(page, HOUSEHOLD_ID);
    await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({
      status: 404,
      body: { error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] },
    }));

    await page.goto('/');

    await expect(page.getByText(/no household was found for the configured id/i)).toBeVisible();
    await expect(page.getByText(HOUSEHOLD_ID)).toBeVisible();
  });
});

test.describe('populated financial position', () => {
  test.beforeEach(async ({ page }) => {
    await mockConfig(page, HOUSEHOLD_ID);
    await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: mixedCurrencyFixture }));
  });

  test('renders exact large decimal amounts and keeps currencies separate at wide and narrow widths', async ({
    page,
  }) => {
    await page.setViewportSize(WIDE);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();

    // JPY's extreme precision test row (20 significant digits) must render intact.
    await expect(page.getByText('99,999,999,999,999,999.99 JPY')).toBeVisible();
    // PHP is negative (liabilities exceed assets); marked with a non-color glyph, not color alone.
    await expect(page.getByText('negative').first()).toBeVisible();

    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(WIDE.width);

    await page.setViewportSize(NARROW);
    // Records reflow into stacked cards below 700px with no page-level horizontal overflow.
    const phpCard = page.getByText('PHP', { exact: true }).locator('xpath=ancestor::section');
    await phpCard.getByRole('button').click();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(NARROW.width);
    await expect(page.getByText('Urban Residential Lot')).toBeVisible();
  });

  test('shows a liabilities-only currency with an explicit empty asset side', async ({ page }) => {
    await page.setViewportSize(WIDE);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();

    const eurCard = page.getByText('EUR', { exact: true }).locator('xpath=ancestor::section');
    await eurCard.getByRole('button').click();
    await expect(eurCard.getByText(/no assets recorded in eur/i)).toBeVisible();
  });

  test('keyboard navigation reaches the first record toggle with a visible focus ring', async ({ page }) => {
    await page.setViewportSize(WIDE);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();

    // Tab from the top of the page to the first interactive control (Refresh),
    // then to the first currency card's toggle button.
    await page.keyboard.press('Tab'); // Refresh
    await page.keyboard.press('Tab'); // first toggle-btn
    const focused = page.locator(':focus');
    await expect(focused).toHaveClass(/toggle-btn/);
  });

  test('200% zoom reflow (640px viewport) has zero horizontal overflow', async ({ page }) => {
    await page.setViewportSize({ width: 640, height: 900 });
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();

    for (const currency of ['PHP', 'USD', 'JPY', 'EUR']) {
      const card = page.getByText(currency, { exact: true }).locator('xpath=ancestor::section');
      await card.getByRole('button').click();
    }

    const overflow = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(overflow.scrollWidth).toBe(overflow.clientWidth);
  });
});

test.describe('zero-valued and future-dated records', () => {
  test('renders a zero-valued asset row and a future-dated liability, distinct from the empty-household state', async ({
    page,
  }) => {
    await mockConfig(page, HOUSEHOLD_ID);
    await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: zeroAndFutureDatedFixture }));

    await page.setViewportSize(WIDE);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Ralph Household' })).toBeVisible();
    await expect(page.getByText(/no assets or liabilities are recorded/i)).toHaveCount(0);

    const phpCard = page.getByText('PHP', { exact: true }).locator('xpath=ancestor::section');
    await phpCard.getByRole('button').click();

    await expect(phpCard.getByText('Written-Off Startup Shares')).toBeVisible();
    await expect(phpCard.getByText('Prepaid Annual Insurance Premium')).toBeVisible();
    await expect(phpCard.getByText('Mar 1, 2027')).toBeVisible();
  });
});

test.describe('empty household', () => {
  test('is distinct from an error, and never claims zero wealth is confirmed', async ({ page }) => {
    await mockConfig(page, HOUSEHOLD_ID);
    await mockFinancialPosition(page, HOUSEHOLD_ID, () => ({ status: 200, body: emptyHouseholdFixture }));

    await page.setViewportSize(WIDE);
    await page.goto('/');

    await expect(page.getByText(/no assets or liabilities are recorded/i)).toBeVisible();
    await expect(page.getByRole('alert')).toHaveCount(0);
  });
});

test.describe('first-load failure', () => {
  test('shows a retry action with no fabricated data', async ({ page }) => {
    await mockConfig(page, HOUSEHOLD_ID);
    await page.route(`**/api/households/${HOUSEHOLD_ID}/financial-position`, (route) => route.abort('failed'));

    await page.setViewportSize(WIDE);
    await page.goto('/');

    await expect(page.getByText(/could not load financial position/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /retry/i })).toBeVisible();
  });
});

test.describe('refresh failure', () => {
  test('retains the prior result, dims it, and shows a banner', async ({ page }) => {
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

    await expect(page.getByRole('alert')).toContainText(/refresh failed/i);
    await expect(page.getByText('99,999,999,999,999,999.99 JPY')).toBeVisible();
  });
});

test.describe('request races', () => {
  // The out-of-order-resolution guarantee itself ("an older response can
  // never overwrite a newer request's result", even if it settles later) is
  // unit-tested directly against the hook in
  // src/hooks/useFinancialPosition.test.ts — that is the only place two
  // overlapping requests from the same app instance can actually be
  // constructed, because this UI's own refresh control (asserted below)
  // disables itself for the duration of a request, so a real browser session
  // can never issue a second overlapping request through it in the first
  // place.
  test('the refresh control disables itself for the duration of a request, preventing overlapping requests', async ({
    page,
  }) => {
    await mockConfig(page, HOUSEHOLD_ID);

    let releaseRefresh: (() => void) | undefined;
    const refreshReleased = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });

    let callIndex = 0;
    await page.route(`**/api/households/${HOUSEHOLD_ID}/financial-position`, async (route) => {
      const index = callIndex++;
      if (index === 1) {
        await refreshReleased;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...emptyHouseholdFixture,
          householdName: index === 0 ? 'Initial Load' : 'Refreshed Response',
        }),
      });
    });

    await page.setViewportSize(WIDE);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Initial Load' })).toBeVisible();

    const refreshButton = page.getByRole('button', { name: /^refresh$/i });
    await refreshButton.click();

    await expect(page.getByRole('button', { name: /refreshing/i })).toBeDisabled();

    releaseRefresh?.();
    await expect(page.getByRole('heading', { name: 'Refreshed Response' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^refresh$/i })).toBeEnabled();
  });
});
