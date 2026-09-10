import { formatLocalDate } from '../dates';
import { liabilityTypeLabel, sourceTypeLabel } from '../labels';
import { formatMoneyMagnitude } from '../money';
import type { PositionLiability } from '../api/types';

export function LiabilityTable({ currency, liabilities }: { currency: string; liabilities: PositionLiability[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label={`${currency} liabilities, scrollable`}>
      <table className="stackable">
        <caption className="sr-only">{currency} liabilities</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Type</th>
            <th scope="col" className="amount">
              Outstanding balance
            </th>
            <th scope="col">Balance as of</th>
            <th scope="col">Source</th>
          </tr>
        </thead>
        <tbody>
          {liabilities.map((liability) => (
            <tr key={liability.id}>
              <td className="record-title">{liability.name}</td>
              <td data-label="Type">
                <span className="badge">{liabilityTypeLabel(liability.liabilityType)}</span>
              </td>
              <td className="amount" data-label="Outstanding balance">
                {formatMoneyMagnitude(liability.outstandingBalance)}
              </td>
              <td data-label="Balance as of">
                <time dateTime={liability.balanceAsOf}>{formatLocalDate(liability.balanceAsOf)}</time>
              </td>
              <td data-label="Source">
                <span className="badge">{sourceTypeLabel(liability.sourceType)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
