import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchPurchaseReserveImpact } from '../api/client';
import type {
  EmergencyFundRunwayResponse,
  PurchaseReserveImpactRequest,
  PurchaseReserveImpactResponse,
} from '../api/types';
import { formatAmountMagnitude } from '../calculatorMoney';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';

const DEFAULTS = {
  currency: 'USD',
  availableReserve: '',
  purchaseAmount: '',
  monthlyExpenses: '',
  monthlyNetIncome: '',
  minimumReserve: '',
};

export function PurchaseReserveImpactSection() {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useCalculatorSubmit(fetchPurchaseReserveImpact);
  const headingId = useId();
  const errorId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: PurchaseReserveImpactRequest = {
      currency: form.currency.trim().toUpperCase(),
      availableReserve: form.availableReserve.trim(),
      purchaseAmount: form.purchaseAmount.trim(),
      monthlyExpenses: form.monthlyExpenses.trim(),
      monthlyNetIncome: form.monthlyNetIncome.trim(),
      minimumReserve: form.minimumReserve.trim(),
    };
    void run(request);
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Purchase impact on reserves</h2>
      <p className="note">
        A hypothetical look at what a purchase of this size would do to your reserve and runway — not a
        recommendation to make the purchase, and not a record of any decision.
      </p>

      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="pri-currency">Currency</label>
          <input
            id="pri-currency"
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
          <label htmlFor="pri-available-reserve">Available reserve</label>
          <input
            id="pri-available-reserve"
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
          <label htmlFor="pri-purchase-amount">Purchase amount</label>
          <input
            id="pri-purchase-amount"
            name="purchaseAmount"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.purchaseAmount}
            onChange={updateField('purchaseAmount')}
          />
        </div>
        <div className="field">
          <label htmlFor="pri-monthly-expenses">Monthly expenses</label>
          <input
            id="pri-monthly-expenses"
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
          <label htmlFor="pri-monthly-net-income">Monthly net income</label>
          <input
            id="pri-monthly-net-income"
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
          <label htmlFor="pri-minimum-reserve">Minimum reserve floor</label>
          <input
            id="pri-minimum-reserve"
            name="minimumReserve"
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.minimumReserve}
            onChange={updateField('minimumReserve')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting} aria-describedby={errorId}>
          {submitting ? 'Calculating…' : 'Calculate reserve impact'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <PurchaseReserveImpactResults response={state.data} />}
    </section>
  );
}

function RunwaySummary({ title, runway }: { title: string; runway: EmergencyFundRunwayResponse }) {
  return (
    <div className="summary-item">
      {title}
      <strong>
        <span className="badge">{runway.status === 'FINITE' ? 'Finite runway' : 'No shortfall'}</span>
      </strong>
      <span>
        {runway.runwayMonths === null ? 'Not applicable' : `${formatAmountMagnitude(runway.runwayMonths)} months`}
      </span>
    </div>
  );
}

function PurchaseReserveImpactResults({ response }: { response: PurchaseReserveImpactResponse }) {
  return (
    <div className="results">
      <div className="summary-grid">
        <div className="summary-item">
          Reserve after purchase
          <strong>
            {formatAmountMagnitude(response.reserveAfterPurchase)} {response.currency}
          </strong>
        </div>
        <div className="summary-item">
          Purchase fits available cash
          <strong>
            <span className="badge">{response.purchaseFitsAvailableCash ? 'Yes' : 'No'}</span>
          </strong>
          {!response.purchaseFitsAvailableCash && (
            <span>
              Funding gap: {formatAmountMagnitude(response.purchaseFundingGap)} {response.currency}
            </span>
          )}
        </div>
        <div className="summary-item">
          Meets reserve floor after purchase
          <strong>
            <span className="badge">{response.reserveMeetsFloorAfterPurchase ? 'Yes' : 'No'}</span>
          </strong>
          {!response.reserveMeetsFloorAfterPurchase && (
            <span>
              Floor gap: {formatAmountMagnitude(response.reserveFloorGapAfterPurchase)} {response.currency}
            </span>
          )}
        </div>
        <RunwaySummary title="Runway before purchase" runway={response.beforePurchaseRunway} />
        {response.afterPurchaseRunwayAvailability === 'AVAILABLE' && response.afterPurchaseRunway ? (
          <RunwaySummary title="Runway after purchase" runway={response.afterPurchaseRunway} />
        ) : (
          <div className="summary-item">
            Runway after purchase
            <strong>
              <span className="badge">Not available</span>
            </strong>
            <span>Cash would go negative, so a runway cannot be computed.</span>
          </div>
        )}
      </div>

      <p className="model-note">{response.modelNote}</p>
    </div>
  );
}
