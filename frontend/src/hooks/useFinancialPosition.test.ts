import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useFinancialPosition } from './useFinancialPosition';
import type { FinancialPositionResponse } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function response(householdName: string): FinancialPositionResponse {
  return {
    householdId: HOUSEHOLD_ID,
    householdName,
    baseCurrency: 'PHP',
    retrievedAt: '2026-09-10T06:15:22.104Z',
    assets: [],
    liabilities: [],
    totalsByCurrency: [],
  };
}

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useFinancialPosition race safety', () => {
  it('never lets an older, slower request overwrite a newer request already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest) // initial load, kept pending
      .mockImplementationOnce(() => Promise.resolve(jsonResponse(response('Newer Response'))));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useFinancialPosition(HOUSEHOLD_ID));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(result.current.state.status).toBe('loading');

    // Start a newer request (refresh) while the initial one is still pending.
    act(() => {
      result.current.refresh();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));

    // The newer request settles first.
    await waitFor(() => {
      expect(result.current.state.status).toBe('ready');
    });
    expect(result.current.state.status === 'ready' && result.current.state.data.householdName).toBe(
      'Newer Response'
    );

    // Now the older, superseded request finally resolves with different data.
    await act(async () => {
      resolveOlder(jsonResponse(response('Older Response')));
    });

    // The older response must never have applied.
    expect(result.current.state.status === 'ready' && result.current.state.data.householdName).toBe(
      'Newer Response'
    );
  });

  it('an older failure arriving after a newer success does not clobber the successful result', async () => {
    let rejectOlder: (reason: unknown) => void = () => {};
    const olderRequest = new Promise<Response>((_resolve, reject) => {
      rejectOlder = reject;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse(response('Newer Response'))));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useFinancialPosition(HOUSEHOLD_ID));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    act(() => {
      result.current.refresh();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.state.status).toBe('ready'));

    await act(async () => {
      rejectOlder(new TypeError('network down'));
    });

    expect(result.current.state.status).toBe('ready');
  });
});
