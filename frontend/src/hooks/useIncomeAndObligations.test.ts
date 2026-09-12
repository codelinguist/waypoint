import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useIncomeAndObligations } from './useIncomeAndObligations';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useIncomeAndObligations race safety', () => {
  it('never lets an older, slower request pair overwrite a newer request pair already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });

    // Each `load` call fires two fetches (income-streams, obligations).
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest) // initial load's income-streams call, kept pending
      .mockImplementationOnce(() => olderRequest) // initial load's obligations call, kept pending
      .mockImplementation(() => Promise.resolve(jsonResponse([])));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useIncomeAndObligations(HOUSEHOLD_ID));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(result.current.state.status).toBe('loading');

    act(() => {
      result.current.refresh();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(4));
    await waitFor(() => expect(result.current.state.status).toBe('ready'));

    // The older, superseded pair finally resolves.
    await act(async () => {
      resolveOlder(jsonResponse([{ id: 'stale' }]));
    });

    // Still reflects the newer (empty) result, not the stale payload.
    expect(result.current.state.status === 'ready' && result.current.state.data.incomeStreams).toEqual([]);
  });

  it('an older failure arriving after a newer success does not clobber the successful result', async () => {
    let rejectOlder: (reason: unknown) => void = () => {};
    const olderRequest = new Promise<Response>((_resolve, reject) => {
      rejectOlder = reject;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => olderRequest)
      .mockImplementation(() => Promise.resolve(jsonResponse([])));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useIncomeAndObligations(HOUSEHOLD_ID));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));

    act(() => {
      result.current.refresh();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(4));
    await waitFor(() => expect(result.current.state.status).toBe('ready'));

    await act(async () => {
      rejectOlder(new TypeError('network down'));
    });

    expect(result.current.state.status).toBe('ready');
  });

  it('reports not-found when the household does not exist', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse({ error: 'HOUSEHOLD_NOT_FOUND', message: 'nope', details: [] }, 404)));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useIncomeAndObligations(HOUSEHOLD_ID));

    await waitFor(() => expect(result.current.state.status).toBe('not-found'));
    expect(result.current.state.status === 'not-found' && result.current.state.householdId).toBe(HOUSEHOLD_ID);
  });
});
