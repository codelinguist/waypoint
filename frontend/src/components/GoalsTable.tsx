import { formatLocalDate } from '../dates';
import { formatMoneyNumber, formatPercent } from '../moneyNumber';
import type { FinancialGoal } from '../api/types';

export function GoalsTable({ goals }: { goals: FinancialGoal[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label="Goals, scrollable">
      <table className="stackable">
        <caption className="sr-only">Financial goals</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Priority</th>
            <th scope="col">Target date</th>
            <th scope="col" className="amount">
              Target
            </th>
            <th scope="col" className="amount">
              Current
            </th>
            <th scope="col" className="amount">
              Remaining
            </th>
            <th scope="col" className="amount">
              Progress
            </th>
          </tr>
        </thead>
        <tbody>
          {goals.map((goal) => (
            <tr key={goal.id}>
              <td className="record-title">{goal.name}</td>
              <td data-label="Priority">{goal.priority}</td>
              <td data-label="Target date">
                <time dateTime={goal.targetDate}>{formatLocalDate(goal.targetDate)}</time>
              </td>
              <td className="amount" data-label="Target">
                {formatMoneyNumber(goal.targetAmount)} {goal.currency}
              </td>
              <td className="amount" data-label="Current">
                {formatMoneyNumber(goal.currentAmount)} {goal.currency}
              </td>
              <td className="amount" data-label="Remaining">
                {formatMoneyNumber(goal.remainingAmount)} {goal.currency}
              </td>
              <td className="amount" data-label="Progress">
                {formatPercent(goal.progressPercentage)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
