import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchPlanningAssumptions, HouseholdNotFoundError } from '../api/client';
import type { PlanningAssumption } from '../api/types';

export type PlanningAssumptionsState =
  | { status: 'loading' }
  | { status: 'not-found'; householdId: string }
  | { status: 'error'; message: string }
  | {
      status: 'ready';
      data: PlanningAssumption[];
      refreshing: boolean;
      refreshError?: { message: string; failedAt: Date };
    };

/**
 * Loads and refreshes a household's planning assumptions (current and
 * superseded). Race safety and refresh-failure retention match
 * `useFinancialPosition` exactly — see that hook's doc comment for the
 * invariants this mirrors.
 */
export function usePlanningAssumptions(householdId: string) {
  const [state, setState] = useState<PlanningAssumptionsState>({ status: 'loading' });
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
        const data = await fetchPlanningAssumptions(householdId, controller.signal);
        if (requestSeqRef.current !== seq) return; // superseded by a newer request
        setState({ status: 'ready', data, refreshing: false });
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
