import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { supersedePlanningAssumption } from '../../api/client';
import type { CreatePlanningAssumptionRequest, PlanningAssumption } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

/** Pre-fills every field from the assumption being superseded, per the backend's replace-in-full contract (no revision — see CreatePlanningAssumptionRequest.java's supersede endpoint). */
export function SupersedePlanningAssumptionForm({
  householdId,
  assumption,
  onSuperseded,
}: {
  householdId: string;
  assumption: PlanningAssumption;
  onSuperseded: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({
    name: assumption.name,
    value: assumption.value,
    valueType: assumption.valueType,
    notes: assumption.notes ?? '',
    effectiveFrom: assumption.effectiveFrom,
    effectiveUntil: assumption.effectiveUntil ?? '',
    reviewDate: assumption.reviewDate,
  });
  const { state, run, reset } = useEntrySubmit((request: CreatePlanningAssumptionRequest) =>
    supersedePlanningAssumption(householdId, assumption.id, request)
  );
  const formId = useId();

  function updateField(field: keyof typeof form) {
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
      onSuperseded();
      setOpen(false);
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <Disclosure open={open} onToggle={toggle} closedLabel="Supersede" openLabel="Cancel">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-value-type`}>Value type</label>
          <input id={`${formId}-value-type`} type="text" required value={form.valueType} onChange={updateField('valueType')} />
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
          {submitting ? 'Saving…' : 'Save as new version'}
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
