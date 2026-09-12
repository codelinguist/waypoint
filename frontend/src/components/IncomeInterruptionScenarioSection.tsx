import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchIncomeInterruptionScenario } from '../api/client';
import type { IncomeInterruptionScenarioRequest, IncomeInterruptionScenarioResponse } from '../api/types';
import { formatAmountMagnitude, formatSignedAmount } from '../calculatorMoney';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';

const DEFAULTS = {
  currency: 'USD',
  openingReserve: '',
  normalMonthlyNetIncome: '',
  interruptedMonthlyNetIncome: '',
  monthlyExpenses: '',
  horizonMonths: '12',
  interruptionStartMonth: '1',
  interruptionMonths: '1',
};

export function IncomeInterruptionScenarioSection() {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useCalculatorSubmit(fetchIncomeInterruptionScenario);
  const headingId = useId();
  const errorId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: IncomeInterruptionScenarioRequest = {
      currency: form.currency.trim().toUpperCase(),
      openingReserve: form.openingReserve.trim(),
      normalMonthlyNetIncome: form.normalMonthlyNetIncome.trim(),
      interruptedMonthlyNetIncome: form.interruptedMonthlyNetIncome.trim(),
      monthlyExpenses: form.monthlyExpenses.trim(),
      horizonMonths: Number(form.horizonMonths),
      interruptionStartMonth: Number(form.interruptionStartMonth),
      interruptionMonths: Number(form.interruptionMonths),
    };
    void run(request);
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Income interruption</h2>
      <p className="note">
        Compares a normal-income baseline against a temporary income interruption you define below — a modeled
        what-if, not a forecast of an actual job loss or income change.
      </p>

      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="iis-currency">Currency</label>
          <input
            id="iis-currency"
            name="currency"
            type="text"
            required
            maxLength={3}
            placeholder="USD"
            value={form.currency}
            onChange={updateField('currency')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-opening-reserve">Opening reserve</label>
          <input
            id="iis-opening-reserve"
            name="openingReserve"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.openingReserve}
            onChange={updateField('openingReserve')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-normal-income">Normal monthly net income</label>
          <input
            id="iis-normal-income"
            name="normalMonthlyNetIncome"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.normalMonthlyNetIncome}
            onChange={updateField('normalMonthlyNetIncome')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-interrupted-income">Interrupted monthly net income</label>
          <input
            id="iis-interrupted-income"
            name="interruptedMonthlyNetIncome"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.interruptedMonthlyNetIncome}
            onChange={updateField('interruptedMonthlyNetIncome')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-monthly-expenses">Monthly expenses</label>
          <input
            id="iis-monthly-expenses"
            name="monthlyExpenses"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.monthlyExpenses}
            onChange={updateField('monthlyExpenses')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-horizon-months">Horizon (months)</label>
          <input
            id="iis-horizon-months"
            name="horizonMonths"
            type="number"
            min={1}
            max={1200}
            step={1}
            required
            value={form.horizonMonths}
            onChange={updateField('horizonMonths')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-interruption-start">Interruption starts at month</label>
          <input
            id="iis-interruption-start"
            name="interruptionStartMonth"
            type="number"
            min={1}
            max={1200}
            step={1}
            required
            value={form.interruptionStartMonth}
            onChange={updateField('interruptionStartMonth')}
          />
        </div>
        <div className="field">
          <label htmlFor="iis-interruption-months">Interruption length (months)</label>
          <input
            id="iis-interruption-months"
            name="interruptionMonths"
            type="number"
            min={1}
            max={1200}
            step={1}
            required
            value={form.interruptionMonths}
            onChange={updateField('interruptionMonths')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting} aria-describedby={errorId}>
          {submitting ? 'Calculating…' : 'Run interruption scenario'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <IncomeInterruptionScenarioResults response={state.data} />}
    </section>
  );
}

function SignedAmount({ value, suffix }: { value: number; suffix?: string }) {
  const signed = formatSignedAmount(value);
  return (
    <span className={signed.negative ? 'negative' : 'positive'}>
      <span className="sr-only">{signed.negative ? 'negative' : 'positive'}</span>
      {signed.formatted}
      {suffix ? ` ${suffix}` : ''}
    </span>
  );
}

function IncomeInterruptionScenarioResults({ response }: { response: IncomeInterruptionScenarioResponse }) {
  return (
    <div className="results">
      <div className="summary-grid">
        <div className="summary-item">
          Ending cash (scenario)
          <strong>
            <SignedAmount value={response.endingCash} suffix={response.currency} />
          </strong>
        </div>
        <div className="summary-item">
          Minimum cash (scenario)
          <strong>
            <SignedAmount value={response.minimumCash} suffix={response.currency} />
          </strong>
        </div>
        <div className="summary-item">
          First month below zero
          <strong>{response.firstNegativeMonth === null ? 'None' : `Month ${response.firstNegativeMonth}`}</strong>
        </div>
        <div className="summary-item">
          Additional reserve needed
          <strong>
            {formatAmountMagnitude(response.additionalOpeningReserveNeeded)} {response.currency}
          </strong>
        </div>
      </div>

      <div className="table-scroll" tabIndex={0} role="region" aria-label="Monthly income-interruption comparison, scrollable">
        <table className="stackable">
          <caption className="sr-only">Baseline vs. scenario monthly closing cash</caption>
          <thead>
            <tr>
              <th scope="col">Month</th>
              <th scope="col" className="amount">
                Scenario income
              </th>
              <th scope="col" className="amount">
                Baseline closing cash
              </th>
              <th scope="col" className="amount">
                Scenario closing cash
              </th>
              <th scope="col" className="amount">
                Delta vs. baseline
              </th>
            </tr>
          </thead>
          <tbody>
            {response.scenarioRows.map((row, index) => (
              <tr key={row.month}>
                <td className="record-title">Month {row.month}</td>
                <td className="amount" data-label="Scenario income">
                  {formatAmountMagnitude(row.income)}
                </td>
                <td className="amount" data-label="Baseline closing cash">
                  <SignedAmount value={response.baselineRows[index].closingCash} />
                </td>
                <td className="amount" data-label="Scenario closing cash">
                  <SignedAmount value={row.closingCash} />
                </td>
                <td className="amount" data-label="Delta vs. baseline">
                  <SignedAmount value={response.closingDeltas[index]} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
