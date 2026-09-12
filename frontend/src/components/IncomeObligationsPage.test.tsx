import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import type { FinancialPositionResponse, IncomeStream, Obligation } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const emptyPositionResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};

const incomeStreamsFixture: IncomeStream[] = [
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

const obligationsFixture: Obligation[] = [
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

function routedFetch({
  position = () => jsonResponse(emptyPositionResponse),
  incomeStreams = () => jsonResponse([]),
  obligations = () => jsonResponse([]),
}: {
  position?: () => Response;
  incomeStreams?: () => Response;
  obligations?: () => Response;
}) {
  return vi.fn((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/financial-position')) return Promise.resolve(position());
    if (url.includes('/income-streams')) return Promise.resolve(incomeStreams());
    if (url.includes('/obligations')) return Promise.resolve(obligations());
    throw new Error(`Unexpected fetch url: ${url}`);
  });
}

beforeEach(() => {
  setHouseholdConfig(HOUSEHOLD_ID);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
});

async function navigateToIncomeObligations() {
  render(<App />);
  await screen.findByRole('heading', { name: 'Ralph Household' });
  await userEvent.click(screen.getByRole('button', { name: 'Income & obligations' }));
  await screen.findByRole('heading', { name: 'Income & obligations' });
}

describe('navigation', () => {
  it('switches between the financial position and income & obligations sections without a page reload', async () => {
    vi.stubGlobal('fetch', routedFetch({ incomeStreams: () => jsonResponse(incomeStreamsFixture) }));

    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });
    expect(screen.getByRole('button', { name: 'Financial position' })).toHaveAttribute('aria-current', 'page');

    await userEvent.click(screen.getByRole('button', { name: 'Income & obligations' }));

    expect(await screen.findByRole('heading', { name: 'Income & obligations' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Income & obligations' })).toHaveAttribute('aria-current', 'page');
    expect(screen.queryByRole('heading', { name: 'Ralph Household' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Financial position' }));
    expect(await screen.findByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();
  });
});

describe('income streams and obligations', () => {
  it('renders every documented field, distinguishing non-confirmed certainty', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        incomeStreams: () => jsonResponse(incomeStreamsFixture),
        obligations: () => jsonResponse(obligationsFixture),
      })
    );
    await navigateToIncomeObligations();

    // Scoped to the income streams table: the page's own "Add income
    // stream" form (WAP-25) also renders type/certainty/classification
    // options with this same text, so an unscoped screen.getByText would be
    // ambiguous.
    const incomeStreamsTable = screen.getByRole('region', { name: 'Income streams, scrollable' });

    expect(within(incomeStreamsTable).getByText('October Salary')).toBeInTheDocument();
    expect(screen.getByText('Freelance Retainer')).toBeInTheDocument();
    expect(screen.getByText(/50,000\.00 PHP/)).toBeInTheDocument();
    expect(screen.getByText(/1,234\.50 USD/)).toBeInTheDocument();
    expect(within(incomeStreamsTable).getByText('Business distribution')).toBeInTheDocument();
    expect(within(incomeStreamsTable).getByText('Net')).toBeInTheDocument();
    // Both "October Salary" (income) and "Home Mortgage Payment" (obligation) are open-ended.
    expect(screen.getAllByText('Ongoing')).toHaveLength(2);

    // Certainty: CONFIRMED is unmarked, VARIABLE is visually flagged (badge
    // styling) and additionally called out for assistive tech (sr-only text).
    const confirmedBadge = within(incomeStreamsTable).getByText('Confirmed');
    expect(confirmedBadge.className).not.toContain('badge--attention');
    const variableBadge = within(incomeStreamsTable).getByText('Variable', { exact: false }).closest('.badge')!;
    expect(variableBadge.className).toContain('badge--attention');
    expect(within(variableBadge as HTMLElement).getByText(/not confirmed/i)).toBeInTheDocument();

    // Obligation fields.
    expect(screen.getByText('Home Mortgage Payment')).toBeInTheDocument();
    expect(screen.getByText(/25,000\.00 PHP/)).toBeInTheDocument();
  });

  it('shows a distinct empty state when the household has no income streams or obligations', async () => {
    vi.stubGlobal('fetch', routedFetch({}));
    await navigateToIncomeObligations();

    expect(await screen.findByText(/no income streams or obligations are recorded/i)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows an error state with retry when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        incomeStreams: () => jsonResponse({ error: 'INTERNAL', message: 'boom', details: [] }, 500),
      })
    );
    await navigateToIncomeObligations();

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load income streams and obligations/i);
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('shows the not-found state distinctly, naming the configured household id', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        incomeStreams: () => jsonResponse({ error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] }, 404),
      })
    );
    await navigateToIncomeObligations();

    expect(await screen.findByText(/no household was found for the configured id/i)).toBeInTheDocument();
    expect(screen.getByText(HOUSEHOLD_ID)).toBeInTheDocument();
  });
});
