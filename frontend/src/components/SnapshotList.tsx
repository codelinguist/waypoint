import { formatInstantUtc, formatLocalDate } from '../dates';
import { sourceTypeLabel } from '../labels';
import { formatSignedSnapshotAmount } from '../snapshotMoney';
import type { FinancialSnapshot } from '../api/types';

export function SnapshotList({ snapshots }: { snapshots: FinancialSnapshot[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label="Financial snapshots, scrollable">
      <table className="stackable">
        <caption className="sr-only">Financial snapshots</caption>
        <thead>
          <tr>
            <th scope="col">As of</th>
            <th scope="col">Captured</th>
            <th scope="col">Source</th>
            <th scope="col">Net worth by currency</th>
          </tr>
        </thead>
        <tbody>
          {snapshots.map((snapshot) => (
            <tr key={snapshot.id}>
              <td className="record-title" data-label="As of">
                <time dateTime={snapshot.asOfDate}>{formatLocalDate(snapshot.asOfDate)}</time>
              </td>
              <td data-label="Captured">
                <time dateTime={snapshot.capturedAt}>{formatInstantUtc(snapshot.capturedAt)}</time>
              </td>
              <td data-label="Source">
                <span className="badge">{sourceTypeLabel(snapshot.sourceType)}</span>
              </td>
              <td data-label="Net worth by currency">
                {snapshot.totalsByCurrency.length === 0 ? (
                  <span className="badge">no records</span>
                ) : (
                  <div className="net-worth-list">
                    {snapshot.totalsByCurrency.map((totals) => {
                      const netWorth = formatSignedSnapshotAmount(totals.netWorth);
                      return (
                        <div key={totals.currency} className={`amount ${netWorth.negative ? 'negative' : 'positive'}`}>
                          {netWorth.formatted} {totals.currency}
                        </div>
                      );
                    })}
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
