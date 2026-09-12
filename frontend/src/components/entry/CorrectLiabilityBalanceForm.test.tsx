import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CorrectLiabilityBalanceForm } from './CorrectLiabilityBalanceForm';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';
const LIABILITY_ID = '3d333333-3333-3333-3333-333333333333';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const currentLiability = {
  id: LIABILITY_ID,
  householdId: HOUSEHOLD_ID,
  name: 'Mortgage',
  liabilityType: 'MORTGAGE',
  outstandingBalance: 500000,
  currency: 'PHP',
  balanceAsOf: '2026-08-01',
  sourceType: 'MANUAL_ENTRY',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
  revision: 2,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('CorrectLiabilityBalanceForm', () => {
  it('fetches the current liability (including revision) on open, pre-fills the form, and submits it as expectedRevision', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith(`/liabilities/${LIABILITY_ID}`)) return Promise.resolve(jsonResponse(currentLiability));
      return Promise.resolve(jsonResponse({ ...currentLiability, outstandingBalance: 480000, revision: 3 }, 201));
    });
    vi.stubGlobal('fetch', fetchMock);
    const onCorrected = vi.fn();

    render(
      <CorrectLiabilityBalanceForm
        householdId={HOUSEHOLD_ID}
        liabilityId={LIABILITY_ID}
        liabilityName="Mortgage"
        onCorrected={onCorrected}
      />
    );

    await userEvent.click(screen.getByRole('button', { name: 'Correct' }));

    expect(await screen.findByLabelText('Outstanding balance')).toHaveValue('500000');
    expect(screen.getByLabelText('Balance as of')).toHaveValue('2026-08-01');

    await userEvent.clear(screen.getByLabelText('Outstanding balance'));
    await userEvent.type(screen.getByLabelText('Outstanding balance'), '480000.00');
    await userEvent.type(screen.getByLabelText('Reason for correction'), 'Statement paid down principal');
    await userEvent.click(screen.getByRole('button', { name: 'Save correction' }));

    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/households/${HOUSEHOLD_ID}/liabilities/${LIABILITY_ID}/balances`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          outstandingBalance: '480000.00',
          balanceAsOf: '2026-08-01',
          reason: 'Statement paid down principal',
          expectedRevision: 2,
        }),
      })
    );
    expect(onCorrected).toHaveBeenCalledTimes(1);
  });

  it('distinguishes a stale-revision conflict (409) from a validation error and offers a reload path', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(currentLiability))
      .mockResolvedValueOnce(
        jsonResponse({ error: 'LIABILITY_REVISION_CONFLICT', message: 'Liability revision has changed.', details: [] }, 409)
      );
    vi.stubGlobal('fetch', fetchMock);

    render(
      <CorrectLiabilityBalanceForm
        householdId={HOUSEHOLD_ID}
        liabilityId={LIABILITY_ID}
        liabilityName="Mortgage"
        onCorrected={vi.fn()}
      />
    );
    await userEvent.click(screen.getByRole('button', { name: 'Correct' }));
    await screen.findByLabelText('Outstanding balance');
    await userEvent.type(screen.getByLabelText('Reason for correction'), 'Retry after conflict');
    await userEvent.click(screen.getByRole('button', { name: 'Save correction' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/changed since it was loaded/i);
    expect(screen.getByRole('button', { name: 'Reload current value' })).toBeInTheDocument();
  });
});
