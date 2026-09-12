import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createSnapshot } from '../../api/client';
import type { CreateSnapshotRequest } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

export function CreateSnapshotForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [asOfDate, setAsOfDate] = useState('');
  const { state, run, reset } = useEntrySubmit((request: CreateSnapshotRequest) => createSnapshot(householdId, request));
  const formId = useId();

  function toggle() {
    if (open) reset();
    setOpen((value) => !value);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const data = await run({ asOfDate });
    if (data) {
      onCreated();
      setAsOfDate('');
      setOpen(false);
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <Disclosure open={open} onToggle={toggle} closedLabel="Create snapshot">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-as-of`}>As of date</label>
          <input
            id={`${formId}-as-of`}
            type="date"
            required
            value={asOfDate}
            onChange={(event: ChangeEvent<HTMLInputElement>) => setAsOfDate(event.target.value)}
          />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create snapshot'}
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
