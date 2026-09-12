import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AddAssetForm } from './AddAssetForm';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

async function openForm() {
  await userEvent.click(screen.getByRole('button', { name: 'Add asset' }));
}

async function fillValidForm() {
  await userEvent.type(screen.getByLabelText('Name'), 'Emergency Cash');
  await userEvent.type(screen.getByLabelText('Currency'), 'php');
  await userEvent.type(screen.getByLabelText('Estimated value'), '10000.00');
  await userEvent.type(screen.getByLabelText('Planning value'), '10000.00');
  await userEvent.type(screen.getByLabelText('Valued as of'), '2026-09-01');
}

describe('AddAssetForm', () => {
  it('submits the exact request shape the backend expects, uppercases currency, then closes and refreshes on success', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          id: 'new-asset-id',
          householdId: HOUSEHOLD_ID,
          name: 'Emergency Cash',
          assetType: 'CASH',
          estimatedValue: 10000,
          planningValue: 10000,
          currency: 'PHP',
          valuedAt: '2026-09-01',
          liquidity: 'LIQUID',
          sourceType: 'MANUAL_ENTRY',
          createdAt: '2026-09-12T00:00:00Z',
          updatedAt: '2026-09-12T00:00:00Z',
        },
        201
      )
    );
    vi.stubGlobal('fetch', fetchMock);
    const onCreated = vi.fn();

    render(<AddAssetForm householdId={HOUSEHOLD_ID} onCreated={onCreated} />);
    await openForm();
    await fillValidForm();
    await userEvent.click(screen.getByRole('button', { name: 'Add asset' }));

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/households/${HOUSEHOLD_ID}/assets`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          name: 'Emergency Cash',
          assetType: 'CASH',
          estimatedValue: '10000.00',
          planningValue: '10000.00',
          currency: 'PHP',
          valuedAt: '2026-09-01',
          liquidity: 'LIQUID',
        }),
      })
    );

    expect(await screen.findByRole('button', { name: 'Add asset' })).toBeInTheDocument();
    expect(onCreated).toHaveBeenCalledTimes(1);
    // The form collapsed back to closed — its fields are unmounted again.
    expect(screen.queryByLabelText('Name')).not.toBeInTheDocument();
  });

  it("surfaces the backend's own validation details instead of failing silently, and keeps the form open with its values intact", async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed',
            details: ['planningValue: planningValue must not exceed estimatedValue'],
          },
          400
        )
      )
    );
    const onCreated = vi.fn();

    render(<AddAssetForm householdId={HOUSEHOLD_ID} onCreated={onCreated} />);
    await openForm();
    await fillValidForm();
    await userEvent.click(screen.getByRole('button', { name: 'Add asset' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/planningValue must not exceed estimatedValue/i);
    expect(onCreated).not.toHaveBeenCalled();
    // Form stays open and populated so the household can fix and resubmit.
    expect(screen.getByLabelText('Name')).toHaveValue('Emergency Cash');
  });
});
