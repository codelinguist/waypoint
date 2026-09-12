import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PlanningAssumptionsPage } from './PlanningAssumptionsPage';
import type { PlanningAssumption } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const currentAssumption: PlanningAssumption = {
  id: 'a1111111-1111-1111-1111-111111111111',
  householdId: HOUSEHOLD_ID,
  name: 'Inflation rate',
  value: '3.5',
  valueType: 'percentage',
  notes: null,
  effectiveFrom: '2026-01-01',
  effectiveUntil: null,
  reviewDate: '2027-01-01',
  sourceType: 'MANUAL_ENTRY',
  supersededBy: null,
  createdAt: '2026-01-01T00:00:00Z',
};

const supersededAssumption: PlanningAssumption = {
  ...currentAssumption,
  id: 'a2222222-2222-2222-2222-222222222222',
  value: '3.0',
  supersededBy: currentAssumption.id,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('PlanningAssumptionsPage', () => {
  it('lists assumptions, offers Supersede only for the current one, and shows an empty state distinct from an error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([supersededAssumption, currentAssumption])));

    render(<PlanningAssumptionsPage householdId={HOUSEHOLD_ID} />);

    const table = await screen.findByRole('region', { name: 'Planning assumptions, scrollable' });
    expect(within(table).getAllByText('Inflation rate', { selector: 'td' })).toHaveLength(2);
    expect(within(table).getAllByText('Current')).toHaveLength(1);
    expect(within(table).getAllByText('Superseded')).toHaveLength(1);
    // Only the current (non-superseded) row gets a Supersede action.
    expect(within(table).getAllByRole('button', { name: 'Supersede' })).toHaveLength(1);
  });

  it('creates a new assumption and refreshes the list', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse({ ...currentAssumption }, 201))
      .mockResolvedValueOnce(jsonResponse([currentAssumption]));
    vi.stubGlobal('fetch', fetchMock);

    render(<PlanningAssumptionsPage householdId={HOUSEHOLD_ID} />);
    expect(await screen.findByText(/no planning assumptions are recorded/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Add assumption' }));
    await userEvent.type(screen.getByLabelText('Name'), 'Inflation rate');
    await userEvent.type(screen.getByLabelText('Value type'), 'percentage');
    await userEvent.type(screen.getByLabelText('Value'), '3.5');
    await userEvent.type(screen.getByLabelText('Effective from'), '2026-01-01');
    await userEvent.type(screen.getByLabelText('Review date'), '2027-01-01');
    await userEvent.click(screen.getByRole('button', { name: 'Add assumption' }));

    expect(await screen.findByRole('region', { name: 'Planning assumptions, scrollable' })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });
});
