import { formatLocalDate } from '../dates';
import { certaintyLabel, compensationClassificationLabel, frequencyLabel, incomeTypeLabel, sourceTypeLabel } from '../labels';
import { formatMoneyNumber } from '../moneyNumber';
import type { IncomeStream } from '../api/types';

export function IncomeStreamTable({ incomeStreams }: { incomeStreams: IncomeStream[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label="Income streams, scrollable">
      <table className="stackable">
        <caption className="sr-only">Income streams</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Type</th>
            <th scope="col" className="amount">
              Amount
            </th>
            <th scope="col">Frequency</th>
            <th scope="col">Classification</th>
            <th scope="col">Certainty</th>
            <th scope="col">Start</th>
            <th scope="col">End</th>
            <th scope="col">Source</th>
          </tr>
        </thead>
        <tbody>
          {incomeStreams.map((incomeStream) => {
            const confirmed = incomeStream.certainty === 'CONFIRMED';
            return (
              <tr key={incomeStream.id}>
                <td className="record-title">{incomeStream.name}</td>
                <td data-label="Type">
                  <span className="badge">{incomeTypeLabel(incomeStream.incomeType)}</span>
                </td>
                <td className="amount" data-label="Amount">
                  {formatMoneyNumber(incomeStream.amount)} {incomeStream.currency}
                </td>
                <td data-label="Frequency">{frequencyLabel(incomeStream.frequency)}</td>
                <td data-label="Classification">
                  <span className="badge">{compensationClassificationLabel(incomeStream.compensationClassification)}</span>
                </td>
                <td data-label="Certainty">
                  <span className={`badge${confirmed ? '' : ' badge--attention'}`}>
                    {!confirmed && <span className="sr-only">Not confirmed — </span>}
                    {certaintyLabel(incomeStream.certainty)}
                  </span>
                </td>
                <td data-label="Start">
                  <time dateTime={incomeStream.startDate}>{formatLocalDate(incomeStream.startDate)}</time>
                </td>
                <td data-label="End">
                  {incomeStream.endDate ? (
                    <time dateTime={incomeStream.endDate}>{formatLocalDate(incomeStream.endDate)}</time>
                  ) : (
                    'Ongoing'
                  )}
                </td>
                <td data-label="Source">
                  <span className="badge">{sourceTypeLabel(incomeStream.sourceType)}</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
