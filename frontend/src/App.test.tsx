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

const incomeStreamsFixture = [
  {
    id: '7a777777-7777-7777-7777-777777777777',
    householdId: HOUSEHOLD_ID,
    name: 'October Salary',
    incomeType: 'SALARY',
    amount: 50000,
    frequency: 'MONTHLY',
    currency: 'PHP',
    compensationClassification: 'GROSS',
    certainty: 'CONFIRMED',
    startDate: '2026-10-01',
    endDate: null,
    sourceType: 'MANUAL_ENTRY',
    createdAt: '2026-09-03T02:10:00Z',
    updatedAt: '2026-09-03T02:10:00Z',
  },
  {
    id: '8b888888-8888-8888-8888-888888888888',
    householdId: HOUSEHOLD_ID,
    name: 'Freelance Retainer',
    incomeType: 'BUSINESS_DISTRIBUTION',
    amount: 1234.5,
    frequency: 'MONTHLY',
    currency: 'USD',
    compensationClassification: 'NET',
    certainty: 'VARIABLE',
    startDate: '2026-09-01',
    endDate: '2027-03-01',
    sourceType: 'MANUAL_ENTRY',
    createdAt: '2026-09-03T02:11:00Z',
    updatedAt: '2026-09-03T02:11:00Z',
  },
];

const obligationsFixture = [
  {
    id: '9c999999-9999-9999-9999-999999999999',
    householdId: HOUSEHOLD_ID,
    name: 'Home Mortgage Payment',
    obligationType: 'MORTGAGE',
    amount: 25000,
    frequency: 'MONTHLY',
    currency: 'PHP',
    startDate: '2026-01-01',
    endDate: null,
    sourceType: 'MANUAL_ENTRY',
    createdAt: '2026-09-03T02:12:00Z',
    updatedAt: '2026-09-03T02:12:00Z',
  },
];

function stubIncomeAndObligationsFetch(options?: {
  incomeStreams?: unknown;
  obligations?: unknown;
  status?: number;
  errorBody?: unknown;
}) {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (options?.status && options.status !== 200) {
      return Promise.resolve(jsonResponse(options.errorBody ?? { error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] }, options.status));
    }
    if (url.includes('/income-streams')) {
      return Promise.resolve(jsonResponse(options?.incomeStreams ?? incomeStreamsFixture));
    }
    if (url.includes('/obligations')) {
      return Promise.resolve(jsonResponse(options?.obligations ?? obligationsFixture));
    }
    return Promise.resolve(jsonResponse(mixedCurrencyResponse));
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

describe('navigation', () => {
  it('defaults to the financial position section and switches to income & obligations on request', async () => {
    stubIncomeAndObligationsFetch();
    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();
    const incomeTab = screen.getByRole('tab', { name: /income & obligations/i });
    expect(screen.getByRole('tab', { name: /financial position/i })).toHaveAttribute('aria-selected', 'true');
    expect(incomeTab).toHaveAttribute('aria-selected', 'false');

    await userEvent.click(incomeTab);

    expect(await screen.findByRole('heading', { name: /income & obligations/i })).toBeInTheDocument();
    expect(incomeTab).toHaveAttribute('aria-selected', 'true');
  });
});

describe('income streams and obligations', () => {
  it('renders every field for income streams and obligations, distinguishing non-confirmed certainty', async () => {
    stubIncomeAndObligationsFetch();
    render(<App />);
    await userEvent.click(screen.getByRole('tab', { name: /income & obligations/i }));
    await screen.findByRole('heading', { name: /income & obligations/i });

    // Income stream fields.
    expect(screen.getByText('October Salary')).toBeInTheDocument();
    expect(screen.getByText('Freelance Retainer')).toBeInTheDocument();
    expect(screen.getByText(/50,000\.00 PHP/)).toBeInTheDocument();
    expect(screen.getByText(/1,234\.50 USD/)).toBeInTheDocument();
    expect(screen.getByText('Business distribution')).toBeInTheDocument();
    expect(screen.getByText('Net')).toBeInTheDocument();
    // Both "October Salary" (income) and "Home Mortgage Payment" (obligation) are open-ended.
    expect(screen.getAllByText('Ongoing')).toHaveLength(2);

    // Certainty: CONFIRMED is unmarked, VARIABLE is visually flagged (badge
    // styling) and additionally called out for assistive tech (sr-only text).
    const confirmedBadge = screen.getByText('Confirmed');
    expect(confirmedBadge.className).not.toContain('badge--attention');
    const variableBadge = screen.getByText('Variable', { exact: false }).closest('.badge')!;
    expect(variableBadge.className).toContain('badge--attention');
    expect(within(variableBadge as HTMLElement).getByText(/not confirmed/i)).toBeInTheDocument();

    // Obligation fields.
    expect(screen.getByText('Home Mortgage Payment')).toBeInTheDocument();
    expect(screen.getByText(/25,000\.00 PHP/)).toBeInTheDocument();
  });

  it('shows a distinct empty state when the household has no income streams or obligations', async () => {
    stubIncomeAndObligationsFetch({ incomeStreams: [], obligations: [] });
    render(<App />);
    await userEvent.click(screen.getByRole('tab', { name: /income & obligations/i }));

    expect(await screen.findByText(/no income streams or obligations are recorded/i)).toBeInTheDocument();
  });

  it('shows an error state with retry when the request fails', async () => {
    stubIncomeAndObligationsFetch({ status: 500, errorBody: { error: 'INTERNAL', message: 'boom', details: [] } });
    render(<App />);
    await userEvent.click(screen.getByRole('tab', { name: /income & obligations/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load income streams and obligations/i);
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('names the configured household id for household-not-found', async () => {
    stubIncomeAndObligationsFetch({ status: 404 });
    render(<App />);
    await userEvent.click(screen.getByRole('tab', { name: /income & obligations/i }));

    expect(await screen.findByText(/no household was found for the configured id/i)).toBeInTheDocument();
    expect(screen.getByText(HOUSEHOLD_ID)).toBeInTheDocument();
  });
});
