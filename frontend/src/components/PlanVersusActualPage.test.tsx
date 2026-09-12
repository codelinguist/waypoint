import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import type { FinancialPositionResponse, FinancialSnapshotListItem, PlanVersusActualResponse } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';
const SNAPSHOT_ID = '7a1b2c3d-4e5f-4a1b-8c2d-1234567890ab';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const emptyPositionResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'USD',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};

/**
 * Routes by URL/method instead of call order: App.tsx's default "position"
 * view fires its own financial-position fetch as soon as a household is
 * configured, before a test ever navigates to Plan vs. actual, so an
 * order-based mock queue would misroute that background call onto this
 * page's snapshot-list/plan-comparison responses (and crash
 * FinancialPositionPage on the resulting shape mismatch).
 */
function routedFetch(routes: {
  snapshots?: () => Response;
  comparison?: () => Response;
}): ReturnType<typeof vi.fn> {
  return vi.fn((url: string, init?: RequestInit) => {
    if (url.endsWith('/plan-comparison')) {
      return Promise.resolve((routes.comparison ?? (() => jsonResponse({})))());
    }
    if (url.endsWith('/financial-snapshots')) {
      return Promise.resolve((routes.snapshots ?? (() => jsonResponse([])))());
    }
    if (url.endsWith('/financial-position')) {
      return Promise.resolve(jsonResponse(emptyPositionResponse));
    }
    throw new Error(`Unexpected fetch in test: ${init?.method ?? 'GET'} ${url}`);
  });
}

const snapshotList: FinancialSnapshotListItem[] = [
  {
    id: SNAPSHOT_ID,
    asOfDate: '2026-09-01',
    capturedAt: '2026-09-01T08:00:00Z',
    totalsByCurrency: [
      { currency: 'USD', assetTotal: 1000, liabilityTotal: 200, netWorth: 800 },
      { currency: 'EUR', assetTotal: 500, liabilityTotal: 0, netWorth: 500 },
    ],
  },
];

const comparisonResponse: PlanVersusActualResponse = {
  snapshot: { id: SNAPSHOT_ID, asOfDate: '2026-09-01', capturedAt: '2026-09-01T08:00:00Z' },
  currencyResults: [
    {
      currency: 'USD',
      assetTotal: { planned: 900, actual: 1000, variance: 100, direction: 'ABOVE_PLAN' },
      liabilityTotal: { planned: 300, actual: 200, variance: -100, direction: 'BELOW_PLAN' },
      netWorth: { planned: 600, actual: 800, variance: 200, direction: 'ABOVE_PLAN' },
    },
    {
      currency: 'EUR',
      assetTotal: { planned: 500, actual: 500, variance: 0, direction: 'ON_PLAN' },
      liabilityTotal: { planned: 0, actual: 0, variance: 0, direction: 'ON_PLAN' },
      netWorth: { planned: 500, actual: 500, variance: 0, direction: 'ON_PLAN' },
    },
  ],
};

async function goToPlanVersusActual() {
  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: 'Plan vs. actual' }));
  await screen.findByRole('heading', { name: 'Plan vs. actual' });
}

beforeEach(() => {
  setHouseholdConfig(HOUSEHOLD_ID);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
});

describe('snapshot loading', () => {
  it('lists recorded snapshots and seeds one input row per currency present in the selected snapshot', async () => {
    vi.stubGlobal('fetch', routedFetch({ snapshots: () => jsonResponse(snapshotList) }));
    await goToPlanVersusActual();

    expect(await screen.findByLabelText('Planned assets total (USD)')).toBeInTheDocument();
    expect(screen.getByLabelText('Planned assets total (EUR)')).toBeInTheDocument();
  });

  it('shows an empty state when the household has no recorded snapshots yet', async () => {
    vi.stubGlobal('fetch', routedFetch({ snapshots: () => jsonResponse([]) }));
    await goToPlanVersusActual();

    expect(await screen.findByText(/no financial snapshots are recorded/i)).toBeInTheDocument();
  });

  it('shows a retry action when the snapshot list fails to load', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.endsWith('/financial-snapshots')) return Promise.reject(new TypeError('network down'));
        return Promise.resolve(jsonResponse(emptyPositionResponse));
      })
    );
    await goToPlanVersusActual();

    expect(await screen.findByText(/could not load financial snapshots/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });
});

