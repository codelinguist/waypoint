import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createAsset } from '../../api/client';
import type { AssetType, CreateAssetRequest, Liquidity } from '../../api/types';
import { assetTypeLabel, liquidityLabel } from '../../labels';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const ASSET_TYPES: AssetType[] = ['CASH', 'BANK_ACCOUNT', 'PROPERTY', 'INVESTMENT', 'BUSINESS_OWNERSHIP', 'OTHER'];
const LIQUIDITIES: Liquidity[] = ['LIQUID', 'RESTRICTED', 'ILLIQUID'];

const DEFAULTS = {
  name: '',
  assetType: 'CASH' as AssetType,
  estimatedValue: '',
  planningValue: '',
  currency: '',
  valuedAt: '',
  liquidity: 'LIQUID' as Liquidity,
};

export function AddAssetForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreateAssetRequest) => createAsset(householdId, request));
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
    const request: CreateAssetRequest = {
      name: form.name.trim(),
      assetType: form.assetType,
      estimatedValue: form.estimatedValue.trim(),
      planningValue: form.planningValue.trim(),
      currency: form.currency.trim().toUpperCase(),
      valuedAt: form.valuedAt,
      liquidity: form.liquidity,
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
    <Disclosure open={open} onToggle={toggle} closedLabel="Add asset">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-type`}>Type</label>
          <select id={`${formId}-type`} value={form.assetType} onChange={updateField('assetType')}>
            {ASSET_TYPES.map((type) => (
              <option key={type} value={type}>
                {assetTypeLabel(type)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-liquidity`}>Liquidity</label>
          <select id={`${formId}-liquidity`} value={form.liquidity} onChange={updateField('liquidity')}>
            {LIQUIDITIES.map((liquidity) => (
              <option key={liquidity} value={liquidity}>
                {liquidityLabel(liquidity)}
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
          <label htmlFor={`${formId}-estimated`}>Estimated value</label>
          <input
            id={`${formId}-estimated`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
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
            placeholder="0.00"
            value={form.planningValue}
            onChange={updateField('planningValue')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-valued-at`}>Valued as of</label>
          <input id={`${formId}-valued-at`} type="date" required value={form.valuedAt} onChange={updateField('valuedAt')} />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add asset'}
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
