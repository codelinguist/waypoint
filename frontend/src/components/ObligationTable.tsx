import { formatLocalDate } from '../dates';
import { frequencyLabel, obligationTypeLabel, sourceTypeLabel } from '../labels';
import { formatMoneyNumber } from '../moneyNumber';
import type { Obligation } from '../api/types';

export function ObligationTable({ obligations }: { obligations: Obligation[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label="Recurring obligations, scrollable">
      <table className="stackable">
        <caption className="sr-only">Recurring obligations</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Type</th>
            <th scope="col" className="amount">
              Amount
            </th>
            <th scope="col">Frequency</th>
            <th scope="col">Start</th>
            <th scope="col">End</th>
            <th scope="col">Source</th>
          </tr>
        </thead>
        <tbody>
          {obligations.map((obligation) => (
            <tr key={obligation.id}>
              <td className="record-title">{obligation.name}</td>
              <td data-label="Type">
                <span className="badge">{obligationTypeLabel(obligation.obligationType)}</span>
              </td>
              <td className="amount" data-label="Amount">
                {formatMoneyNumber(obligation.amount)} {obligation.currency}
              </td>
              <td data-label="Frequency">{frequencyLabel(obligation.frequency)}</td>
              <td data-label="Start">
                <time dateTime={obligation.startDate}>{formatLocalDate(obligation.startDate)}</time>
              </td>
              <td data-label="End">
                {obligation.endDate ? (
                  <time dateTime={obligation.endDate}>{formatLocalDate(obligation.endDate)}</time>
                ) : (
                  'Ongoing'
                )}
              </td>
              <td data-label="Source">
                <span className="badge">{sourceTypeLabel(obligation.sourceType)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
