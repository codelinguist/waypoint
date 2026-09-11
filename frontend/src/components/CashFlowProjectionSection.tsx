import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchCashFlowProjection } from '../api/client';
import type { CashFlowProjectionRequest, CashFlowProjectionResponse } from '../api/types';
import { formatAmountMagnitude, formatSignedAmount, formatYearMonth } from '../calculatorMoney';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';

const DEFAULTS = {
  currency: 'USD',
  startMonth: '',
  startingCash: '',
  monthlyInflow: '',
  monthlyOutflow: '',
  months: '12',
};

export function CashFlowProjectionSection() {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useCalculatorSubmit(fetchCashFlowProjection);
  const headingId = useId();
  const errorId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: CashFlowProjectionRequest = {
      currency: form.currency.trim().toUpperCase(),
      startMonth: form.startMonth,
      startingCash: form.startingCash.trim(),
      monthlyInflow: form.monthlyInflow.trim(),
      monthlyOutflow: form.monthlyOutflow.trim(),
      months: Number(form.months),
    };
    void run(request);
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Cash-flow projection</h2>
      <p className="note">
        A projection over the constant monthly amounts you enter below — not a forecast of what will actually
        happen, and not a guaranteed outcome.
      </p>

      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="cfp-currency">Currency</label>
          <input
            id="cfp-currency"
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
          <label htmlFor="cfp-start-month">Start month</label>
          <input
            id="cfp-start-month"
            name="startMonth"
            type="text"
            inputMode="numeric"
            required
            placeholder="YYYY-MM"
            pattern="\d{4}-(0[1-9]|1[0-2])"
            value={form.startMonth}
            onChange={updateField('startMonth')}
          />
        </div>
        <div className="field">
          <label htmlFor="cfp-starting-cash">Starting cash</label>
          <input
            id="cfp-starting-cash"
            name="startingCash"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.startingCash}
            onChange={updateField('startingCash')}
          />
        </div>
        <div className="field">
          <label htmlFor="cfp-monthly-inflow">Monthly inflow</label>
          <input
            id="cfp-monthly-inflow"
            name="monthlyInflow"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.monthlyInflow}
            onChange={updateField('monthlyInflow')}
          />
        </div>
        <div className="field">
          <label htmlFor="cfp-monthly-outflow">Monthly outflow</label>
          <input
            id="cfp-monthly-outflow"
            name="monthlyOutflow"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.monthlyOutflow}
            onChange={updateField('monthlyOutflow')}
          />
        </div>
        <div className="field">
          <label htmlFor="cfp-months">Months to project</label>
          <input
            id="cfp-months"
            name="months"
            type="number"
            min={1}
            max={1200}
            step={1}
            required
            value={form.months}
            onChange={updateField('months')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting} aria-describedby={errorId}>
          {submitting ? 'Projecting…' : 'Run projection'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <CashFlowProjectionResults response={state.data} />}
    </section>
  );
}

/** A signed amount rendered with the project's non-color-only sign convention (see index.css .negative/.positive). */
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

function CashFlowProjectionResults({ response }: { response: CashFlowProjectionResponse }) {
  return (
    <div className="results">
      <div className="summary-grid">
        <div className="summary-item">
          Ending cash
          <strong>
            <SignedAmount value={response.endingCash} suffix={response.currency} />
          </strong>
        </div>
        <div className="summary-item">
          Lowest closing balance
          <strong>
            <SignedAmount value={response.lowestClosingBalance} suffix={response.currency} />
          </strong>
          <span>in {formatYearMonth(response.lowestClosingBalanceMonth)}</span>
        </div>
        <div className="summary-item">
          First month below zero
          <strong>{response.firstNegativeMonth ? formatYearMonth(response.firstNegativeMonth) : 'None'}</strong>
        </div>
        <div className="summary-item">
          Status
          <strong>
            <span className="badge">
              {response.status === 'BECOMES_NEGATIVE' ? 'Becomes negative' : 'Remains non-negative'}
            </span>
          </strong>
        </div>
      </div>

      <div className="table-scroll" tabIndex={0} role="region" aria-label="Monthly projection, scrollable">
        <table className="stackable">
          <caption className="sr-only">Monthly cash-flow projection</caption>
          <thead>
            <tr>
              <th scope="col">Month</th>
              <th scope="col" className="amount">
                Opening cash
              </th>
              <th scope="col" className="amount">
                Inflow
              </th>
              <th scope="col" className="amount">
                Outflow
              </th>
              <th scope="col" className="amount">
                Net cash flow
              </th>
              <th scope="col" className="amount">
                Closing cash
              </th>
            </tr>
          </thead>
          <tbody>
            {response.rows.map((row) => (
              <tr key={row.month}>
                <td className="record-title" data-label="Month">
                  {formatYearMonth(row.month)}
                </td>
                <td className="amount" data-label="Opening cash">
                  {formatAmountMagnitude(row.openingCash)}
                </td>
                <td className="amount" data-label="Inflow">
                  {formatAmountMagnitude(row.inflow)}
                </td>
                <td className="amount" data-label="Outflow">
                  {formatAmountMagnitude(row.outflow)}
                </td>
                <td className="amount" data-label="Net cash flow">
                  <SignedAmount value={row.netCashFlow} />
                </td>
                <td className="amount" data-label="Closing cash">
                  <SignedAmount value={row.closingCash} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
