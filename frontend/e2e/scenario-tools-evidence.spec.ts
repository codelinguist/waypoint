import { expect, test } from '@playwright/test';
import type {
  DebtPrepaymentComparisonResponse,
  IncomeInterruptionScenarioResponse,
  PurchaseReserveImpactResponse,
} from '../src/api/types';
import { mockConfig } from './fixtures';

// Not a correctness suite — captures representative wide/narrow screenshots
// of the real implementation as durable evidence under
// agent/ui/scenario-tools/evidence/implementation/, per the Implement
// stage's requirement to capture wide/narrow UI evidence for UI work.
const OUT_DIR = '../agent/ui/scenario-tools/evidence/implementation';
const WIDE = { width: 1440, height: 1200 };
const NARROW = { width: 390, height: 1400 };

const runwayFixture = {
  currency: 'USD',
  availableReserve: 5000,
  monthlyExpenses: 2000,
  monthlyNetIncome: 1500,
  monthlyShortfall: 500,
  status: 'FINITE' as const,
  runwayMonths: 10,
  fullMonthsCovered: 10,
  modelNote: 'Constant-input runway estimate computed only from the supplied reserve, expenses, and income.',
};

const purchaseReserveImpactFixture: PurchaseReserveImpactResponse = {
  currency: 'USD',
  availableReserve: 5000,
  purchaseAmount: 1000,
  monthlyExpenses: 2000,
  monthlyNetIncome: 1500,
  minimumReserve: 3000,
  reserveAfterPurchase: 4000,
  purchaseFundingGap: 0,
  purchaseFitsAvailableCash: true,
  baselineReserveFloorGap: 0,
  reserveFloorGapAfterPurchase: 0,
  reserveMeetsFloorAfterPurchase: true,
  beforePurchaseRunway: runwayFixture,
  afterPurchaseRunwayAvailability: 'AVAILABLE',
  afterPurchaseRunway: { ...runwayFixture, availableReserve: 4000, runwayMonths: 8, fullMonthsCovered: 8 },
  modelNote:
    'Neutral, read-only scenario facts computed only from the supplied inputs; this result does not approve, ' +
    'deny, or recommend a purchase or reserve floor, and it does not persist or represent any household decision. ' +
    "Before/after coverage reuses the emergency-fund runway calculator's constant-input convention, which " +
    'excludes any change in income, spending, interest, inflation, or timing within a month.',
};

const incomeInterruptionFixture: IncomeInterruptionScenarioResponse = {
  currency: 'USD',
  openingReserve: 10000,
  normalMonthlyNetIncome: 3000,
  interruptedMonthlyNetIncome: 0,
  monthlyExpenses: 2500,
  horizonMonths: 3,
  interruptionStartMonth: 1,
  interruptionMonths: 1,
  baselineRows: [
    { month: 1, openingCash: 10000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 10500 },
    { month: 2, openingCash: 10500, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 11000 },
    { month: 3, openingCash: 11000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 11500 },
  ],
  scenarioRows: [
    { month: 1, openingCash: 10000, income: 0, expenses: 2500, netCashFlow: -2500, closingCash: 7500 },
    { month: 2, openingCash: 7500, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 8000 },
    { month: 3, openingCash: 8000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 8500 },
  ],
  closingDeltas: [-3000, -3000, -3000],
  endingCash: 8500,
  minimumCash: 7500,
  firstNegativeMonth: null,
  additionalOpeningReserveNeeded: 0,
};

const debtPrepaymentFixture: DebtPrepaymentComparisonResponse = {
  principal: 10000,
  monthlyInterestRate: 0.01,
  monthlyPayment: 500,
  currency: 'USD',
  immediatePrepayment: 2000,
  baseline: {
    startingBalance: 10000,
    status: 'PAID_OFF',
    payoffMonths: 22,
    totalPaid: 10800,
    totalInterest: 800,
    remainingBalance: 0,
  },
  scenario: {
    startingBalance: 8000,
    status: 'PAID_OFF',
    payoffMonths: 17,
    totalPaid: 8500,
    totalInterest: 500,
    remainingBalance: 0,
  },
  scenarioTotalCashPaid: 10500,
  lifetimeInterestSaved: 300,
  payoffMonthsSaved: 5,
  lifetimeCashSaved: 300,
  comparisonUnavailableReason: null,
};

async function mockScenarioRoutes(
  page: import('@playwright/test').Page,
  overrides: {
    purchaseReserveImpact?: { status: number; body: unknown };
    incomeInterruption?: { status: number; body: unknown };
    debtPrepayment?: { status: number; body: unknown };
  } = {}
) {
  const purchase = overrides.purchaseReserveImpact ?? { status: 200, body: purchaseReserveImpactFixture };
  const interruption = overrides.incomeInterruption ?? { status: 200, body: incomeInterruptionFixture };
  const prepayment = overrides.debtPrepayment ?? { status: 200, body: debtPrepaymentFixture };

  await page.route('**/api/scenarios/purchase-reserve-impact', (route) =>
    route.fulfill({ status: purchase.status, contentType: 'application/json', body: JSON.stringify(purchase.body) })
  );
  await page.route('**/api/scenarios/income-interruption', (route) =>
    route.fulfill({
      status: interruption.status,
      contentType: 'application/json',
      body: JSON.stringify(interruption.body),
    })
  );
  await page.route('**/api/scenarios/debt-prepayment', (route) =>
    route.fulfill({ status: prepayment.status, contentType: 'application/json', body: JSON.stringify(prepayment.body) })
  );
}

