import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createGoal } from '../../api/client';
import type { CreateGoalRequest } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const DEFAULTS = {
  name: '',
  targetAmount: '',
  currency: '',
  targetDate: '',
  priority: '',
  currentAmount: '',
};

export function AddGoalForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreateGoalRequest) => createGoal(householdId, request));
  const formId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  function toggle() {
    if (open) reset();
    setOpen((value) => !value);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: CreateGoalRequest = {
      name: form.name.trim(),
      targetAmount: form.targetAmount.trim(),
      currency: form.currency.trim().toUpperCase(),
      targetDate: form.targetDate,
      priority: Number(form.priority),
      currentAmount: form.currentAmount.trim(),
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
    <Disclosure open={open} onToggle={toggle} closedLabel="Add goal">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
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
          <label htmlFor={`${formId}-target-amount`}>Target amount</label>
          <input
            id={`${formId}-target-amount`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.targetAmount}
            onChange={updateField('targetAmount')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-current-amount`}>Current amount</label>
          <input
            id={`${formId}-current-amount`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.currentAmount}
            onChange={updateField('currentAmount')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-target-date`}>Target date</label>
          <input
            id={`${formId}-target-date`}
            type="date"
            required
            value={form.targetDate}
            onChange={updateField('targetDate')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-priority`}>Priority</label>
          <input
            id={`${formId}-priority`}
            type="number"
            min={1}
            step={1}
            required
            value={form.priority}
            onChange={updateField('priority')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add goal'}
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
