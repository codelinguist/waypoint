import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createLiability } from '../../api/client';
import type { CreateLiabilityRequest, LiabilityType } from '../../api/types';
import { liabilityTypeLabel } from '../../labels';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const LIABILITY_TYPES: LiabilityType[] = ['CREDIT_CARD', 'MORTGAGE', 'PERSONAL_LOAN', 'BUSINESS_LOAN', 'OTHER'];

const DEFAULTS = {
  name: '',
  liabilityType: 'CREDIT_CARD' as LiabilityType,
  outstandingBalance: '',
  currency: '',
  balanceAsOf: '',
};

export function AddLiabilityForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreateLiabilityRequest) => createLiability(householdId, request));
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
    const request: CreateLiabilityRequest = {
      name: form.name.trim(),
      liabilityType: form.liabilityType,
      outstandingBalance: form.outstandingBalance.trim(),
      currency: form.currency.trim().toUpperCase(),
      balanceAsOf: form.balanceAsOf,
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
    <Disclosure open={open} onToggle={toggle} closedLabel="Add liability">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-type`}>Type</label>
          <select id={`${formId}-type`} value={form.liabilityType} onChange={updateField('liabilityType')}>
            {LIABILITY_TYPES.map((type) => (
              <option key={type} value={type}>
                {liabilityTypeLabel(type)}
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
          <label htmlFor={`${formId}-balance`}>Outstanding balance</label>
          <input
            id={`${formId}-balance`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.outstandingBalance}
            onChange={updateField('outstandingBalance')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-balance-as-of`}>Balance as of</label>
          <input
            id={`${formId}-balance-as-of`}
            type="date"
            required
            value={form.balanceAsOf}
            onChange={updateField('balanceAsOf')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add liability'}
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