async function fillAllForms(page: import('@playwright/test').Page) {
  await page.getByLabel('Available reserve').fill('5000.00');
  await page.getByLabel('Purchase amount').fill('1000.00');
  await page.locator('#pri-monthly-expenses').fill('2000.00');
  await page.locator('#pri-monthly-net-income').fill('1500.00');
  await page.getByLabel('Minimum reserve floor').fill('3000.00');
  await page.getByRole('button', { name: 'Calculate reserve impact' }).click();
  await expect(page.getByText(/4,000\.00 USD/)).toBeVisible();

  await page.getByLabel('Opening reserve').fill('10000.00');
  await page.getByLabel('Normal monthly net income').fill('3000.00');
  await page.getByLabel('Interrupted monthly net income').fill('0.00');
  await page.locator('#iis-monthly-expenses').fill('2500.00');
  await page.getByLabel('Horizon (months)').fill('3');
  await page.getByRole('button', { name: 'Run interruption scenario' }).click();
  await expect(page.getByText('Month 2')).toBeVisible();

  await page.getByLabel('Principal').fill('10000.00');
  await page.getByLabel('Monthly interest rate').fill('0.01');
  await page.getByLabel('Monthly payment').fill('500.00');
  await page.getByLabel('Immediate prepayment').fill('2000.00');
  await page.getByRole('button', { name: 'Compare prepayment' }).click();
  await expect(page.getByText(/5 months sooner/)).toBeVisible();
}

test('empty scenarios view — wide', async ({ page }) => {
  await mockConfig(page, null);
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await page.getByRole('button', { name: 'Scenarios' }).click();
  await expect(page.getByRole('heading', { name: 'What-if scenarios' })).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/scenarios-empty-wide.png`, fullPage: true });
});

test('all three scenarios populated — wide and narrow', async ({ page }) => {
  await mockConfig(page, null);
  await mockScenarioRoutes(page);
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await page.getByRole('button', { name: 'Scenarios' }).click();
  await fillAllForms(page);
  await page.screenshot({ path: `${OUT_DIR}/scenarios-populated-wide.png`, fullPage: true });

  await page.setViewportSize(NARROW);
  await page.screenshot({ path: `${OUT_DIR}/scenarios-populated-narrow.png`, fullPage: true });
});

test('purchase exceeding available cash — after-purchase runway unavailable', async ({ page }) => {
  await mockConfig(page, null);
  await mockScenarioRoutes(page, {
    purchaseReserveImpact: {
      status: 200,
      body: {
        ...purchaseReserveImpactFixture,
        availableReserve: 500,
        purchaseAmount: 1000,
        reserveAfterPurchase: -500,
        purchaseFundingGap: 500,
        purchaseFitsAvailableCash: false,
        reserveMeetsFloorAfterPurchase: false,
        reserveFloorGapAfterPurchase: 3500,
        afterPurchaseRunwayAvailability: 'INSUFFICIENT_CASH',
        afterPurchaseRunway: null,
      },
    },
  });
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await page.getByRole('button', { name: 'Scenarios' }).click();

  await page.getByLabel('Available reserve').fill('500.00');
  await page.getByLabel('Purchase amount').fill('1000.00');
  await page.locator('#pri-monthly-expenses').fill('2000.00');
  await page.locator('#pri-monthly-net-income').fill('1500.00');
  await page.getByLabel('Minimum reserve floor').fill('3000.00');
  await page.getByRole('button', { name: 'Calculate reserve impact' }).click();

  await expect(page.getByText('Not available')).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/purchase-reserve-insufficient-cash-wide.png`, fullPage: true });
});

test('debt prepayment savings unavailable — horizon limit reached', async ({ page }) => {
  await mockConfig(page, null);
  await mockScenarioRoutes(page, {
    debtPrepayment: {
      status: 200,
      body: {
        ...debtPrepaymentFixture,
        scenario: { ...debtPrepaymentFixture.scenario, status: 'HORIZON_LIMIT', payoffMonths: null },
        lifetimeInterestSaved: null,
        payoffMonthsSaved: null,
        lifetimeCashSaved: null,
        comparisonUnavailableReason:
          "The scenario path does not reach PAID_OFF (HORIZON_LIMIT), so its truncated or absent total cannot " +
          "be compared against the baseline's lifetime total.",
      },
    },
  });
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await page.getByRole('button', { name: 'Scenarios' }).click();

  await page.getByLabel('Principal').fill('10000.00');
  await page.getByLabel('Monthly interest rate').fill('0.01');
  await page.getByLabel('Monthly payment').fill('500.00');
  await page.getByLabel('Immediate prepayment').fill('2000.00');
  await page.getByRole('button', { name: 'Compare prepayment' }).click();

  await expect(page.getByText(/does not reach PAID_OFF/)).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/debt-prepayment-savings-unavailable-wide.png`, fullPage: true });
});

test('validation error surfaced instead of failing silently — wide', async ({ page }) => {
  await mockConfig(page, null);
  await mockScenarioRoutes(page, {
    incomeInterruption: {
      status: 400,
      body: {
        error: 'VALIDATION_FAILED',
        message: 'interruptedMonthlyNetIncome must not exceed normalMonthlyNetIncome',
        details: [],
      },
    },
  });
  await page.setViewportSize(WIDE);
  await page.goto('/');
  await page.getByRole('button', { name: 'Scenarios' }).click();

  await page.getByLabel('Opening reserve').fill('0');
  await page.getByLabel('Normal monthly net income').fill('0');
  await page.getByLabel('Interrupted monthly net income').fill('100');
  await page.locator('#iis-monthly-expenses').fill('0');
  await page.getByRole('button', { name: 'Run interruption scenario' }).click();

  await expect(page.getByRole('alert')).toBeVisible();
  await page.screenshot({ path: `${OUT_DIR}/income-interruption-validation-error-wide.png`, fullPage: true });
});
