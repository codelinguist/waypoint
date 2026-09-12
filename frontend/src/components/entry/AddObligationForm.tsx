import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createObligation } from '../../api/client';
import type { CreateObligationRequest, Frequency, ObligationType } from '../../api/types';
import { frequencyLabel, obligationTypeLabel } from '../../labels';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const OBLIGATION_TYPES: ObligationType[] = [
  'HOUSEHOLD_BASELINE',
  'MORTGAGE',
  'LOAN_PAYMENT',
  'INSURANCE',
  'TUITION',
  'TRAVEL_SINKING_FUND',
  'DISCRETIONARY',
  'OTHER',
];
const FREQUENCIES: Frequency[] = ['HOURLY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'ANNUAL'];

const DEFAULTS = {
  name: '',
  obligationType: 'HOUSEHOLD_BASELINE' as ObligationType,
  amount: '',
  frequency: 'MONTHLY' as Frequency,
  currency: '',
  startDate: '',
  endDate: '',
};

export function AddObligationForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreateObligationRequest) => createObligation(householdId, request));
  const formId = useId();

  function updateField<K extends keyof typeof DEFAULTS>(field: K) {
    return (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function toggle() {
    if (open) reset();
    setOpen((value) => !value);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: CreateObligationRequest = {
      name: form.name.trim(),
      obligationType: form.obligationType,
      amount: form.amount.trim(),
      frequency: form.frequency,
      currency: form.currency.trim().toUpperCase(),
      startDate: form.startDate,
      endDate: form.endDate || null,
    };
    const data = await run(request);
    if (data) {
      onCreated();
      setForm(DEFAULTS);
      setOpen(false);
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <Disclosure open={open} onToggle={toggle} closedLabel="Add obligation">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-type`}>Type</label>
          <select id={`${formId}-type`} value={form.obligationType} onChange={updateField('obligationType')}>
            {OBLIGATION_TYPES.map((type) => (
              <option key={type} value={type}>
                {obligationTypeLabel(type)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-currency`}>Currency</label>
          <input
            id={`${formId}-currency`}
            type="text"
            required
            maxLength={3}
            placeholder="USD"
            value={form.currency}
            onChange={updateField('currency')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-amount`}>Amount</label>
          <input
            id={`${formId}-amount`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.amount}
            onChange={updateField('amount')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-frequency`}>Frequency</label>
          <select id={`${formId}-frequency`} value={form.frequency} onChange={updateField('frequency')}>
            {FREQUENCIES.map((frequency) => (
              <option key={frequency} value={frequency}>
                {frequencyLabel(frequency)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-start`}>Start date</label>
          <input id={`${formId}-start`} type="date" required value={form.startDate} onChange={updateField('startDate')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-end`}>End date (optional)</label>
          <input id={`${formId}-end`} type="date" value={form.endDate} onChange={updateField('endDate')} />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add obligation'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" role="alert">
          {formatEntryErrorMessage(state)}
        </p>
      )}
    </Disclosure>
  );
}
