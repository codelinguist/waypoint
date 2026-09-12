import { useEffect, useId, useRef, useState, type ComponentType } from 'react';
import { setHouseholdIdOverride } from '../config';
import { AddAssetForm } from './entry/AddAssetForm';
import { AddGoalForm } from './entry/AddGoalForm';
import { AddHouseholdForm } from './entry/AddHouseholdForm';
import { AddIncomeStreamForm } from './entry/AddIncomeStreamForm';
import { AddLiabilityForm } from './entry/AddLiabilityForm';
import { AddObligationForm } from './entry/AddObligationForm';
import { AddPersonForm } from './entry/AddPersonForm';
import { CreateSnapshotForm } from './entry/CreateSnapshotForm';

type EntityStepKey = 'assets' | 'liabilities' | 'income' | 'obligations' | 'goals';
type StepKey = 'household' | EntityStepKey | 'snapshot';

const ALL_STEPS: StepKey[] = ['household', 'assets', 'liabilities', 'income', 'obligations', 'goals', 'snapshot'];

const STEP_LABELS: Record<StepKey, string> = {
  household: 'Household & people',
  assets: 'Assets',
  liabilities: 'Liabilities',
  income: 'Income streams',
  obligations: 'Obligations',
  goals: 'Goals',
  snapshot: 'First snapshot',
};

/** Every WAP-25 entry form used below shares this exact prop shape, unmodified. */
type EntryFormType = ComponentType<{ householdId: string; onCreated: () => void }>;

const ENTITY_STEPS: Record<EntityStepKey, { Form: EntryFormType; noun: string; pluralNoun: string }> = {
  assets: { Form: AddAssetForm, noun: 'asset', pluralNoun: 'assets' },
  liabilities: { Form: AddLiabilityForm, noun: 'liability', pluralNoun: 'liabilities' },
  income: { Form: AddIncomeStreamForm, noun: 'income stream', pluralNoun: 'income streams' },
  obligations: { Form: AddObligationForm, noun: 'obligation', pluralNoun: 'obligations' },
  goals: { Form: AddGoalForm, noun: 'goal', pluralNoun: 'goals' },
};

function EntityStepBody({
  stepKey,
  householdId,
  count,
  onAdded,
}: {
  stepKey: EntityStepKey;
  householdId: string;
  count: number;
  onAdded: () => void;
}) {
  const { Form, noun, pluralNoun } = ENTITY_STEPS[stepKey];
  return (
    <>
      <Form householdId={householdId} onCreated={onAdded} />
      <p className="no-records-side">
        {count === 0 ? `No ${pluralNoun} added yet — add one above.` : `${count} ${count === 1 ? noun : pluralNoun} added.`}
      </p>
    </>
  );
}

/**
 * WAP-26: guides a brand-new household from nothing to a populated financial
 * position in one sequence — household + person(s), then assets,
 * liabilities, income streams, obligations, goals, and a closing snapshot.
 * Every entity step's body is the real WAP-25 create form for that entity,
 * unmodified; this component owns only sequencing, per-step progress
 * counts, and the household/person step (net new — no WAP-25 equivalent
 * exists). See agent/ui/onboarding-wizard/design-brief.md (Direction A).
 *
 * `initialHouseholdId` supports the second entry point: an already-configured
 * household with empty data can resume here (via a banner on Financial
 * Position) skipping the household/person step entirely.
 */
export function OnboardingWizard({
  initialHouseholdId,
  onComplete,
  onCancel,
}: {
  initialHouseholdId?: string;
  onComplete: () => void;
  onCancel?: () => void;
}) {
  const steps = initialHouseholdId ? ALL_STEPS.slice(1) : ALL_STEPS;
  const [stepPos, setStepPos] = useState(0);
  const [householdId, setHouseholdId] = useState<string | null>(initialHouseholdId ?? null);
  const [householdName, setHouseholdName] = useState<string | null>(null);
  const [personCount, setPersonCount] = useState(0);
  const [counts, setCounts] = useState<Record<EntityStepKey, number>>({
    assets: 0,
    liabilities: 0,
    income: 0,
    obligations: 0,
    goals: 0,
  });

  const currentStep = steps[stepPos];
  const stepNumber = stepPos + 1;
  const totalSteps = steps.length;
  const headingId = useId();
  const headingRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    headingRef.current?.focus();
  }, [stepPos]);

  function incrementCount(key: EntityStepKey) {
    setCounts((prev) => ({ ...prev, [key]: prev[key] + 1 }));
  }

  function canAdvance(): boolean {
    if (currentStep === 'household') return householdId !== null && personCount >= 1;
    if (currentStep === 'snapshot') return false; // no Next — the form's own success finishes the wizard
    return counts[currentStep] >= 1;
  }

  return (
    <div className="page">
      <header className="app-header">
        <h1 id={headingId} ref={headingRef} tabIndex={-1}>
          Set up your household
        </h1>
      </header>
      <p className="status-line" role="status" aria-live="polite">
        Step {stepNumber} of {totalSteps}: {STEP_LABELS[currentStep]}
      </p>

      <div className="card">
        {currentStep === 'household' &&
          (householdId === null ? (
            <AddHouseholdForm
              onCreated={(household) => {
                setHouseholdId(household.id);
                setHouseholdName(household.name);
                setHouseholdIdOverride(household.id);
              }}
            />
          ) : (
            <>
              <p className="status-line">
                Household &ldquo;{householdName}&rdquo; created. Add at least one person to continue.
              </p>
              <AddPersonForm householdId={householdId} onCreated={() => setPersonCount((c) => c + 1)} />
              <p className="no-records-side">
                {personCount === 0
                  ? 'No one added yet — add the first household member above.'
                  : `${personCount} ${personCount === 1 ? 'person' : 'people'} added.`}
              </p>
            </>
          ))}

        {currentStep !== 'household' && currentStep !== 'snapshot' && householdId && (
          <EntityStepBody
            stepKey={currentStep}
            householdId={householdId}
            count={counts[currentStep]}
            onAdded={() => incrementCount(currentStep)}
          />
        )}

        {currentStep === 'snapshot' && householdId && <CreateSnapshotForm householdId={householdId} onCreated={onComplete} />}
      </div>

      <div className="wizard-actions">
        {onCancel && (
          <button type="button" className="toggle-btn" onClick={onCancel}>
            Exit setup
          </button>
        )}
        {stepPos > 0 && (
          <button type="button" className="refresh-btn" onClick={() => setStepPos((p) => p - 1)}>
            Back
          </button>
        )}
        {currentStep !== 'snapshot' && (
          <button type="button" className="submit-btn" onClick={() => setStepPos((p) => p + 1)} disabled={!canAdvance()}>
            Next
          </button>
        )}
      </div>
    </div>
  );
}
