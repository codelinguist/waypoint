import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useGoals } from './useGoals';
import type { FinancialGoal } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function goal(name: string): FinancialGoal {
  return {
    id: '1b111111-1111-1111-1111-111111111111',
    householdId: HOUSEHOLD_ID,
    name,
    targetAmount: 1000,
    currency: 'PHP',
    targetDate: '2027-01-01',
    priority: 1,
    currentAmount: 100,
    remainingAmount: 900,
    progressPercentage: 10,
    sourceType: 'MANUAL_ENTRY',
    createdAt: '2026-09-10T06:15:22.104Z',
    updatedAt: '2026-09-10T06:15:22.104Z',
  };
}

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useGoals race safety', () => {
  it('never lets an older, slower request overwrite a newer request already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse([goal('Newer Goal')])));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useGoals(HOUSEHOLD_ID));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(result.current.state.status).toBe('loading');

    act(() => {
      result.current.refresh();
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));

    await waitFor(() => {
      expect(result.current.state.status).toBe('ready');
    });
    expect(result.current.state.status === 'ready' && result.current.state.data[0].name).toBe('Newer Goal');

    await act(async () => {
      resolveOlder(jsonResponse([goal('Older Goal')]));
    });

    expect(result.current.state.status === 'ready' && result.current.state.data[0].name).toBe('Newer Goal');
  });

  it('an older failure arriving after a newer success does not clobber the successful result', async () => {
    let rejectOlder: (reason: unknown) => void = () => {};
    const olderRequest = new Promise<Response>((_resolve, reject) => {
      rejectOlder = reject;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse([goal('Newer Goal')])));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useGoals(HOUSEHOLD_ID));
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
