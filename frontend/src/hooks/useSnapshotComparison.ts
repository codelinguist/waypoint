import { useCallback, useRef, useState } from 'react';
import { fetchSnapshotComparison } from '../api/client';
import type { FinancialSnapshotComparison } from '../api/types';

export type SnapshotComparisonState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; data: FinancialSnapshotComparison }
  | { status: 'error'; message: string };

/**
 * Runs an on-demand comparison between two of a household's financial
 * snapshots. Unlike useFinancialSnapshots, nothing loads until `compare` is
 * called. Race safety: only the response belonging to the most recently
 * started comparison is ever applied to state, so rapidly re-submitting
 * cannot let a slower, earlier comparison overwrite a newer one.
 */
export function useSnapshotComparison(householdId: string) {
  const [state, setState] = useState<SnapshotComparisonState>({ status: 'idle' });
  const requestSeqRef = useRef(0);

  const compare = useCallback(
    async (earlierSnapshotId: string, laterSnapshotId: string) => {
      const seq = ++requestSeqRef.current;
      setState({ status: 'loading' });

      try {
        const data = await fetchSnapshotComparison(householdId, earlierSnapshotId, laterSnapshotId);
        if (requestSeqRef.current !== seq) return; // superseded by a newer request
        setState({ status: 'ready', data });
      } catch (error) {
        if (requestSeqRef.current !== seq) return; // superseded by a newer request
        const message = error instanceof Error ? error.message : 'Could not reach the server.';
        setState({ status: 'error', message });
      }
    },
    [householdId]
  );

  return { state, compare };
}
