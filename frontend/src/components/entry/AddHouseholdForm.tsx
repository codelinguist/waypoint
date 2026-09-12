import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createHousehold } from '../../api/client';
import type { CreateHouseholdRequest, Household } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';

const DEFAULTS = {
  name: '',
  baseCurrency: '',
};

/**
 * Net-new for WAP-26 (no WAP-25 equivalent exists to reuse): the wizard's
 * first step. Always rendered open — unlike every WAP-25 entry form there is
 * nothing else on this step to collapse it for. Same field/button/error
 * conventions (`useEntrySubmit`, `.calc-form`/`.field`/`.submit-btn`) as
 * every reused form below it in the wizard.
 */
export function AddHouseholdForm({ onCreated }: { onCreated: (household: Household) => void }) {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run } = useEntrySubmit((request: CreateHouseholdRequest) => createHousehold(request));
  const formId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: CreateHouseholdRequest = {
      name: form.name.trim(),
      baseCurrency: form.baseCurrency.trim().toUpperCase(),
    };
    const data = await run(request);
    if (data) {
      onCreated(data);
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <>
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Household name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-currency`}>Base currency</label>
          <input
            id={`${formId}-currency`}
            type="text"
            required
            maxLength={3}
            placeholder="USD"
            value={form.baseCurrency}
            onChange={updateField('baseCurrency')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create household'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" role="alert">
          {formatEntryErrorMessage(state)}
        </p>
      )}
    </>
  );
}
