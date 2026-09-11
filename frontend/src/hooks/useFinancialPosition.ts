import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchFinancialPosition, HouseholdNotFoundError } from '../api/client';
import type { FinancialPositionResponse } from '../api/types';

export type FinancialPositionState =
  | { status: 'loading' }
  | { status: 'not-found'; householdId: string }
  | { status: 'error'; message: string }
  | {
      status: 'ready';
      data: FinancialPositionResponse;
      refreshing: boolean;
      refreshError?: { message: string; failedAt: Date };
    };

/**
 * Loads and refreshes a household's financial position.
 *
 * Race safety: only the response belonging to the most recently started
 * request is ever applied to state. An in-flight request superseded by a
 * newer one is aborted and its eventual settlement (success or failure) is
 * ignored, so a slow earlier response can never overwrite a newer one
 * ("An older response cannot replace a newer request's result").
 *
 * Refresh-failure retention: a failed refresh keeps the last successful
 * `data` in place (status stays "ready") and records `refreshError`
 * alongside it, rather than clearing the screen — matching the approved
 * design's refresh-failure state.
 */
export function useFinancialPosition(householdId: string) {
  const [state, setState] = useState<FinancialPositionState>({ status: 'loading' });
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
        const data = await fetchFinancialPosition(householdId, controller.signal);
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
    // Fetching on mount (React's documented data-fetching-in-an-effect
    // pattern) synchronously sets the "loading" status before the first
    // `await` inside `load`; that's the point of starting the request here.
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
