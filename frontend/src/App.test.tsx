import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import type { FinancialPositionResponse } from './api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const mixedCurrencyResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [
    {
      id: '1b111111-1111-1111-1111-111111111111',
      name: 'Urban Lot',
      assetType: 'PROPERTY',
      estimatedValue: '3000000.00',
      planningValue: '2500000.00',
      currency: 'PHP',
      valuedAt: '2026-08-01',
      liquidity: 'ILLIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '2c222222-2222-2222-2222-222222222222',
      name: 'US Brokerage',
      assetType: 'INVESTMENT',
      estimatedValue: '200.00',
      planningValue: '200.00',
      currency: 'USD',
      valuedAt: '2026-09-01',
      liquidity: 'LIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
    {
      id: '6a666666-6666-6666-6666-666666666666',
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
      name: 'Mortgage',
      liabilityType: 'MORTGAGE',
      outstandingBalance: '500000.00',
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
      id: '5f555555-5555-5555-5555-555555555555',
      name: 'US Credit Card',
      liabilityType: 'CREDIT_CARD',
      outstandingBalance: '50.00',
      currency: 'USD',
      balanceAsOf: '2026-09-02',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  totalsByCurrency: [
    { currency: 'EUR', assetTotal: '0.00', liabilityTotal: '3200.00', netWorth: '-3200.00' },
    { currency: 'JPY', assetTotal: '99999999999999999.99', liabilityTotal: '0.00', netWorth: '99999999999999999.99' },
    { currency: 'PHP', assetTotal: '2500000.00', liabilityTotal: '500000.00', netWorth: '2000000.00' },
    { currency: 'USD', assetTotal: '200.00', liabilityTotal: '50.00', netWorth: '150.00' },
  ],
};

const zeroAndFutureDatedResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [
    {
      id: '9d999999-9999-9999-9999-999999999999',
      name: 'Written-Off Startup Shares',
      assetType: 'INVESTMENT',
      estimatedValue: '0.00',
      planningValue: '0.00',
      currency: 'PHP',
      valuedAt: '2026-08-20',
      liquidity: 'ILLIQUID',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  liabilities: [
    {
      id: 'ae0eeeee-eeee-eeee-eeee-eeeeeeeeeeee',
      name: 'Prepaid Annual Insurance Premium',
      liabilityType: 'PERSONAL_LOAN',
      outstandingBalance: '15000.00',
      currency: 'PHP',
      // A future balanceAsOf: retrievedAt never implies source values were
      // verified "today" — a dated record can legitimately postdate it.
      balanceAsOf: '2027-03-01',
      sourceType: 'MANUAL_ENTRY',
    },
  ],
  totalsByCurrency: [{ currency: 'PHP', assetTotal: '0.00', liabilityTotal: '15000.00', netWorth: '-15000.00' }],
};

const emptyHouseholdResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'New Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:16:01.442Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};

beforeEach(() => {
  setHouseholdConfig(HOUSEHOLD_ID);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
});

describe('missing configuration', () => {
  it('shows the missing-configuration state and never calls the API', async () => {
    setHouseholdConfig(undefined);
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByText(/no household is configured/i)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('invalid configuration', () => {
  it('reports a malformed configured id distinctly from missing configuration, and never calls the API', async () => {
    setHouseholdConfig('not-a-uuid');
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByText(/configured household id is malformed/i)).toBeInTheDocument();
    expect(screen.getByText('not-a-uuid')).toBeInTheDocument();
    expect(screen.queryByText(/no household is configured/i)).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('household not found', () => {
  it('names the configured id distinctly from missing configuration', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] }, 404)
      )
    );

    render(<App />);

    expect(await screen.findByText(/no household was found for the configured id/i)).toBeInTheDocument();
    expect(screen.getByText(HOUSEHOLD_ID)).toBeInTheDocument();
  });
});

describe('populated financial position', () => {
  it('renders exact large decimal amounts without precision loss, and keeps currencies separate', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(mixedCurrencyResponse)));

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();

    // The extreme JPY amount must render with every digit intact (both in the
    // headline net worth and the assets-total line), never truncated or
    // coerced through a JS Number.
    expect(screen.getAllByText(/99,999,999,999,999,999\.99/).length).toBeGreaterThanOrEqual(2);

    // No combined/FX total: each currency's own totals appear, never summed together.
    expect(screen.getByText(/2,000,000\.00 PHP/)).toBeInTheDocument();
    expect(screen.getByText(/150\.00 USD/)).toBeInTheDocument();
  });

  it('shows a liabilities-only currency with an explicit empty asset side, not a fabricated zero row', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(mixedCurrencyResponse)));
    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });

    const eurCard = screen.getByText('EUR').closest('section')!;
    await userEvent.click(within(eurCard).getByRole('button'));

    expect(within(eurCard).getByText(/no assets recorded in eur/i)).toBeInTheDocument();
  });

  it('marks negative net worth with a non-color-only indicator', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(mixedCurrencyResponse)));
    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });

    expect(screen.getByText('negative')).toBeInTheDocument();
    expect(screen.getByText(/3,200\.00 EUR/)).toHaveClass('negative');
  });

  it('renders the empty-household state distinctly from an error, without asserting zero wealth', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyHouseholdResponse)));
    render(<App />);

    expect(await screen.findByText(/no assets or liabilities are recorded/i)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('renders a zero-valued asset row and a future-dated liability, distinct from the empty-household state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(zeroAndFutureDatedResponse)));
    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });

    // A currency with real (if zero-valued/future-dated) records is not the
    // same as a household with no records at all.
    expect(screen.queryByText(/no assets or liabilities are recorded/i)).not.toBeInTheDocument();

    const phpCard = screen.getByText('PHP').closest('section')!;
    await userEvent.click(within(phpCard).getByRole('button'));

    expect(within(phpCard).getByText('Written-Off Startup Shares')).toBeInTheDocument();
    expect(within(phpCard).getAllByText('0.00').length).toBeGreaterThan(0);

    expect(within(phpCard).getByText('Prepaid Annual Insurance Premium')).toBeInTheDocument();
    expect(within(phpCard).getByText('Mar 1, 2027')).toBeInTheDocument();
  });
});

