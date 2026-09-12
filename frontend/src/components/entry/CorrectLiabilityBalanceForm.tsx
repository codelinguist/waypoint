import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { correctLiabilityBalance, fetchLiability } from '../../api/client';
import type { Liability, RecordLiabilityBalanceRequest } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

type LoadState = { status: 'idle' | 'loading' } | { status: 'error'; message: string } | { status: 'ready'; current: Liability };

export function CorrectLiabilityBalanceForm({
  householdId,
  liabilityId,
  liabilityName,
  onCorrected,
}: {
  householdId: string;
  liabilityId: string;
  liabilityName: string;
  onCorrected: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [load, setLoad] = useState<LoadState>({ status: 'idle' });
  const [form, setForm] = useState({ outstandingBalance: '', balanceAsOf: '', reason: '' });
  const { state, run, reset } = useEntrySubmit((request: RecordLiabilityBalanceRequest) =>
    correctLiabilityBalance(householdId, liabilityId, request)
  );
  const formId = useId();

  async function loadCurrent() {
    setLoad({ status: 'loading' });
    try {
      const current = await fetchLiability(householdId, liabilityId);
      setForm({
        outstandingBalance: String(current.outstandingBalance),
        balanceAsOf: current.balanceAsOf,
        reason: '',
      });
      setLoad({ status: 'ready', current });
    } catch (error) {
      setLoad({ status: 'error', message: error instanceof Error ? error.message : 'Could not reach the server.' });
    }
  }

  function toggle() {
    if (open) {
      reset();
      setLoad({ status: 'idle' });
    } else {
      void loadCurrent();
    }
    setOpen((value) => !value);
  }

  function updateField<K extends keyof typeof form>(field: K) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (load.status !== 'ready') return;
    const request: RecordLiabilityBalanceRequest = {
      outstandingBalance: form.outstandingBalance.trim(),
      balanceAsOf: form.balanceAsOf,
      reason: form.reason.trim(),
      expectedRevision: load.current.revision,
    };
    const data = await run(request);
    if (data) {
      onCorrected();
      setOpen(false);
    }
  }

  const submitting = state.status === 'submitting';

  return (
    <Disclosure open={open} onToggle={toggle} closedLabel="Correct" openLabel="Cancel correction">
      {load.status === 'loading' && (
        <p className="status-line" role="status" aria-live="polite">
          Loading current balance for {liabilityName}&hellip;
        </p>
      )}

      {load.status === 'error' && (
        <p className="form-error" role="alert">
          {load.message}{' '}
          <button type="button" className="toggle-btn" onClick={() => void loadCurrent()}>
            Retry
          </button>
        </p>
      )}

      {load.status === 'ready' && (
        <form className="calc-form" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor={`${formId}-balance`}>Outstanding balance</label>
            <input
              id={`${formId}-balance`}
              type="text"
              inputMode="decimal"
              required
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
          <div className="field">
            <label htmlFor={`${formId}-reason`}>Reason for correction</label>
            <input
              id={`${formId}-reason`}
              type="text"
              required
              maxLength={500}
              value={form.reason}
              onChange={updateField('reason')}
            />
          </div>
          <button className="submit-btn" type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save correction'}
          </button>
        </form>
      )}

      {state.status === 'error' && state.kind === 'conflict' && (
        <p className="form-error" role="alert">
          This value changed since it was loaded.{' '}
          <button type="button" className="toggle-btn" onClick={() => void loadCurrent()}>
            Reload current value
          </button>
        </p>
      )}
      {state.status === 'error' && state.kind !== 'conflict' && (
        <p className="form-error" role="alert">
          {formatEntryErrorMessage(state)}
        </p>
      )}
    </Disclosure>
  );
}
