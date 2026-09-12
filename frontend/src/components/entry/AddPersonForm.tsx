import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createPerson } from '../../api/client';
import type { CreatePersonRequest, Person } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';

const DEFAULTS = {
  name: '',
  role: '',
};

/**
 * Net-new for WAP-26, the second half of the wizard's first step. `role` is
 * free text (PD-003, agent/product/household-foundation/product-brief.md) —
 * no enum to pick from, same as the backend accepts. Always rendered open,
 * like AddHouseholdForm — repeatable via the wizard re-mounting it after
 * each successful add, not a Disclosure toggle.
 */
export function AddPersonForm({ householdId, onCreated }: { householdId: string; onCreated: (person: Person) => void }) {
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreatePersonRequest) => createPerson(householdId, request));
  const formId = useId();

  function updateField(field: keyof typeof DEFAULTS) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const request: CreatePersonRequest = {
      name: form.name.trim(),
      role: form.role.trim(),
    };
    const data = await run(request);
    if (data) {
      onCreated(data);
      setForm(DEFAULTS);
      reset();
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <>
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-role`}>Role</label>
          <input
            id={`${formId}-role`}
            type="text"
            required
            placeholder="Parent"
            value={form.role}
            onChange={updateField('role')}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add person'}
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
