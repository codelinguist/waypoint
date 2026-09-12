import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchEmergencyFundRunway } from '../api/client';
import type { EmergencyFundRunwayRequest, EmergencyFundRunwayResponse } from '../api/types';
import { formatAmountMagnitude } from '../calculatorMoney';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';

const DEFAULTS = {
  availableReserve: '',
  monthlyExpenses: '',
  monthlyNetIncome: '',
  currency: 'USD',
};

export function EmergencyFundRunwaySection() {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useCalculatorSubmit(fetchEmergencyFundRunway);
  const headingId = useId();
  const errorId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: EmergencyFundRunwayRequest = {
      availableReserve: form.availableReserve.trim(),
      monthlyExpenses: form.monthlyExpenses.trim(),
      monthlyNetIncome: form.monthlyNetIncome.trim(),
      currency: form.currency.trim().toUpperCase(),
    };
    void run(request);
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Emergency-fund runway</h2>
      <p className="note">
        How long a fixed reserve covers a constant monthly shortfall — an estimate from the amounts you enter, not a
        guaranteed outcome.
      </p>

      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="efr-reserve">Available reserve</label>
          <input
            id="efr-reserve"
            name="availableReserve"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.availableReserve}
            onChange={updateField('availableReserve')}
          />
        </div>
        <div className="field">
          <label htmlFor="efr-expenses">Monthly expenses</label>
          <input
            id="efr-expenses"
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
          <label htmlFor="efr-income">Monthly net income</label>
          <input
            id="efr-income"
            name="monthlyNetIncome"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.monthlyNetIncome}
            onChange={updateField('monthlyNetIncome')}
          />
        </div>
        <div className="field">
          <label htmlFor="efr-currency">Currency</label>
          <input
            id="efr-currency"
            name="currency"
            type="text"
            required
            maxLength={3}
            placeholder="USD"
            value={form.currency}
            onChange={updateField('currency')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting} aria-describedby={errorId}>
          {submitting ? 'Calculating…' : 'Calculate runway'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <EmergencyFundRunwayResults response={state.data} />}
    </section>
  );
}

function EmergencyFundRunwayResults({ response }: { response: EmergencyFundRunwayResponse }) {
  return (
    <div className="results">
      <div className="summary-grid">
        <div className="summary-item">
          Monthly shortfall
          <strong>
            {formatAmountMagnitude(response.monthlyShortfall)} {response.currency}
          </strong>
        </div>
        <div className="summary-item">
          Status
          <strong>
            <span className="badge">{response.status === 'FINITE' ? 'Finite runway' : 'No shortfall'}</span>
          </strong>
        </div>
        <div className="summary-item">
          Runway
          <strong>
            {response.runwayMonths === null ? 'Not applicable' : `${formatAmountMagnitude(response.runwayMonths)} months`}
          </strong>
        </div>
        <div className="summary-item">
          Full months covered
          <strong>{response.fullMonthsCovered === null ? 'Not applicable' : response.fullMonthsCovered}</strong>
        </div>
      </div>

      {/* modelNote is server-generated and must be shown verbatim, unmodified (WAP-19 acceptance criteria). */}
      <p className="model-note">{response.modelNote}</p>
    </div>
  );
}
