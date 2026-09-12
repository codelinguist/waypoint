import { ConfigNotFound } from './ConfigProblem';
import { LoadingSkeleton } from './LoadingSkeleton';
import { AddPlanningAssumptionForm } from './entry/AddPlanningAssumptionForm';
import { SupersedePlanningAssumptionForm } from './entry/SupersedePlanningAssumptionForm';
import { formatLocalDate, formatTimeUtc } from '../dates';
import { usePlanningAssumptions } from '../hooks/usePlanningAssumptions';
import type { PlanningAssumption } from '../api/types';

export function PlanningAssumptionsPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = usePlanningAssumptions(householdId);

  if (state.status === 'not-found') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Planning assumptions</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Planning assumptions</h1>
          <button className="refresh-btn" type="button" disabled>
            Refresh
          </button>
        </header>
        <p className="status-line" role="status" aria-live="polite">
          Loading planning assumptions&hellip;
        </p>
        <LoadingSkeleton />
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Planning assumptions</h1>
        </header>
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load planning assumptions.</strong>
          </p>
          <p>{state.message} No assumptions are shown below because none have loaded yet.</p>
          <p>
            <button className="refresh-btn" type="button" onClick={retryInitial}>
              Retry
            </button>
          </p>
        </div>
      </div>
    );
  }

  const { data, refreshing, refreshError } = state;

  return (
    <div className="page">
      <header className="app-header">
        <h1>Planning assumptions</h1>
        <button className="refresh-btn" type="button" onClick={refresh} disabled={refreshing}>
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>

      <AddPlanningAssumptionForm householdId={householdId} onCreated={refresh} />

      {refreshError && (
        <div className="banner" role="alert">
          <strong>Refresh failed at {formatTimeUtc(refreshError.failedAt)}.</strong>
          Showing the results from the last successful refresh below. Select &ldquo;Refresh&rdquo; to try again.
        </div>
      )}

      {data.length === 0 ? (
        <div className="empty-state">
          <p>
            <strong>No planning assumptions are recorded for this household yet.</strong>
          </p>
          <p>Use &ldquo;Add assumption&rdquo; above to record one.</p>
        </div>
      ) : (
        <div className={`card${refreshError ? ' dimmed' : ''}`}>
          <div className="table-scroll" tabIndex={0} role="region" aria-label="Planning assumptions, scrollable">
            <table className="stackable">
              <caption className="sr-only">Planning assumptions</caption>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Value type</th>
                  <th scope="col">Value</th>
                  <th scope="col">Effective from</th>
                  <th scope="col">Effective until</th>
                  <th scope="col">Review date</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.map((assumption) => (
                  <AssumptionRow key={assumption.id} householdId={householdId} assumption={assumption} onSuperseded={refresh} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function AssumptionRow({
  householdId,
  assumption,
  onSuperseded,
}: {
  householdId: string;
  assumption: PlanningAssumption;
  onSuperseded: () => void;
}) {
  const superseded = assumption.supersededBy !== null;

  return (
    <tr>
      <td className="record-title">{assumption.name}</td>
      <td data-label="Value type">{assumption.valueType}</td>
      <td data-label="Value">{assumption.value}</td>
      <td data-label="Effective from">
        <time dateTime={assumption.effectiveFrom}>{formatLocalDate(assumption.effectiveFrom)}</time>
      </td>
      <td data-label="Effective until">
        {assumption.effectiveUntil ? (
          <time dateTime={assumption.effectiveUntil}>{formatLocalDate(assumption.effectiveUntil)}</time>
        ) : (
          '—'
        )}
      </td>
      <td data-label="Review date">
        <time dateTime={assumption.reviewDate}>{formatLocalDate(assumption.reviewDate)}</time>
      </td>
      <td data-label="Status">
        <span className="badge">{superseded ? 'Superseded' : 'Current'}</span>
      </td>
      <td data-label="Actions">
        {superseded ? (
          <span className="no-records-side">—</span>
        ) : (
          <SupersedePlanningAssumptionForm householdId={householdId} assumption={assumption} onSuperseded={onSuperseded} />
        )}
      </td>
    </tr>
  );
}
