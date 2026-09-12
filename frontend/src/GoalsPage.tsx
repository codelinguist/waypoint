import { ConfigNotFound } from './components/ConfigProblem';
import { GoalContributionCalculator } from './components/GoalContributionCalculator';
import { GoalsTable } from './components/GoalsTable';
import { LoadingSkeleton } from './components/LoadingSkeleton';
import { formatTimeUtc } from './dates';
import { useGoals } from './hooks/useGoals';

export function GoalsPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = useGoals(householdId);

  if (state.status === 'not-found') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Goals</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Goals</h1>
          <button className="refresh-btn" type="button" disabled>
            Refresh
          </button>
        </header>
        <p className="status-line" role="status" aria-live="polite">
          Loading goals&hellip;
        </p>
        <LoadingSkeleton />
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Goals</h1>
        </header>
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load goals.</strong>
          </p>
          <p>{state.message} No goals are shown below because none have loaded yet.</p>
          <p>
            <button className="refresh-btn" type="button" onClick={retryInitial}>
              Retry
            </button>
          </p>
        </div>
        <GoalContributionCalculator goals={[]} />
      </div>
    );
  }

  const { data, refreshing, refreshError } = state;

  return (
    <div className="page">
      <header className="app-header">
        <h1>Goals</h1>
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

      {data.length === 0 ? (
        <div className="empty-state">
          <p>
            <strong>No goals are recorded for this household yet.</strong>
          </p>
          <p>Add goals through the existing household data tools, then refresh this page.</p>
        </div>
      ) : (
        <GoalsTable goals={data} />
      )}

      <GoalContributionCalculator goals={data} />
    </div>
  );
}