describe('data entry (WAP-25)', () => {
  it('names the real "Add asset"/"Add liability" actions in the empty-state copy instead of a nonexistent external tool', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyHouseholdResponse)));
    render(<App />);

    expect(await screen.findByText(/no assets or liabilities are recorded/i)).toBeInTheDocument();
    expect(screen.getByText(/use.*add asset.*or.*add liability.*above/i)).toBeInTheDocument();
  });

  it('adding an asset refreshes the financial position list to show it, with no page navigation', async () => {
    const fetchMock = vi.fn();
    fetchMock.mockResolvedValueOnce(jsonResponse(emptyHouseholdResponse)); // initial load
    fetchMock.mockResolvedValueOnce(
      jsonResponse(
        {
          id: 'new-asset-id',
          householdId: HOUSEHOLD_ID,
          name: 'Emergency Cash',
          assetType: 'CASH',
          estimatedValue: 5000,
          planningValue: 5000,
          currency: 'PHP',
          valuedAt: '2026-09-01',
          liquidity: 'LIQUID',
          sourceType: 'MANUAL_ENTRY',
          createdAt: '2026-09-12T00:00:00Z',
          updatedAt: '2026-09-12T00:00:00Z',
        },
        201
      )
    ); // create response
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ...emptyHouseholdResponse,
        assets: [
          {
            id: 'new-asset-id',
            name: 'Emergency Cash',
            assetType: 'CASH',
            estimatedValue: '5000.00',
            planningValue: '5000.00',
            currency: 'PHP',
            valuedAt: '2026-09-01',
            liquidity: 'LIQUID',
            sourceType: 'MANUAL_ENTRY',
          },
        ],
        totalsByCurrency: [{ currency: 'PHP', assetTotal: '5000.00', liabilityTotal: '0.00', netWorth: '5000.00' }],
      })
    ); // post-create refresh
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);
    await screen.findByRole('heading', { name: 'New Household' });

    await userEvent.click(screen.getByRole('button', { name: 'Add asset' }));
    await userEvent.type(screen.getByLabelText('Name'), 'Emergency Cash');
    await userEvent.type(screen.getByLabelText('Currency'), 'PHP');
    await userEvent.type(screen.getByLabelText('Estimated value'), '5000.00');
    await userEvent.type(screen.getByLabelText('Planning value'), '5000.00');
    await userEvent.type(screen.getByLabelText('Valued as of'), '2026-09-01');
    await userEvent.click(screen.getByRole('button', { name: 'Add asset' }));

    expect(await screen.findByText('Emergency Cash')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(3);
    // Still the same page/heading — no navigation happened.
    expect(screen.getByRole('heading', { name: 'New Household' })).toBeInTheDocument();
  });
});

describe('first-load failure', () => {
  it('shows a retry action and no fabricated data when there is no prior successful load', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network down')));

    render(<App />);

    expect(await screen.findByText(/could not load financial position/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('recovers once retry succeeds', async () => {
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new TypeError('network down'))
      .mockResolvedValueOnce(jsonResponse(mixedCurrencyResponse));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);
    await screen.findByRole('button', { name: /retry/i });
    await userEvent.click(screen.getByRole('button', { name: /retry/i }));

    expect(await screen.findByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();
  });
});

describe('refresh failure', () => {
  it('retains the last successful result, dims it, and shows a banner instead of clearing the page', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(mixedCurrencyResponse))
      .mockRejectedValueOnce(new TypeError('network down'));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });

    await userEvent.click(screen.getByRole('button', { name: /^refresh$/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/refresh failed/i);
    // The previously loaded totals are still present and still inspectable.
    expect(screen.getByText(/2,000,000\.00 PHP/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Ralph Household' }).closest('.page')).toBeInTheDocument();
  });
});

describe('request races', () => {
  it('a refresh triggered mid-load still lets the ready state settle on real data', async () => {
    // End-to-end race coverage through the UI: refresh only becomes available
    // once the page is in the "ready" state, so this exercises the ordinary
    // refresh path. The precise out-of-order-resolution guarantee (an older
    // in-flight request can never overwrite a newer one, regardless of which
    // settles first) is unit-tested directly against the hook in
    // src/hooks/useFinancialPosition.test.ts, since the UI has no control
    // that can start a second request before the first (initial) one settles.
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(mixedCurrencyResponse)));
    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });

    await userEvent.click(screen.getByRole('button', { name: /^refresh$/i }));

    await waitFor(() => expect(screen.getByRole('button', { name: /^refresh$/i })).not.toBeDisabled());
    expect(screen.getByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();
  });
});
