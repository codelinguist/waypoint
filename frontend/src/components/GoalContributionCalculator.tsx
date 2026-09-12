import { useId, useState, type FormEvent } from 'react';
import { useGoalContributionCalculator } from '../hooks/useGoalContributionCalculator';
import { formatMoneyNumber } from '../moneyNumber';
import { goalContributionStatusLabel } from '../labels';
import type { FinancialGoal } from '../api/types';

export function GoalContributionCalculator({ goals }: { goals: FinancialGoal[] }) {
  const { state, submit } = useGoalContributionCalculator();
  const [selectedGoalId, setSelectedGoalId] = useState('');
  const [currency, setCurrency] = useState('');
  const [targetAmount, setTargetAmount] = useState('');
  const [currentAmount, setCurrentAmount] = useState('');
  const [contributionMonths, setContributionMonths] = useState('');
  const formId = useId();

  function applyGoal(goalId: string) {
    setSelectedGoalId(goalId);
    const goal = goals.find((candidate) => candidate.id === goalId);
    if (goal) {
      setCurrency(goal.currency);
      setTargetAmount(String(goal.targetAmount));
      setCurrentAmount(String(goal.currentAmount));
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    void submit({
      currency: currency.trim(),
      targetAmount: targetAmount.trim(),
      currentAmount: currentAmount.trim(),
      contributionMonths: Number(contributionMonths),
    });
  }

  return (
    <section className="card" aria-labelledby={`${formId}-heading`}>
      <h2 id={`${formId}-heading`}>Contribution calculator</h2>
      <p className="note">
        Computes the equal monthly contribution needed to close a goal&apos;s gap. Assumes zero growth, fees, and
        withdrawals — a modeling convention, not a recommendation.
      </p>
      <form onSubmit={handleSubmit}>
        {goals.length > 0 && (
          <div className="field">
            <label htmlFor={`${formId}-goal`}>Prefill from goal</label>
            <select
              id={`${formId}-goal`}
              value={selectedGoalId}
              onChange={(event) => applyGoal(event.target.value)}
            >
              <option value="">Enter values manually</option>
              {goals.map((goal) => (
                <option key={goal.id} value={goal.id}>
                  {goal.name}
                </option>
              ))}
            </select>
          </div>
        )}
        <div className="field">
          <label htmlFor={`${formId}-currency`}>Currency</label>
          <input
            id={`${formId}-currency`}
            type="text"
            required
            maxLength={3}
            value={currency}
            onChange={(event) => setCurrency(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-target`}>Target amount</label>
          <input
            id={`${formId}-target`}
            type="text"
            inputMode="decimal"
            required
            value={targetAmount}
            onChange={(event) => setTargetAmount(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-current`}>Current amount</label>
          <input
            id={`${formId}-current`}
            type="text"
            inputMode="decimal"
            required
            value={currentAmount}
            onChange={(event) => setCurrentAmount(event.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-months`}>Months to contribute</label>
          <input
            id={`${formId}-months`}
            type="number"
            min={1}
            max={1200}
            step={1}
            required
            value={contributionMonths}
            onChange={(event) => setContributionMonths(event.target.value)}
          />
        </div>
        <button className="refresh-btn" type="submit" disabled={state.status === 'submitting'}>
          {state.status === 'submitting' ? 'Calculating…' : 'Calculate'}
        </button>
      </form>

      {state.status === 'error' && (
        <div className="banner" role="alert">
          <strong>{state.message}</strong>
          {state.details.length > 0 && (
            <ul>
              {state.details.map((detail) => (
                <li key={detail}>{detail}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {state.status === 'result' && (
        <div className="result" aria-live="polite">
          <p>
            <strong>{goalContributionStatusLabel(state.data.status)}</strong>
          </p>
          <dl>
            <dt>Monthly contribution</dt>
            <dd>
              {formatMoneyNumber(state.data.monthlyContribution)} {state.data.currency}
            </dd>
            <dt>Remaining amount</dt>
            <dd>
              {formatMoneyNumber(state.data.remainingAmount)} {state.data.currency}
            </dd>
            <dt>Total contributions</dt>
            <dd>
              {formatMoneyNumber(state.data.totalContributions)} {state.data.currency}
            </dd>
            <dt>Projected amount</dt>
            <dd>
              {formatMoneyNumber(state.data.projectedAmount)} {state.data.currency}
            </dd>
            <dt>Amount above target</dt>
            <dd>
              {formatMoneyNumber(state.data.amountAboveTarget)} {state.data.currency}
            </dd>
          </dl>
        </div>
      )}
    </section>
  );
}
