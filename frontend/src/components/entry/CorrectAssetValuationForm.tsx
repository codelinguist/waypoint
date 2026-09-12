import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { correctAssetValuation, fetchAssetValuationState } from '../../api/client';
import type { AssetValuationState, UpdateAssetValuationRequest } from '../../api/types';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

type LoadState = { status: 'idle' | 'loading' } | { status: 'error'; message: string } | { status: 'ready'; current: AssetValuationState };

export function CorrectAssetValuationForm({
  householdId,
  assetId,
  assetName,
  onCorrected,
}: {
  householdId: string;
  assetId: string;
  assetName: string;
  onCorrected: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [load, setLoad] = useState<LoadState>({ status: 'idle' });
  const [form, setForm] = useState({ estimatedValue: '', planningValue: '', valuedAt: '', reason: '' });
  const { state, run, reset } = useEntrySubmit((request: UpdateAssetValuationRequest) =>
    correctAssetValuation(householdId, assetId, request)
  );
  const formId = useId();

  async function loadCurrent() {
    setLoad({ status: 'loading' });
    try {
      const current = await fetchAssetValuationState(householdId, assetId);
      setForm({
        estimatedValue: current.estimatedValue,
        planningValue: current.planningValue,
        valuedAt: current.valuedAt,
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
    const request: UpdateAssetValuationRequest = {
      estimatedValue: form.estimatedValue.trim(),
      planningValue: form.planningValue.trim(),
      valuedAt: form.valuedAt,
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
          Loading current value for {assetName}&hellip;
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
            <label htmlFor={`${formId}-estimated`}>Estimated value</label>
            <input
              id={`${formId}-estimated`}
              type="text"
              inputMode="decimal"
              required
              value={form.estimatedValue}
              onChange={updateField('estimatedValue')}
            />
          </div>
          <div className="field">
            <label htmlFor={`${formId}-planning`}>Planning value</label>
            <input
              id={`${formId}-planning`}
              type="text"
              inputMode="decimal"
              required
              value={form.planningValue}
              onChange={updateField('planningValue')}
            />
          </div>
          <div className="field">
            <label htmlFor={`${formId}-valued-at`}>Valued as of</label>
            <input id={`${formId}-valued-at`} type="date" required value={form.valuedAt} onChange={updateField('valuedAt')} />
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
