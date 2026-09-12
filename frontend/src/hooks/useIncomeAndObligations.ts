import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchIncomeStreams, fetchObligations, HouseholdNotFoundError } from '../api/client';
import type { IncomeStream, Obligation } from '../api/types';

export interface IncomeAndObligationsData {
  incomeStreams: IncomeStream[];
  obligations: Obligation[];
}

export type IncomeAndObligationsState =
  | { status: 'loading' }
  | { status: 'not-found'; householdId: string }
  | { status: 'error'; message: string }
  | {
      status: 'ready';
      data: IncomeAndObligationsData;
      refreshing: boolean;
      refreshError?: { message: string; failedAt: Date };
    };

/**
 * Loads and refreshes a household's income streams and recurring
 * obligations together, following the same race-safety and
 * refresh-failure-retention conventions as `useFinancialPosition`: only the
 * most recently started request can ever update state, and a failed refresh
 * keeps the last successful `data` in place rather than clearing the screen.
 */
export function useIncomeAndObligations(householdId: string) {
  const [state, setState] = useState<IncomeAndObligationsState>({ status: 'loading' });
  const requestSeqRef = useRef(0);
  const abortRef = useRef<AbortController | null>(null);

  const load = useCallback(
    async (mode: 'initial' | 'refresh') => {
      const seq = ++requestSeqRef.current;
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      if (mode === 'refresh') {
        setState((prev) => (prev.status === 'ready' ? { ...prev, refreshing: true } : prev));
      } else {
        setState({ status: 'loading' });
      }

      try {
        const [incomeStreams, obligations] = await Promise.all([
          fetchIncomeStreams(householdId, controller.signal),
          fetchObligations(householdId, controller.signal),
        ]);
        if (requestSeqRef.current !== seq) return; // superseded by a newer request
        setState({ status: 'ready', data: { incomeStreams, obligations }, refreshing: false });
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return;
        if (requestSeqRef.current !== seq) return; // superseded by a newer request

        const message = error instanceof Error ? error.message : 'Could not reach the server.';
        setState((prev) => {
          if (error instanceof HouseholdNotFoundError) {
            return { status: 'not-found', householdId };
          }
          if (prev.status === 'ready') {
            return { ...prev, refreshing: false, refreshError: { message, failedAt: new Date() } };
          }
          return { status: 'error', message };
        });
      }
    },
    [householdId]
  );

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect
    load('initial');
    return () => abortRef.current?.abort();
  }, [load]);

  const refresh = useCallback(() => {
    void load('refresh');
  }, [load]);

  const retryInitial = useCallback(() => {
    void load('initial');
  }, [load]);

  return { state, refresh, retryInitial };
}
