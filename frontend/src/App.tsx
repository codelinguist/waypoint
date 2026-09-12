import { useState } from 'react';
import { readHouseholdConfig } from './config';
import { ConfigInvalid, ConfigMissing, ConfigNotFound } from './components/ConfigProblem';
import { CurrencyCard } from './components/CurrencyCard';
import { IncomeStreamTable } from './components/IncomeStreamTable';
import { ObligationTable } from './components/ObligationTable';
import { LoadingSkeleton } from './components/LoadingSkeleton';
import { useFinancialPosition } from './hooks/useFinancialPosition';
import { useIncomeAndObligations } from './hooks/useIncomeAndObligations';
import { formatInstantUtc, formatTimeUtc } from './dates';

const EXPLAINER =
  'Net worth is recorded asset planning values minus outstanding liability balances. ' +
  'Records keep their own valuation or balance dates; refreshing does not update those dates.';

type View = 'financial-position' | 'income-obligations';

const VIEWS: { id: View; label: string }[] = [
  { id: 'financial-position', label: 'Financial position' },
  { id: 'income-obligations', label: 'Income & obligations' },
];

function AppNav({ view, onSelect }: { view: View; onSelect: (view: View) => void }) {
  return (
    <nav className="app-nav" aria-label="Sections">
      <div role="tablist" aria-label="Sections">
        {VIEWS.map((candidate) => (
          <button
            key={candidate.id}
            type="button"
            role="tab"
            aria-selected={view === candidate.id}
            className={`tab-btn${view === candidate.id ? ' active' : ''}`}
            onClick={() => onSelect(candidate.id)}
          >
            {candidate.label}
          </button>
        ))}
      </div>
    </nav>
  );
}

function FinancialPositionPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = useFinancialPosition(householdId);

  if (state.status === 'not-found') {
    return (
      <>
        <header className="app-header">
          <h1>Financial position</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </>
    );
  }

  if (state.status === 'loading') {
    return (
      <>
        <header className="app-header">
          <h1>Financial position</h1>
          <button className="refresh-btn" type="button" disabled>
            Refresh
          </button>
        </header>
        <p className="status-line" role="status" aria-live="polite">
          Loading financial position&hellip;
        </p>
        <LoadingSkeleton />
      </>
    );
  }

  if (state.status === 'error') {
    return (
      <>
        <header className="app-header">
          <h1>Financial position</h1>
        </header>
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load financial position.</strong>
          </p>
          <p>{state.message} No figures are shown below because none have loaded yet.</p>
          <p>
            <button className="refresh-btn" type="button" onClick={retryInitial}>
              Retry
            </button>
          </p>
        </div>
      </>
    );
  }

  const { data, refreshing, refreshError } = state;
  const isEmptyHousehold = data.totalsByCurrency.length === 0;

  return (
    <>
      <header className="app-header">
        <h1>{data.householdName}</h1>
        <button className="refresh-btn" type="button" onClick={refresh} disabled={refreshing}>
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>
      <p className="status-line" role="status" aria-live="polite">
        {refreshError ? `Showing results from ${formatInstantUtc(data.retrievedAt)}` : `Retrieved ${formatInstantUtc(data.retrievedAt)}`}
      </p>

      {!isEmptyHousehold && <p className="explainer">{EXPLAINER}</p>}

      {refreshError && (
        <div className="banner" role="alert">
          <strong>Refresh failed at {formatTimeUtc(refreshError.failedAt)}.</strong>
          Showing the results from the last successful refresh below. Select &ldquo;Refresh&rdquo; to try again.
        </div>
      )}

      {isEmptyHousehold ? (
        <div className="empty-state">
          <p>
            <strong>No assets or liabilities are recorded for this household yet.</strong>
          </p>
          <p>Add records through the existing household data tools, then refresh this page.</p>
        </div>
      ) : (
        data.totalsByCurrency.map((totals) => (
          <CurrencyCard
            key={totals.currency}
            totals={totals}
            assets={data.assets.filter((asset) => asset.currency === totals.currency)}
            liabilities={data.liabilities.filter((liability) => liability.currency === totals.currency)}
            dimmed={Boolean(refreshError)}
          />
        ))
      )}
    </>
  );
}

function IncomeObligationsPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = useIncomeAndObligations(householdId);

  if (state.status === 'not-found') {
    return (
      <>
        <header className="app-header">
          <h1>Income &amp; obligations</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </>
    );
  }

  if (state.status === 'loading') {
    return (
      <>
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
      </>
    );
  }

  if (state.status === 'error') {
    return (
      <>
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
      </>
    );
  }

  const { data, refreshing, refreshError } = state;
  const isEmpty = data.incomeStreams.length === 0 && data.obligations.length === 0;

  return (
    <>
      <header className="app-header">
        <h1>Income &amp; obligations</h1>
        <button className="refresh-btn" type="button" onClick={refresh} disabled={refreshing}>
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>
      <p className="status-line" role="status" aria-live="polite">
        {refreshError ? 'Showing previously loaded results' : 'Loaded'}
      </p>

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
    </>
  );
}

export function App() {
  const config = readHouseholdConfig();
  const [view, setView] = useState<View>('financial-position');

  if (config.status === 'missing') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Financial position</h1>
        </header>
        <ConfigMissing />
      </div>
    );
  }

  if (config.status === 'invalid') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Financial position</h1>
        </header>
        <ConfigInvalid rawValue={config.rawValue} />
      </div>
    );
  }

  return (
    <div className="page">
      <AppNav view={view} onSelect={setView} />
      {view === 'financial-position' ? (
        <FinancialPositionPage householdId={config.householdId} />
      ) : (
        <IncomeObligationsPage householdId={config.householdId} />
      )}
    </div>
  );
}
