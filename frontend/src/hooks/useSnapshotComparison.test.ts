import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useSnapshotComparison } from './useSnapshotComparison';
import type { FinancialSnapshotComparison } from '../api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';
const EARLIER_ID = '11111111-1111-1111-1111-111111111111';
const LATER_ID = '22222222-2222-2222-2222-222222222222';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const comparison: FinancialSnapshotComparison = {
  earlierSnapshot: { id: EARLIER_ID, asOfDate: '2026-01-01', capturedAt: '2026-01-02T00:00:00Z' },
  laterSnapshot: { id: LATER_ID, asOfDate: '2026-09-01', capturedAt: '2026-09-02T00:00:00Z' },
  currencyDeltas: [{ currency: 'PHP', assetTotalDelta: 500, liabilityTotalDelta: -100, netWorthDelta: 600 }],
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useSnapshotComparison', () => {
  it('starts idle and does not call the API until compare is invoked', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useSnapshotComparison(HOUSEHOLD_ID));

    expect(result.current.state.status).toBe('idle');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('runs a comparison and exposes the result', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(comparison)));

    const { result } = renderHook(() => useSnapshotComparison(HOUSEHOLD_ID));

    await act(async () => {
      await result.current.compare(EARLIER_ID, LATER_ID);
    });

    expect(result.current.state).toEqual({ status: 'ready', data: comparison });
  });

  it('surfaces the identical-snapshot comparison error rather than failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            error: 'VALIDATION_FAILED',
            message: `Cannot compare a financial snapshot against itself: ${EARLIER_ID}`,
            details: [],
          },
          400
        )
      )
    );

    const { result } = renderHook(() => useSnapshotComparison(HOUSEHOLD_ID));

    await act(async () => {
      await result.current.compare(EARLIER_ID, EARLIER_ID);
    });

    expect(result.current.state.status).toBe('error');
    expect(result.current.state.status === 'error' && result.current.state.message).toMatch(/cannot compare.*itself/i);
  });

  it('never lets an older, slower comparison overwrite a newer one already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse(comparison)));
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useSnapshotComparison(HOUSEHOLD_ID));

    let firstCall!: Promise<void>;
    act(() => {
      firstCall = result.current.compare(EARLIER_ID, LATER_ID);
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    await act(async () => {
      await result.current.compare(EARLIER_ID, LATER_ID);
    });
    await waitFor(() => expect(result.current.state.status).toBe('ready'));

    await act(async () => {
      resolveOlder(
        jsonResponse({
          earlierSnapshot: { id: EARLIER_ID, asOfDate: '2020-01-01', capturedAt: '2020-01-02T00:00:00Z' },
          laterSnapshot: { id: LATER_ID, asOfDate: '2020-09-01', capturedAt: '2020-09-02T00:00:00Z' },
          currencyDeltas: [],
        })
      );
      await firstCall;
    });

    expect(result.current.state).toEqual({ status: 'ready', data: comparison });
  });
});
