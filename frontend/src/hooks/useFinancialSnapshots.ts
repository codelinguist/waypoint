import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchFinancialSnapshots, HouseholdNotFoundError } from '../api/client';
import type { FinancialSnapshot } from '../api/types';

export type FinancialSnapshotsState =
  | { status: 'loading' }
  | { status: 'not-found'; householdId: string }
  | { status: 'error'; message: string }
  | { status: 'ready'; snapshots: FinancialSnapshot[] };

/**
 * Loads a household's recorded financial snapshots.
 *
 * Race safety mirrors useFinancialPosition.ts: only the response belonging
 * to the most recently started request is ever applied to state.
 */
export function useFinancialSnapshots(householdId: string) {
  const [state, setState] = useState<FinancialSnapshotsState>({ status: 'loading' });
  const requestSeqRef = useRef(0);
  const abortRef = useRef<AbortController | null>(null);

  const load = useCallback(async () => {
    const seq = ++requestSeqRef.current;
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setState({ status: 'loading' });

    try {
      const snapshots = await fetchFinancialSnapshots(householdId, controller.signal);
      if (requestSeqRef.current !== seq) return; // superseded by a newer request
      setState({ status: 'ready', snapshots });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      if (requestSeqRef.current !== seq) return; // superseded by a newer request

      if (error instanceof HouseholdNotFoundError) {
        setState({ status: 'not-found', householdId });
        return;
      }
      const message = error instanceof Error ? error.message : 'Could not reach the server.';
      setState({ status: 'error', message });
    }
  }, [householdId]);

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect
    load();
    return () => abortRef.current?.abort();
  }, [load]);

  const retry = useCallback(() => {
    void load();
  }, [load]);

  return { state, retry };
}
