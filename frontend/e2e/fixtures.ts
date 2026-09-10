import type { Page } from '@playwright/test';
import type { FinancialPositionResponse } from '../src/api/types';

export const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

/** Mocks the runtime config.js the app reads its household id from. */
export async function mockConfig(page: Page, householdId: string | null) {
  await page.route('**/config.js', (route) =>
    route.fulfill({
      contentType: 'application/javascript',
      body: `window.__WAYPOINT_CONFIG__ = { householdId: ${householdId ? JSON.stringify(householdId) : "''"} };`,
    })
  );
}

/** Mocks GET /api/households/{id}/financial-position. `onRequest` fires per call for sequencing/race tests. */
export async function mockFinancialPosition(
  page: Page,
  householdId: string,
  responder: (callIndex: number) => { status: number; body: unknown }
) {
  let callIndex = 0;
  await page.route(`**/api/households/${householdId}/financial-position`, async (route) => {
    const index = callIndex++;
    const { status, body } = responder(index);
    await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

export const mixedCurrencyFixture: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [
    {
      id: '1b111111-1111-1111-1111-111111111111',
      name: 'Urban Residential Lot — Riverside Heights Subdivision Phase 2 Block 14 Lot 9',
      assetType: 'PROPERTY',
      estimatedValue: '3200000.00',
      planningValue: '3000000.00',
      currency: 'PHP',
      valuedAt: '2026-06-01',
      liquidity: 'ILLIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '2c222222-2222-2222-2222-222222222222',
      name: 'US Brokerage Account',
      assetType: 'INVESTMENT',
      estimatedValue: '12500.50',
      planningValue: '12500.50',
      currency: 'USD',
      valuedAt: '2026-09-01',
      liquidity: 'LIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '7b777777-7777-7777-7777-777777777777',
      name: 'Precision Reserve Test Holding',
      assetType: 'INVESTMENT',
      estimatedValue: '99999999999999999.99',
      planningValue: '99999999999999999.99',
      currency: 'JPY',
      valuedAt: '2026-09-09',
      liquidity: 'LIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  liabilities: [
    {
      id: '3d333333-3333-3333-3333-333333333333',
      name: 'Home Mortgage',
      liabilityType: 'MORTGAGE',
      outstandingBalance: '7450000.00',
      currency: 'PHP',
      balanceAsOf: '2026-09-01',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '4e444444-4444-4444-4444-444444444444',
      name: 'European Consolidation Loan',
      liabilityType: 'PERSONAL_LOAN',
      outstandingBalance: '3200.00',
      currency: 'EUR',
      balanceAsOf: '2026-08-15',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '8c888888-8888-8888-8888-888888888888',
      name: 'US Personal Line of Credit',
      liabilityType: 'PERSONAL_LOAN',
      outstandingBalance: '8000.00',
      currency: 'USD',
      balanceAsOf: '2026-09-02',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  totalsByCurrency: [
    { currency: 'EUR', assetTotal: '0.00', liabilityTotal: '3200.00', netWorth: '-3200.00' },
    { currency: 'JPY', assetTotal: '99999999999999999.99', liabilityTotal: '0.00', netWorth: '99999999999999999.99' },
    { currency: 'PHP', assetTotal: '3000000.00', liabilityTotal: '7450000.00', netWorth: '-4450000.00' },
    { currency: 'USD', assetTotal: '12500.50', liabilityTotal: '8000.00', netWorth: '4500.50' },
  ],
};

export const emptyHouseholdFixture: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'New Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:16:01.442Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};
