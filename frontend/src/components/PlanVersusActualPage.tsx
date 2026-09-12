import { useCallback, useId, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { fetchPlanVersusActual } from '../api/client';
import { ConfigNotFound } from './ConfigProblem';
import { useFinancialSnapshots } from '../hooks/useFinancialSnapshots';
import { useCalculatorSubmit } from '../hooks/useCalculatorSubmit';
import { formatAmountMagnitude, formatSignedAmount } from '../calculatorMoney';
import { formatSignedMoney } from '../money';
import { formatInstantUtc, formatLocalDate } from '../dates';
import { varianceDirectionLabel } from '../labels';
import { centsToMoneyString, parseNonNegativeCents } from '../planComparisonMath';
import type {
  FinancialSnapshotListItem,
  PlanVersusActualRequest,
  PlanVersusActualResponse,
  PlannedCurrencyTotalsInput,
  Variance,
} from '../api/types';

export function PlanVersusActualPage({ householdId }: { householdId: string }) {
  const { state, retry } = useFinancialSnapshots(householdId);

  return (
    <div className="page">
      <header className="app-header">
        <h1>Plan vs. actual</h1>
      </header>
      <p className="status-line">
        Compare planned totals you enter now against one recorded snapshot&apos;s actual figures. Nothing you enter
        below is saved — enter the plan fresh each time you want a comparison.
      </p>

      {state.status === 'loading' && (
        <p className="status-line" role="status" aria-live="polite">
          Loading snapshots&hellip;
        </p>
      )}

      {state.status === 'not-found' && <ConfigNotFound householdId={state.householdId} />}

      {state.status === 'error' && (
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load financial snapshots.</strong>
          </p>
          <p>{state.message}</p>
          <p>
            <button className="refresh-btn" type="button" onClick={retry}>
              Retry
            </button>
          </p>
        </div>
      )}

      {state.status === 'ready' &&
        (state.snapshots.length === 0 ? (
          <div className="empty-state">
            <p>
              <strong>No financial snapshots are recorded for this household yet.</strong>
            </p>
            <p>Capture a snapshot first, then come back to compare it with a plan.</p>
          </div>
        ) : (
          <SnapshotPicker householdId={householdId} snapshots={state.snapshots} />
        ))}
    </div>
  );
}

function SnapshotPicker({ householdId, snapshots }: { householdId: string; snapshots: FinancialSnapshotListItem[] }) {
  const [selectedId, setSelectedId] = useState(snapshots[0].id);
  const selectId = useId();
  const selected = snapshots.find((snapshot) => snapshot.id === selectedId) ?? snapshots[0];

  return (
    <section className="calc-section">
      <div className="field">
        <label htmlFor={selectId}>Snapshot to compare against</label>
        <select id={selectId} value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>
          {snapshots.map((snapshot) => (
            <option key={snapshot.id} value={snapshot.id}>
              {formatLocalDate(snapshot.asOfDate)} (captured {formatInstantUtc(snapshot.capturedAt)})
            </option>
          ))}
        </select>
      </div>

      {/* Remounting on snapshot change resets the form and any prior
          comparison result, so a stale result never lingers under a
          newly selected snapshot. */}
      <PlanComparisonForm key={selected.id} householdId={householdId} snapshot={selected} />
    </section>
  );
}

type PlannedRow = { assetTotal: string; liabilityTotal: string };

function PlanComparisonForm({ householdId, snapshot }: { householdId: string; snapshot: FinancialSnapshotListItem }) {
  const [rows, setRows] = useState<Record<string, PlannedRow>>(() =>
    Object.fromEntries(snapshot.totalsByCurrency.map((totals) => [totals.currency, { assetTotal: '', liabilityTotal: '' }]))
  );
  const submit = useCallback(
    (request: PlanVersusActualRequest) => fetchPlanVersusActual(householdId, snapshot.id, request),
    [householdId, snapshot.id]
  );
  const { state, run } = useCalculatorSubmit(submit);
  const headingId = useId();
  const errorId = useId();

  function updateField(currency: string, field: keyof PlannedRow) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setRows((prev) => ({ ...prev, [currency]: { ...prev[currency], [field]: event.target.value } }));
    };
  }

  const parsedRows = snapshot.totalsByCurrency.map((totals) => {
    const row = rows[totals.currency];
    const assetCents = parseNonNegativeCents(row.assetTotal);
    const liabilityCents = parseNonNegativeCents(row.liabilityTotal);
    const netWorth = assetCents !== null && liabilityCents !== null ? centsToMoneyString(assetCents - liabilityCents) : null;
    return { currency: totals.currency, row, netWorth };
  });
  const allRowsValid = parsedRows.every((parsed) => parsed.netWorth !== null);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!allRowsValid) return;
    const plannedMeasures: PlannedCurrencyTotalsInput[] = parsedRows.map((parsed) => ({
      currency: parsed.currency,
      assetTotal: parsed.row.assetTotal.trim(),
      liabilityTotal: parsed.row.liabilityTotal.trim(),
      netWorth: parsed.netWorth!,
    }));
    void run({ plannedMeasures });
  }

  const submitting = state.status === 'submitting';

  return (
    <section className="calc-section" aria-labelledby={headingId}>
      <h2 id={headingId}>Planned totals for {formatLocalDate(snapshot.asOfDate)}</h2>
      <p className="note">Enter a planned asset and liability total for each currency recorded in this snapshot.</p>

      <form onSubmit={handleSubmit}>
        <div className="table-scroll" tabIndex={0} role="region" aria-label="Planned totals by currency">
          <table className="stackable">
            <caption className="sr-only">Planned totals by currency</caption>
            <thead>
              <tr>
                <th scope="col">Currency</th>
                <th scope="col" className="amount">
                  Planned assets
                </th>
                <th scope="col" className="amount">
                  Planned liabilities
                </th>
                <th scope="col" className="amount">
                  Planned net worth
                </th>
              </tr>
            </thead>
            <tbody>
              {parsedRows.map(({ currency, row, netWorth }) => {
                const netWorthDisplay = netWorth === null ? null : formatSignedMoney(netWorth);
                return (
                  <tr key={currency}>
                    <td className="record-title">{currency}</td>
                    <td data-label="Planned assets">
                      <label className="sr-only" htmlFor={`pva-asset-${currency}`}>
                        Planned assets total ({currency})
                      </label>
                      <input
                        id={`pva-asset-${currency}`}
                        type="text"
                        inputMode="decimal"
                        required
                        placeholder="0.00"
                        value={row.assetTotal}
                        onChange={updateField(currency, 'assetTotal')}
                      />
                    </td>
                    <td data-label="Planned liabilities">
                      <label className="sr-only" htmlFor={`pva-liability-${currency}`}>
                        Planned liabilities total ({currency})
                      </label>
                      <input
                        id={`pva-liability-${currency}`}
                        type="text"
                        inputMode="decimal"
                        required
                        placeholder="0.00"
                        value={row.liabilityTotal}
                        onChange={updateField(currency, 'liabilityTotal')}
                      />
                    </td>
                    <td className="amount" data-label="Planned net worth">
                      {netWorthDisplay === null ? (
                        '—'
                      ) : (
                        <span className={netWorthDisplay.negative ? 'negative' : 'positive'}>
                          <span className="sr-only">{netWorthDisplay.negative ? 'negative' : 'positive'}</span>
                          {netWorthDisplay.formatted}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <button
          className="submit-btn"
          type="submit"
          disabled={submitting || !allRowsValid}
          aria-describedby={errorId}
        >
          {submitting ? 'Comparing…' : 'Compare to plan'}
        </button>
      </form>

      {state.status === 'error' && (
        <p className="form-error" id={errorId} role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'success' && <PlanVersusActualResults response={state.data} />}
    </section>
  );
}

function PlanVersusActualResults({ response }: { response: PlanVersusActualResponse }) {
  return (
    <div className="results">
      <p className="note">
        Against the snapshot captured {formatInstantUtc(response.snapshot.capturedAt)} (as of{' '}
        {formatLocalDate(response.snapshot.asOfDate)}). Planned figures above are entered assumptions, not recorded
        fact — only the snapshot&apos;s own totals are actual.
      </p>

      {response.currencyResults.map((result) => (
        <div className="card" key={result.currency}>
          <div className="currency-code">{result.currency}</div>
          <div className="table-scroll" tabIndex={0} role="region" aria-label={`${result.currency} plan vs. actual`}>
            <table className="stackable">
              <caption className="sr-only">{result.currency} plan vs. actual</caption>
              <thead>
                <tr>
                  <th scope="col">Measure</th>
                  <th scope="col" className="amount">
                    Planned
                  </th>
                  <th scope="col" className="amount">
                    Actual
                  </th>
                  <th scope="col" className="amount">
                    Variance (actual − planned)
                  </th>
                </tr>
              </thead>
              <tbody>
                <VarianceRow label="Asset total" variance={result.assetTotal} />
                <VarianceRow label="Liability total" variance={result.liabilityTotal} />
                <VarianceRow label="Net worth" variance={result.netWorth} />
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </div>
  );
}

function VarianceRow({ label, variance }: { label: string; variance: Variance }) {
  const signed = formatSignedAmount(variance.variance);
  const directionLabel = varianceDirectionLabel(variance.direction);

  return (
    <tr>
      <td className="record-title">{label}</td>
      <td className="amount" data-label="Planned">
        {formatAmountMagnitude(variance.planned)}
      </td>
      <td className="amount" data-label="Actual">
        {formatAmountMagnitude(variance.actual)}
      </td>
      <td className="amount" data-label="Variance">
        <span className="variance-value">
          <span className={signed.negative ? 'negative' : 'positive'}>
            <span className="sr-only">{directionLabel}</span>
            {signed.formatted}
          </span>
          <span className="badge">{directionLabel}</span>
        </span>
      </td>
    </tr>
  );
}
