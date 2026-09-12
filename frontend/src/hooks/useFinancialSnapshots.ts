import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchFinancialSnapshots, HouseholdNotFoundError } from '../api/client';
import type { FinancialSnapshotListItem } from '../api/types';

export type FinancialSnapshotsState =
  | { status: 'loading' }
  | { status: 'not-found'; householdId: string }
  | { status: 'error'; message: string }
  | { status: 'ready'; snapshots: FinancialSnapshotListItem[] };

/**
 * Loads the household's recorded financial snapshots, for the plan-vs-actual
 * snapshot picker. Race safety mirrors useFinancialPosition: only the
 * response belonging to the most recently started request is ever applied to
 * state, so a slow superseded retry can never overwrite a newer one's result.
 */
export function useFinancialSnapshots(householdId: string) {
  const [state, setState] = useState<FinancialSnapshotsState>({ status: 'loading' });
  const requestSeqRef = useRef(0);

  const load = useCallback(async () => {
    const seq = ++requestSeqRef.current;
    setState({ status: 'loading' });

    try {
      const snapshots = await fetchFinancialSnapshots(householdId);
      if (requestSeqRef.current !== seq) return; // superseded by a newer request
      setState({ status: 'ready', snapshots });
    } catch (error) {
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
    void load();
  }, [load]);

  const retry = useCallback(() => {
    void load();
  }, [load]);

  return { state, retry };
}
