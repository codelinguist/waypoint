import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import type { FinancialSnapshot } from './api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';
const EARLIER_ID = '11111111-1111-1111-1111-111111111111';
const LATER_ID = '22222222-2222-2222-2222-222222222222';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const twoSnapshots: FinancialSnapshot[] = [
  {
    id: EARLIER_ID,
    householdId: HOUSEHOLD_ID,
    asOfDate: '2026-01-01',
    capturedAt: '2026-01-02T00:00:00Z',
    sourceType: 'MANUAL_ENTRY',
    assetLineItems: [],
    liabilityLineItems: [],
    totalsByCurrency: [{ currency: 'PHP', assetTotal: 1000, liabilityTotal: 200, netWorth: 800 }],
  },
  {
    id: LATER_ID,
    householdId: HOUSEHOLD_ID,
    asOfDate: '2026-09-01',
    capturedAt: '2026-09-02T00:00:00Z',
    sourceType: 'MANUAL_ENTRY',
    assetLineItems: [],
    liabilityLineItems: [],
    totalsByCurrency: [{ currency: 'PHP', assetTotal: 1600, liabilityTotal: 100, netWorth: 1500 }],
  },
];

async function goToSnapshotsTab() {
  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: 'Snapshots' }));
}

beforeEach(() => {
  setHouseholdConfig(HOUSEHOLD_ID);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
});

describe('navigation between sections', () => {
  it('starts on Financial position and switches to the Snapshots tab on request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(emptyPositionResponse()));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });
    expect(screen.getByRole('button', { name: 'Financial position' })).toHaveAttribute('aria-current', 'page');

    fetchMock.mockResolvedValueOnce(jsonResponse(twoSnapshots));
    await userEvent.click(screen.getByRole('button', { name: 'Snapshots' }));

    await screen.findByRole('heading', { name: 'Snapshots' });
    expect(screen.getByRole('button', { name: 'Snapshots' })).toHaveAttribute('aria-current', 'page');
    expect(fetchMock).toHaveBeenLastCalledWith(
      expect.stringContaining(`/api/households/${HOUSEHOLD_ID}/financial-snapshots`),
      expect.anything()
    );
  });
});

describe('snapshot list', () => {
  it('renders every snapshot returned by the list endpoint', async () => {
    // Two distinct Response objects: the initial Financial position tab
    // fetches first, and a Response body can only be read once.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValueOnce(jsonResponse(emptyPositionResponse())).mockResolvedValueOnce(jsonResponse(twoSnapshots))
    );

    await goToSnapshotsTab();

    await screen.findByRole('heading', { name: 'Snapshots' });
    const list = screen.getByRole('region', { name: /financial snapshots/i });
    expect(within(list).getByText('Jan 1, 2026')).toBeInTheDocument();
    expect(within(list).getByText('Sep 1, 2026')).toBeInTheDocument();
  });

  it('shows an empty state when no snapshots are recorded', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValueOnce(jsonResponse(emptyPositionResponse())).mockResolvedValueOnce(jsonResponse([]))
    );

    await goToSnapshotsTab();

    expect(await screen.findByText(/no financial snapshots are recorded/i)).toBeInTheDocument();
  });

  it('reports a not-found household', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 404)));

    await goToSnapshotsTab();

    expect(await screen.findByText(/no household was found/i)).toBeInTheDocument();
  });
});

describe('snapshot comparison', () => {
  it('selecting an earlier and later snapshot and comparing calls the comparison endpoint and renders the result', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(jsonResponse(emptyPositionResponse())).mockResolvedValueOnce(jsonResponse(twoSnapshots));
    vi.stubGlobal('fetch', fetchMock);

    await goToSnapshotsTab();
    await screen.findByRole('button', { name: /^compare$/i });

    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        earlierSnapshot: { id: EARLIER_ID, asOfDate: '2026-01-01', capturedAt: '2026-01-02T00:00:00Z' },
        laterSnapshot: { id: LATER_ID, asOfDate: '2026-09-01', capturedAt: '2026-09-02T00:00:00Z' },
        currencyDeltas: [{ currency: 'PHP', assetTotalDelta: 600, liabilityTotalDelta: -100, netWorthDelta: 700 }],
      })
    );

    await userEvent.selectOptions(screen.getByLabelText(/earlier snapshot/i), EARLIER_ID);
    await userEvent.selectOptions(screen.getByLabelText(/later snapshot/i), LATER_ID);
    await userEvent.click(screen.getByRole('button', { name: /^compare$/i }));

    const comparisonCall = fetchMock.mock.calls.at(-1)![0] as string;
    expect(comparisonCall).toContain(`/api/households/${HOUSEHOLD_ID}/financial-snapshots/comparison`);
    expect(comparisonCall).toContain(`earlierSnapshotId=${EARLIER_ID}`);
    expect(comparisonCall).toContain(`laterSnapshotId=${LATER_ID}`);

    const resultSection = (await screen.findByText(/comparing/i)).closest('section')!;
    expect(within(resultSection).getByText('PHP')).toBeInTheDocument();
    expect(within(resultSection).getByText(/700\.00/)).toBeInTheDocument();
  });

  it('surfaces the identical-snapshot comparison error instead of failing silently', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(jsonResponse(emptyPositionResponse())).mockResolvedValueOnce(jsonResponse(twoSnapshots));
    vi.stubGlobal('fetch', fetchMock);

    await goToSnapshotsTab();
    await screen.findByRole('button', { name: /^compare$/i });

    fetchMock.mockResolvedValueOnce(
      jsonResponse(
        {
          error: 'VALIDATION_FAILED',
          message: `Cannot compare a financial snapshot against itself: ${EARLIER_ID}`,
          details: [],
        },
        400
      )
    );

    await userEvent.selectOptions(screen.getByLabelText(/earlier snapshot/i), EARLIER_ID);
    await userEvent.selectOptions(screen.getByLabelText(/later snapshot/i), EARLIER_ID);
    await userEvent.click(screen.getByRole('button', { name: /^compare$/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/cannot compare.*itself/i);
  });
});

function emptyPositionResponse() {
  return {
    householdId: HOUSEHOLD_ID,
    householdName: 'Ralph Household',
    baseCurrency: 'PHP',
    retrievedAt: '2026-09-10T06:15:22.104Z',
    assets: [],
    liabilities: [],
    totalsByCurrency: [],
  };
}
