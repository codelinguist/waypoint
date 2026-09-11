import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useFinancialSnapshots } from './useFinancialSnapshots';
import type { FinancialSnapshot } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function snapshot(id: string, asOfDate: string): FinancialSnapshot {
  return {
    id,
    householdId: HOUSEHOLD_ID,
    asOfDate,
    capturedAt: '2026-09-10T06:15:22.104Z',
    sourceType: 'MANUAL_ENTRY',
    assetLineItems: [],
    liabilityLineItems: [],
    totalsByCurrency: [],
  };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useFinancialSnapshots', () => {
  it('loads the snapshot list', async () => {
    const snapshots = [snapshot('11111111-1111-1111-1111-111111111111', '2026-08-01')];
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse(snapshots))
    );

    const { result } = renderHook(() => useFinancialSnapshots(HOUSEHOLD_ID));

    await waitFor(() => expect(result.current.state.status).toBe('ready'));
    expect(result.current.state.status === 'ready' && result.current.state.snapshots).toEqual(snapshots);
  });

  it('reports a not-found household distinctly from a generic error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 404)));

    const { result } = renderHook(() => useFinancialSnapshots(HOUSEHOLD_ID));

    await waitFor(() => expect(result.current.state.status).toBe('not-found'));
  });

  it('never lets an older, slower request overwrite a newer request already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });
    const newer = [snapshot('22222222-2222-2222-2222-222222222222', '2026-09-01')];

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse(newer)));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useFinancialSnapshots(HOUSEHOLD_ID));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(result.current.state.status).toBe('loading');

    act(() => {
      result.current.retry();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.state.status).toBe('ready'));

    await act(async () => {
      resolveOlder(jsonResponse([snapshot('33333333-3333-3333-3333-333333333333', '2026-01-01')]));
    });

    expect(result.current.state.status === 'ready' && result.current.state.snapshots).toEqual(newer);
  });
});
