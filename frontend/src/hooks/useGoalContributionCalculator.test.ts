import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useGoalContributionCalculator } from './useGoalContributionCalculator';
import type { GoalContributionRequestBody, GoalContributionResult } from '../api/types';

const REQUEST: GoalContributionRequestBody = {
  currency: 'PHP',
  targetAmount: '1000.00',
  currentAmount: '100.00',
  contributionMonths: 3,
};

function result(monthlyContribution: number): GoalContributionResult {
  return {
    currency: 'PHP',
    targetAmount: 1000,
    currentAmount: 100,
    contributionMonths: 3,
    remainingAmount: 900,
    monthlyContribution,
    totalContributions: 900,
    projectedAmount: 1000,
    amountAboveTarget: 0,
    status: 'CONTRIBUTIONS_REQUIRED',
  };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('useGoalContributionCalculator', () => {
  it('starts idle and transitions to a result on success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(result(300))));

    const { result: hookResult } = renderHook(() => useGoalContributionCalculator());
    expect(hookResult.current.state.status).toBe('idle');

    await act(async () => {
      await hookResult.current.submit(REQUEST);
    });

    expect(hookResult.current.state.status).toBe('result');
    expect(hookResult.current.state.status === 'result' && hookResult.current.state.data.monthlyContribution).toBe(
      300
    );
  });

  it('surfaces a validation error with its field-level details', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed',
            details: ['targetAmount: targetAmount must be greater than zero'],
          },
          400
        )
      )
    );

    const { result: hookResult } = renderHook(() => useGoalContributionCalculator());

    await act(async () => {
      await hookResult.current.submit({ ...REQUEST, targetAmount: '0' });
    });

    expect(hookResult.current.state.status).toBe('error');
    expect(hookResult.current.state.status === 'error' && hookResult.current.state.message).toBe(
      'Request validation failed'
    );
    expect(hookResult.current.state.status === 'error' && hookResult.current.state.details).toEqual([
      'targetAmount: targetAmount must be greater than zero',
    ]);
  });

  it('never lets an older, slower submission overwrite a newer one already applied', async () => {
    let resolveOlder: (value: Response) => void = () => {};
    const olderRequest = new Promise<Response>((resolve) => {
      resolveOlder = resolve;
    });

    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() => olderRequest)
      .mockImplementationOnce(() => Promise.resolve(jsonResponse(result(500))));
    vi.stubGlobal('fetch', fetchMock);

    const { result: hookResult } = renderHook(() => useGoalContributionCalculator());

    act(() => {
      void hookResult.current.submit(REQUEST);
    });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(hookResult.current.state.status).toBe('submitting');

    await act(async () => {
      await hookResult.current.submit({ ...REQUEST, contributionMonths: 2 });
    });

    expect(hookResult.current.state.status).toBe('result');
    expect(hookResult.current.state.status === 'result' && hookResult.current.state.data.monthlyContribution).toBe(
      500
    );

    await act(async () => {
      resolveOlder(jsonResponse(result(999)));
    });

    expect(hookResult.current.state.status === 'result' && hookResult.current.state.data.monthlyContribution).toBe(
      500
    );
  });
});
