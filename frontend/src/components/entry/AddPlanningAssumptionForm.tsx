import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createPlanningAssumption } from '../../api/client';
import type { CreatePlanningAssumptionRequest } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const DEFAULTS = {
  name: '',
  value: '',
  valueType: '',
  notes: '',
  effectiveFrom: '',
  effectiveUntil: '',
  reviewDate: '',
};

export function AddPlanningAssumptionForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreatePlanningAssumptionRequest) =>
    createPlanningAssumption(householdId, request)
  );
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
    const request: CreatePlanningAssumptionRequest = {
      name: form.name.trim(),
      value: form.value.trim(),
      valueType: form.valueType.trim(),
      notes: form.notes.trim() || null,
      effectiveFrom: form.effectiveFrom,
      effectiveUntil: form.effectiveUntil || null,
      reviewDate: form.reviewDate,
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
    <Disclosure open={open} onToggle={toggle} closedLabel="Add assumption">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-value-type`}>Value type</label>
          <input
            id={`${formId}-value-type`}
            type="text"
            required
            placeholder="e.g. percentage, currency, boolean"
            value={form.valueType}
            onChange={updateField('valueType')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-value`}>Value</label>
          <input id={`${formId}-value`} type="text" required value={form.value} onChange={updateField('value')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-effective-from`}>Effective from</label>
          <input
            id={`${formId}-effective-from`}
            type="date"
            required
            value={form.effectiveFrom}
            onChange={updateField('effectiveFrom')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-effective-until`}>Effective until (optional)</label>
          <input
            id={`${formId}-effective-until`}
            type="date"
            value={form.effectiveUntil}
            onChange={updateField('effectiveUntil')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-review-date`}>Review date</label>
          <input
            id={`${formId}-review-date`}
            type="date"
            required
            value={form.reviewDate}
            onChange={updateField('reviewDate')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-notes`}>Notes (optional)</label>
          <input id={`${formId}-notes`} type="text" maxLength={2000} value={form.notes} onChange={updateField('notes')} />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add assumption'}
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
