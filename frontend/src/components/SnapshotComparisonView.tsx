import { useId, useState } from 'react';
import type { FormEvent } from 'react';
import { useSnapshotComparison } from '../hooks/useSnapshotComparison';
import { formatLocalDate } from '../dates';
import { formatSignedSnapshotAmount } from '../snapshotMoney';
import type { FinancialSnapshot } from '../api/types';

function SnapshotOptions({ snapshots }: { snapshots: FinancialSnapshot[] }) {
  return (
    <>
      <option value="" disabled>
        Select a snapshot
      </option>
      {snapshots.map((snapshot) => (
        <option key={snapshot.id} value={snapshot.id}>
          {formatLocalDate(snapshot.asOfDate)}
        </option>
      ))}
    </>
  );
}

/**
 * Lets the household pick an earlier and later snapshot and run a
 * comparison. The two selects are labeled "Earlier"/"Later" rather than by
 * position (first/second) so the comparison direction is never ambiguous —
 * per the ticket's risk note, the API distinguishes earlierSnapshotId from
 * laterSnapshotId explicitly and the UI should too.
 *
 * Comparing a snapshot against itself is not blocked client-side: the
 * ticket's acceptance criteria call for the backend's
 * IdenticalSnapshotComparisonException to be surfaced, so the request is
 * allowed to go through and its error is shown below.
 */
export function SnapshotComparisonView({
  householdId,
  snapshots,
}: {
  householdId: string;
  snapshots: FinancialSnapshot[];
}) {
  const { state, compare } = useSnapshotComparison(householdId);
  const [earlierId, setEarlierId] = useState('');
  const [laterId, setLaterId] = useState('');
  const earlierSelectId = useId();
  const laterSelectId = useId();
  const canCompare = earlierId !== '' && laterId !== '';

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!canCompare) return;
    void compare(earlierId, laterId);
  }

  return (
    <section className="card" aria-labelledby="snapshot-comparison-heading">
      <h3 id="snapshot-comparison-heading">Compare snapshots</h3>
      <form className="comparison-controls" onSubmit={handleSubmit}>
        <label htmlFor={earlierSelectId}>
          Earlier snapshot
          <select id={earlierSelectId} value={earlierId} onChange={(event) => setEarlierId(event.target.value)}>
            <SnapshotOptions snapshots={snapshots} />
          </select>
        </label>
        <label htmlFor={laterSelectId}>
          Later snapshot
          <select id={laterSelectId} value={laterId} onChange={(event) => setLaterId(event.target.value)}>
            <SnapshotOptions snapshots={snapshots} />
          </select>
        </label>
        <button className="refresh-btn" type="submit" disabled={!canCompare || state.status === 'loading'}>
          {state.status === 'loading' ? 'Comparing…' : 'Compare'}
        </button>
      </form>

      {state.status === 'error' && (
        <div className="banner" role="alert">
          <strong>Could not compare these snapshots.</strong>
          {state.message}
        </div>
      )}

      {state.status === 'ready' && (
        <div className="comparison-result">
          <p className="status-line">
            Comparing <time dateTime={state.data.earlierSnapshot.asOfDate}>{formatLocalDate(state.data.earlierSnapshot.asOfDate)}</time>{' '}
            to <time dateTime={state.data.laterSnapshot.asOfDate}>{formatLocalDate(state.data.laterSnapshot.asOfDate)}</time>
          </p>
          {state.data.currencyDeltas.length === 0 ? (
            <p className="no-records-side">No currencies to compare between these snapshots.</p>
          ) : (
            <div className="table-scroll" tabIndex={0} role="region" aria-label="Per-currency deltas, scrollable">
              <table className="stackable">
                <caption className="sr-only">Per-currency deltas</caption>
                <thead>
                  <tr>
                    <th scope="col">Currency</th>
                    <th scope="col" className="amount">
                      Asset total change
                    </th>
                    <th scope="col" className="amount">
                      Liability total change
                    </th>
                    <th scope="col" className="amount">
                      Net worth change
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {state.data.currencyDeltas.map((delta) => {
                    const assetDelta = formatSignedSnapshotAmount(delta.assetTotalDelta);
                    const liabilityDelta = formatSignedSnapshotAmount(delta.liabilityTotalDelta);
                    const netWorthDelta = formatSignedSnapshotAmount(delta.netWorthDelta);
                    return (
                      <tr key={delta.currency}>
                        <td className="record-title">{delta.currency}</td>
                        <td className={`amount ${assetDelta.negative ? 'negative' : 'positive'}`} data-label="Asset total change">
                          {assetDelta.formatted}
                        </td>
                        <td
                          className={`amount ${liabilityDelta.negative ? 'negative' : 'positive'}`}
                          data-label="Liability total change"
                        >
                          {liabilityDelta.formatted}
                        </td>
                        <td className={`amount ${netWorthDelta.negative ? 'negative' : 'positive'}`} data-label="Net worth change">
                          {netWorthDelta.formatted}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
