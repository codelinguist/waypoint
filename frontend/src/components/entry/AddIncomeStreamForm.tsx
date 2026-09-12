import { useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { createIncomeStream } from '../../api/client';
import type { CompensationClassification, CreateIncomeStreamRequest, Frequency, IncomeCertainty, IncomeType } from '../../api/types';
import { certaintyLabel, compensationClassificationLabel, frequencyLabel, incomeTypeLabel } from '../../labels';
import { useEntrySubmit, formatEntryErrorMessage } from '../../hooks/useEntrySubmit';
import { Disclosure } from '../Disclosure';

const INCOME_TYPES: IncomeType[] = ['SALARY', 'HOURLY_CONTRACT', 'BUSINESS_DISTRIBUTION', 'OTHER'];
const FREQUENCIES: Frequency[] = ['HOURLY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'ANNUAL'];
const CERTAINTIES: IncomeCertainty[] = ['CONFIRMED', 'EXPECTED', 'VARIABLE'];
const COMPENSATION_CLASSIFICATIONS: CompensationClassification[] = ['GROSS', 'NET', 'UNKNOWN'];

const DEFAULTS = {
  name: '',
  incomeType: 'SALARY' as IncomeType,
  amount: '',
  frequency: 'MONTHLY' as Frequency,
  currency: '',
  compensationClassification: 'GROSS' as CompensationClassification,
  certainty: 'CONFIRMED' as IncomeCertainty,
  startDate: '',
  endDate: '',
};

export function AddIncomeStreamForm({ householdId, onCreated }: { householdId: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(DEFAULTS);
  const { state, run, reset } = useEntrySubmit((request: CreateIncomeStreamRequest) =>
    createIncomeStream(householdId, request)
  );
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
    const request: CreateIncomeStreamRequest = {
      name: form.name.trim(),
      incomeType: form.incomeType,
      amount: form.amount.trim(),
      frequency: form.frequency,
      currency: form.currency.trim().toUpperCase(),
      compensationClassification: form.compensationClassification,
      certainty: form.certainty,
      startDate: form.startDate,
      endDate: form.endDate || null,
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
    <Disclosure open={open} onToggle={toggle} closedLabel="Add income stream">
      <form className="calc-form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor={`${formId}-name`}>Name</label>
          <input id={`${formId}-name`} type="text" required value={form.name} onChange={updateField('name')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-type`}>Type</label>
          <select id={`${formId}-type`} value={form.incomeType} onChange={updateField('incomeType')}>
            {INCOME_TYPES.map((type) => (
              <option key={type} value={type}>
                {incomeTypeLabel(type)}
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
          <label htmlFor={`${formId}-amount`}>Amount</label>
          <input
            id={`${formId}-amount`}
            type="text"
            inputMode="decimal"
            required
            placeholder="0.00"
            value={form.amount}
            onChange={updateField('amount')}
          />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-frequency`}>Frequency</label>
          <select id={`${formId}-frequency`} value={form.frequency} onChange={updateField('frequency')}>
            {FREQUENCIES.map((frequency) => (
              <option key={frequency} value={frequency}>
                {frequencyLabel(frequency)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-classification`}>Compensation classification</label>
          <select
            id={`${formId}-classification`}
            value={form.compensationClassification}
            onChange={updateField('compensationClassification')}
          >
            {COMPENSATION_CLASSIFICATIONS.map((classification) => (
              <option key={classification} value={classification}>
                {compensationClassificationLabel(classification)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-certainty`}>Certainty</label>
          <select id={`${formId}-certainty`} value={form.certainty} onChange={updateField('certainty')}>
            {CERTAINTIES.map((certainty) => (
              <option key={certainty} value={certainty}>
                {certaintyLabel(certainty)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${formId}-start`}>Start date</label>
          <input id={`${formId}-start`} type="date" required value={form.startDate} onChange={updateField('startDate')} />
        </div>
        <div className="field">
          <label htmlFor={`${formId}-end`}>End date (optional)</label>
          <input id={`${formId}-end`} type="date" value={form.endDate} onChange={updateField('endDate')} />
        </div>
        <button className="submit-btn" type="submit" disabled={submitting}>
          {submitting ? 'Adding…' : 'Add income stream'}
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
