import { ConfigNotFound } from './ConfigProblem';
import { IncomeStreamTable } from './IncomeStreamTable';
import { ObligationTable } from './ObligationTable';
import { LoadingSkeleton } from './LoadingSkeleton';
import { formatTimeUtc } from '../dates';
import { useIncomeAndObligations } from '../hooks/useIncomeAndObligations';

export function IncomeObligationsPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = useIncomeAndObligations(householdId);

  if (state.status === 'not-found') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Income &amp; obligations</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Income &amp; obligations</h1>
          <button className="refresh-btn" type="button" disabled>
            Refresh
          </button>
        </header>
        <p className="status-line" role="status" aria-live="polite">
          Loading income streams and obligations&hellip;
        </p>
        <LoadingSkeleton />
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Income &amp; obligations</h1>
        </header>
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load income streams and obligations.</strong>
          </p>
          <p>{state.message} No records are shown below because none have loaded yet.</p>
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
  const isEmpty = data.incomeStreams.length === 0 && data.obligations.length === 0;

  return (
    <div className="page">
      <header className="app-header">
        <h1>Income &amp; obligations</h1>
        <button className="refresh-btn" type="button" onClick={refresh} disabled={refreshing}>
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>

      {refreshError && (
        <div className="banner" role="alert">
          <strong>Refresh failed at {formatTimeUtc(refreshError.failedAt)}.</strong>
          Showing the results from the last successful refresh below. Select &ldquo;Refresh&rdquo; to try again.
        </div>
      )}

      {isEmpty ? (
        <div className="empty-state">
          <p>
            <strong>No income streams or obligations are recorded for this household yet.</strong>
          </p>
          <p>Add records through the existing household data tools, then refresh this page.</p>
        </div>
      ) : (
        <div className={`card${refreshError ? ' dimmed' : ''}`}>
          <div className="records">
            <h4>Income streams{data.incomeStreams.length > 0 ? ` (${data.incomeStreams.length})` : ''}</h4>
            {data.incomeStreams.length > 0 ? (
              <IncomeStreamTable incomeStreams={data.incomeStreams} />
            ) : (
              <p className="no-records-side">No income streams recorded.</p>
            )}

            <h4>Recurring obligations{data.obligations.length > 0 ? ` (${data.obligations.length})` : ''}</h4>
            {data.obligations.length > 0 ? (
              <ObligationTable obligations={data.obligations} />
            ) : (
              <p className="no-records-side">No recurring obligations recorded.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
