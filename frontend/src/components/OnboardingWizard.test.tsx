import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import type { FinancialPositionResponse } from '../api/types';

const HOUSEHOLD_ID = 'a1a1a1a1-a1a1-4a1a-8a1a-a1a1a1a1a1a1';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const populatedPositionResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Wizard Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-12T00:00:00Z',
  assets: [
    {
      id: 'asset-1',
      name: 'Savings',
      assetType: 'CASH',
      estimatedValue: '1000.00',
      planningValue: '1000.00',
      currency: 'PHP',
      valuedAt: '2026-09-01',
      liquidity: 'LIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  liabilities: [
    {
      id: 'liability-1',
      name: 'Credit Card',
      liabilityType: 'CREDIT_CARD',
      outstandingBalance: '200.00',
      currency: 'PHP',
      balanceAsOf: '2026-09-01',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  totalsByCurrency: [{ currency: 'PHP', assetTotal: '1000.00', liabilityTotal: '200.00', netWorth: '800.00' }],
};

const emptyPositionResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Empty Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-12T00:00:00Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};

/** Routes every request the wizard (and the app it hands off to) can make, by method + path fragment. */
function routedFetch(overrides: Partial<Record<string, () => Response>> = {}) {
  return vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    const key = `${method} ${url}`;

    if (method === 'POST' && url === '/api/households') {
      return Promise.resolve((overrides.createHousehold ?? (() => jsonResponse({ id: HOUSEHOLD_ID, name: 'Wizard Household', baseCurrency: 'PHP', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))());
    }
    if (method === 'POST' && url.endsWith('/people')) {
      return Promise.resolve(
        (overrides.createPerson ?? (() => jsonResponse({ id: 'person-1', householdId: HOUSEHOLD_ID, name: 'Ralph', role: 'Parent', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/assets')) {
      return Promise.resolve(
        (overrides.createAsset ?? (() => jsonResponse({ id: 'asset-1', householdId: HOUSEHOLD_ID, name: 'Savings', assetType: 'CASH', estimatedValue: 1000, planningValue: 1000, currency: 'PHP', valuedAt: '2026-09-01', liquidity: 'LIQUID', sourceType: 'MANUAL_ENTRY', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/liabilities')) {
      return Promise.resolve(
        (overrides.createLiability ?? (() => jsonResponse({ id: 'liability-1', householdId: HOUSEHOLD_ID, name: 'Credit Card', liabilityType: 'CREDIT_CARD', outstandingBalance: 200, currency: 'PHP', balanceAsOf: '2026-09-01', sourceType: 'MANUAL_ENTRY', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z', revision: 0 }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/income-streams')) {
      return Promise.resolve(
        (overrides.createIncomeStream ?? (() => jsonResponse({ id: 'income-1', householdId: HOUSEHOLD_ID, name: 'Salary', incomeType: 'SALARY', amount: 50000, frequency: 'MONTHLY', currency: 'PHP', compensationClassification: 'GROSS', certainty: 'CONFIRMED', startDate: '2026-09-01', endDate: null, sourceType: 'MANUAL_ENTRY', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/obligations')) {
      return Promise.resolve(
        (overrides.createObligation ?? (() => jsonResponse({ id: 'obligation-1', householdId: HOUSEHOLD_ID, name: 'Rent', obligationType: 'HOUSEHOLD_BASELINE', amount: 10000, frequency: 'MONTHLY', currency: 'PHP', startDate: '2026-09-01', endDate: null, sourceType: 'MANUAL_ENTRY', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/goals')) {
      return Promise.resolve(
        (overrides.createGoal ?? (() => jsonResponse({ id: 'goal-1', householdId: HOUSEHOLD_ID, name: 'Emergency fund', targetAmount: 100000, currency: 'PHP', targetDate: '2027-01-01', priority: 1, currentAmount: 0, remainingAmount: 100000, progressPercentage: 0, sourceType: 'MANUAL_ENTRY', createdAt: '2026-09-12T00:00:00Z', updatedAt: '2026-09-12T00:00:00Z' }, 201)))()
      );
    }
    if (method === 'POST' && url.endsWith('/financial-snapshots')) {
      return Promise.resolve(
        (overrides.createSnapshot ?? (() => jsonResponse({ id: 'snapshot-1', householdId: HOUSEHOLD_ID, asOfDate: '2026-09-12', capturedAt: '2026-09-12T00:00:00Z', sourceType: 'MANUAL_ENTRY', assetLineItems: [], liabilityLineItems: [], totalsByCurrency: [] }, 201)))()
      );
    }
    if (method === 'GET' && url.includes('/financial-position')) {
      return Promise.resolve((overrides.financialPosition ?? (() => jsonResponse(populatedPositionResponse)))());
    }
    throw new Error(`Unexpected fetch: ${key}`);
  });
}

beforeEach(() => {
  setHouseholdConfig(undefined);
  localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
  localStorage.clear();
});

async function completeStep(opener: RegExp | string | null, fields: Record<string, string>, submitName: string) {
  if (opener) {
    await userEvent.click(screen.getByRole('button', { name: opener }));
  }
  for (const [label, value] of Object.entries(fields)) {
    await userEvent.type(screen.getByLabelText(label), value);
  }
  await userEvent.click(screen.getByRole('button', { name: submitName }));
}

describe('fresh household (config missing)', () => {
  it('walks every step end to end and lands on a populated Financial position, with no HOUSEHOLD_ID restart', async () => {
    vi.stubGlobal('fetch', routedFetch());

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Set up your household' })).toBeInTheDocument();
    expect(screen.getByText('Step 1 of 7: Household & people')).toBeInTheDocument();

    // Step 1a: household.
    await completeStep(null, { 'Household name': 'Wizard Household', 'Base currency': 'php' }, 'Create household');
    expect(await screen.findByText(/Wizard Household.*created/)).toBeInTheDocument();

    // Step 1b: at least one person, "Next" gated until then.
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    await completeStep(null, { Name: 'Ralph', Role: 'Parent' }, 'Add person');
    expect(await screen.findByText('1 person added.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 2: assets.
    expect(await screen.findByText('Step 2 of 7: Assets')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    await completeStep(
      'Add asset',
      {
        Name: 'Savings',
        Currency: 'PHP',
        'Estimated value': '1000',
        'Planning value': '1000',
        'Valued as of': '2026-09-01',
      },
      'Add asset'
    );
    expect(await screen.findByText('1 asset added.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 3: liabilities.
    expect(await screen.findByText('Step 3 of 7: Liabilities')).toBeInTheDocument();
    await completeStep(
      'Add liability',
      { Name: 'Credit Card', Currency: 'PHP', 'Outstanding balance': '200', 'Balance as of': '2026-09-01' },
      'Add liability'
    );
    expect(await screen.findByText('1 liability added.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 4: income streams.
    expect(await screen.findByText('Step 4 of 7: Income streams')).toBeInTheDocument();
    await completeStep(
      'Add income stream',
      { Name: 'Salary', Currency: 'PHP', Amount: '50000', 'Start date': '2026-09-01' },
      'Add income stream'
    );
    expect(await screen.findByText('1 income stream added.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 5: obligations.
    expect(await screen.findByText('Step 5 of 7: Obligations')).toBeInTheDocument();
    await completeStep(
      'Add obligation',
      { Name: 'Rent', Currency: 'PHP', Amount: '10000', 'Start date': '2026-09-01' },
      'Add obligation'
    );
    expect(await screen.findByText('1 obligation added.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 6: goals.
    expect(await screen.findByText('Step 6 of 7: Goals')).toBeInTheDocument();
    await completeStep(
      'Add goal',
      {
        Name: 'Emergency fund',
        Currency: 'PHP',
        'Target amount': '100000',
        'Current amount': '0',
        'Target date': '2027-01-01',
        Priority: '1',
      },
      'Add goal'
    );
    expect(await screen.findByText('1 goal added.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    // Step 7: closing snapshot — no "Next"; the form's own success finishes the wizard.
    expect(await screen.findByText('Step 7 of 7: First snapshot')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
    await completeStep('Create snapshot', { 'As of date': '2026-09-12' }, 'Create snapshot');

    // The wizard is gone; the ordinary app (with AppNav) shows the data just entered.
    expect(await screen.findByRole('heading', { name: 'Wizard Household' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Financial position' })).toBeInTheDocument();
    expect(screen.getByText('Savings')).toBeInTheDocument();
    expect(screen.getByText('Credit Card')).toBeInTheDocument();

    // No manual HOUSEHOLD_ID/restart step: the id now lives in this browser's override.
    expect(localStorage.getItem('waypoint.householdIdOverride')).toBe(HOUSEHOLD_ID);
  });
});

describe('already-configured, empty household', () => {
  it('offers the wizard from a Financial position banner, skipping the household/person step', async () => {
    setHouseholdConfig(HOUSEHOLD_ID);
    vi.stubGlobal('fetch', routedFetch({ financialPosition: () => jsonResponse(emptyPositionResponse) }));

    render(<App />);
    await screen.findByText(/no assets or liabilities are recorded/i);

    await userEvent.click(screen.getByRole('button', { name: 'New here? Run guided setup' }));

    // Household/person step is skipped: 6 steps, starting at Assets.
    expect(await screen.findByText('Step 1 of 6: Assets')).toBeInTheDocument();
    expect(screen.queryByLabelText('Household name')).not.toBeInTheDocument();

    // Exiting returns to the ordinary empty-state page, not the middle of a half-finished wizard.
    await userEvent.click(screen.getByRole('button', { name: 'Exit setup' }));
    expect(await screen.findByText(/no assets or liabilities are recorded/i)).toBeInTheDocument();
  });
});