describe('plan comparison', () => {
  it('computes planned net worth locally and submits assetTotal/liabilityTotal/netWorth per currency', async () => {
    const fetchMock = routedFetch({
      snapshots: () => jsonResponse(snapshotList),
      comparison: () => jsonResponse(comparisonResponse),
    });
    vi.stubGlobal('fetch', fetchMock);
    await goToPlanVersusActual();

    await userEvent.type(await screen.findByLabelText('Planned assets total (USD)'), '900');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (USD)'), '300');
    await userEvent.type(screen.getByLabelText('Planned assets total (EUR)'), '500');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (EUR)'), '0');

    // Locally computed planned net worth (assetTotal - liabilityTotal) shown before submit.
    expect(screen.getByText('600.00')).toBeInTheDocument();
    expect(screen.getByText('500.00')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Compare to plan' }));

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/households/${HOUSEHOLD_ID}/financial-snapshots/${SNAPSHOT_ID}/plan-comparison`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          plannedMeasures: [
            { currency: 'USD', assetTotal: '900', liabilityTotal: '300', netWorth: '600.00' },
            { currency: 'EUR', assetTotal: '500', liabilityTotal: '0', netWorth: '500.00' },
          ],
        }),
      })
    );

    expect(await screen.findByText(/as of sep 1, 2026/i)).toBeInTheDocument();
    const usdCard = screen.getByText('USD', { selector: '.currency-code' }).closest<HTMLElement>('.card')!;
    expect(within(usdCard).getAllByText('Above plan', { selector: '.badge' }).length).toBeGreaterThan(0);
    expect(within(usdCard).getByText('Below plan', { selector: '.badge' })).toBeInTheDocument();
  });

  it('disables submission until every currency row has a valid non-negative planned total', async () => {
    vi.stubGlobal('fetch', routedFetch({ snapshots: () => jsonResponse(snapshotList) }));
    await goToPlanVersusActual();

    const submitButton = screen.getByRole('button', { name: 'Compare to plan' });
    expect(submitButton).toBeDisabled();

    await userEvent.type(await screen.findByLabelText('Planned assets total (USD)'), '900');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (USD)'), '300');
    expect(submitButton).toBeDisabled(); // EUR row still empty

    await userEvent.type(screen.getByLabelText('Planned assets total (EUR)'), '500');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (EUR)'), '0');
    expect(submitButton).not.toBeDisabled();
  });

  it('surfaces an API error (e.g. an invalid snapshot) instead of failing silently', async () => {
    const fetchMock = routedFetch({
      snapshots: () => jsonResponse(snapshotList),
      comparison: () =>
        jsonResponse({ error: 'FINANCIAL_SNAPSHOT_NOT_FOUND', message: 'Snapshot not found', details: [] }, 404),
    });
    vi.stubGlobal('fetch', fetchMock);
    await goToPlanVersusActual();

    await userEvent.type(await screen.findByLabelText('Planned assets total (USD)'), '900');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (USD)'), '300');
    await userEvent.type(screen.getByLabelText('Planned assets total (EUR)'), '500');
    await userEvent.type(screen.getByLabelText('Planned liabilities total (EUR)'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Compare to plan' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/snapshot not found/i);
  });

  it('resets the form and any prior result when a different snapshot is selected', async () => {
    const secondSnapshot: FinancialSnapshotListItem = {
      id: 'b2c3d4e5-6f7a-4b1c-9d2e-234567890abc',
      asOfDate: '2026-08-01',
      capturedAt: '2026-08-01T08:00:00Z',
      totalsByCurrency: [{ currency: 'USD', assetTotal: 100, liabilityTotal: 0, netWorth: 100 }],
    };
    vi.stubGlobal(
      'fetch',
      routedFetch({ snapshots: () => jsonResponse([...snapshotList, secondSnapshot]) })
    );
    await goToPlanVersusActual();

    await userEvent.type(await screen.findByLabelText('Planned assets total (USD)'), '900');
    expect(screen.getByLabelText('Planned assets total (EUR)')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Snapshot to compare against'), [
      screen.getByText(/aug 1, 2026/i),
    ]);

    expect(screen.queryByLabelText('Planned assets total (EUR)')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Planned assets total (USD)')).toHaveValue('');
  });
});
