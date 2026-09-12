import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CorrectAssetValuationForm } from './CorrectAssetValuationForm';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';
const ASSET_ID = '1b111111-1111-1111-1111-111111111111';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const currentValuation = {
  assetId: ASSET_ID,
  householdId: HOUSEHOLD_ID,
  estimatedValue: '10000.00',
  planningValue: '9000.00',
  currency: 'PHP',
  valuedAt: '2026-08-01',
  sourceType: 'MANUAL_ENTRY',
  revision: 3,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('CorrectAssetValuationForm', () => {
  it('fetches the current revision on open, pre-fills the form, and submits it as expectedRevision', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith('/valuations')) return Promise.resolve(jsonResponse(currentValuation));
      return Promise.resolve(jsonResponse({ ...currentValuation, estimatedValue: '11000.00', revision: 4 }));
    });
    vi.stubGlobal('fetch', fetchMock);
    const onCorrected = vi.fn();

    render(
      <CorrectAssetValuationForm householdId={HOUSEHOLD_ID} assetId={ASSET_ID} assetName="Urban Lot" onCorrected={onCorrected} />
    );

    await userEvent.click(screen.getByRole('button', { name: 'Correct' }));

    expect(await screen.findByLabelText('Estimated value')).toHaveValue('10000.00');
    expect(screen.getByLabelText('Planning value')).toHaveValue('9000.00');

    await userEvent.clear(screen.getByLabelText('Estimated value'));
    await userEvent.type(screen.getByLabelText('Estimated value'), '11000.00');
    await userEvent.type(screen.getByLabelText('Reason for correction'), 'Updated bank statement');
    await userEvent.click(screen.getByRole('button', { name: 'Save correction' }));

    expect(fetchMock).toHaveBeenLastCalledWith(
      `/api/households/${HOUSEHOLD_ID}/assets/${ASSET_ID}/valuations`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          estimatedValue: '11000.00',
          planningValue: '9000.00',
          valuedAt: '2026-08-01',
          reason: 'Updated bank statement',
          expectedRevision: 3,
        }),
      })
    );
    expect(onCorrected).toHaveBeenCalledTimes(1);
  });

  it('distinguishes a stale-revision conflict (409) from a validation error and offers a reload path', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(currentValuation))
      .mockResolvedValueOnce(
        jsonResponse({ error: 'ASSET_REVISION_CONFLICT', message: 'Asset revision has changed.', details: [] }, 409)
      );
    vi.stubGlobal('fetch', fetchMock);

    render(
      <CorrectAssetValuationForm householdId={HOUSEHOLD_ID} assetId={ASSET_ID} assetName="Urban Lot" onCorrected={vi.fn()} />
    );
    await userEvent.click(screen.getByRole('button', { name: 'Correct' }));
    await screen.findByLabelText('Estimated value');
    await userEvent.type(screen.getByLabelText('Reason for correction'), 'Retry after conflict');
    await userEvent.click(screen.getByRole('button', { name: 'Save correction' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/changed since it was loaded/i);
    expect(screen.getByRole('button', { name: 'Reload current value' })).toBeInTheDocument();
  });
});
