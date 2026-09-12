import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchDebtPrepaymentComparison } from '../api/client';
import type {
  DebtAmortizationStatus,
  DebtPrepaymentComparisonRequest,
  DebtPrepaymentComparisonResponse,
  DebtPrepaymentPathResponse,
} from '../api/types';
import { formatAmountMagnitude } from '../calculatorMoney';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';

const DEFAULTS = {
  principal: '',
  monthlyInterestRate: '',
  monthlyPayment: '',
  currency: 'USD',
  immediatePrepayment: '',
};

const STATUS_LABELS: Record<DebtAmortizationStatus, string> = {
  PAID_OFF: 'Paid off',
  NON_AMORTIZING: 'Non-amortizing',
  HORIZON_LIMIT: 'Horizon limit reached',
};

export function DebtPrepaymentComparisonSection() {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useCalculatorSubmit(fetchDebtPrepaymentComparison);
  const headingId = useId();
  const errorId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: DebtPrepaymentComparisonRequest = {
      principal: form.principal.trim(),
      monthlyInterestRate: form.monthlyInterestRate.trim(),
      monthlyPayment: form.monthlyPayment.trim(),
      currency: form.currency.trim().toUpperCase(),
      immediatePrepayment: form.immediatePrepayment.trim(),
    };
    void run(request);
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Debt prepayment comparison</h2>
      <p className="note">
        Compares an immediate principal prepayment against continuing the same fixed monthly payment — a modeled
        what-if over the amounts you enter, not a recommendation to make a prepayment or a record of any decision.
      </p>

      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="dpc-principal">Principal</label>
          <input
            id="dpc-principal"
            name="principal"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.principal}
            onChange={updateField('principal')}
          />
        </div>
        <div className="field">
          <label htmlFor="dpc-rate">Monthly interest rate</label>
          <input
            id="dpc-rate"
            name="monthlyInterestRate"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.01"
            value={form.monthlyInterestRate}
            onChange={updateField('monthlyInterestRate')}
          />
        </div>
        <div className="field">
          <label htmlFor="dpc-payment">Monthly payment</label>
          <input
            id="dpc-payment"
            name="monthlyPayment"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.monthlyPayment}
            onChange={updateField('monthlyPayment')}
          />
        </div>
        <div className="field">
          <label htmlFor="dpc-currency">Currency</label>
          <input
            id="dpc-currency"
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
          <label htmlFor="dpc-prepayment">Immediate prepayment</label>
          <input
            id="dpc-prepayment"
            name="immediatePrepayment"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.immediatePrepayment}
            onChange={updateField('immediatePrepayment')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting} aria-describedby={errorId}>
          {submitting ? 'Calculating…' : 'Compare prepayment'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <DebtPrepaymentComparisonResults response={state.data} />}
    </section>
  );
}

function PathSummary({ title, path, currency }: { title: string; path: DebtPrepaymentPathResponse; currency: string }) {
  return (
    <div className="summary-item">
      {title}
      <strong>
        <span className="badge">{STATUS_LABELS[path.status]}</span>
      </strong>
      <div>{path.payoffMonths === null ? 'Does not pay off' : `Payoff in ${path.payoffMonths} months`}</div>
      <div>
        Total paid: {formatAmountMagnitude(path.totalPaid)} {currency}
      </div>
      <div>
        Total interest: {formatAmountMagnitude(path.totalInterest)} {currency}
      </div>
    </div>
  );
}

function DebtPrepaymentComparisonResults({ response }: { response: DebtPrepaymentComparisonResponse }) {
  return (
    <div className="results">
      <div className="summary-grid">
        <PathSummary title="Baseline (no prepayment)" path={response.baseline} currency={response.currency} />
        <PathSummary title="Scenario (with prepayment)" path={response.scenario} currency={response.currency} />
        <div className="summary-item">
          Scenario total cash paid
          <strong>
            {formatAmountMagnitude(response.scenarioTotalCashPaid)} {response.currency}
          </strong>
          <span>Includes the immediate prepayment itself.</span>
        </div>
        {response.comparisonUnavailableReason === null ? (
          <div className="summary-item">
            Lifetime savings
            <strong>
              {formatAmountMagnitude(response.lifetimeCashSaved!)} {response.currency}
            </strong>
            <span>
              {formatAmountMagnitude(response.lifetimeInterestSaved!)} {response.currency} in interest, {response.payoffMonthsSaved}{' '}
              months sooner
            </span>
          </div>
        ) : (
          <div className="summary-item">
            Lifetime savings
            <strong>
              <span className="badge">Not available</span>
            </strong>
            <span>{response.comparisonUnavailableReason}</span>
          </div>
        )}
      </div>
    </div>
  );
}
