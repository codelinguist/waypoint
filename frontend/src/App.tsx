import { useState } from 'react';
import { readHouseholdConfig } from './config';
import { ConfigInvalid, ConfigMissing, ConfigNotFound } from './components/ConfigProblem';
import { CurrencyCard } from './components/CurrencyCard';
import { ForecastingPage } from './components/ForecastingPage';
import { IncomeObligationsPage } from './components/IncomeObligationsPage';
import { LoadingSkeleton } from './components/LoadingSkeleton';
import { GoalsPage } from './GoalsPage';
import { PlanVersusActualPage } from './components/PlanVersusActualPage';
import { useFinancialPosition } from './hooks/useFinancialPosition';
import { formatInstantUtc, formatTimeUtc } from './dates';

type View = 'position' | 'goals' | 'forecasting' | 'plan-vs-actual' | 'income-obligations';

const EXPLAINER =
  'Net worth is recorded asset planning values minus outstanding liability balances. ' +
  'Records keep their own valuation or balance dates; refreshing does not update those dates.';

function FinancialPositionPage({ householdId }: { householdId: string }) {
  const { state, refresh, retryInitial } = useFinancialPosition(householdId);

  if (state.status === 'not-found') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Financial position</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="page">
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
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="page">
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
      </div>
    );
  }

  const { data, refreshing, refreshError } = state;
  const isEmptyHousehold = data.totalsByCurrency.length === 0;

  return (
    <div className="page">
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
    </div>
  );
}

function AppNav({ view, onSelect }: { view: View; onSelect: (view: View) => void }) {
  return (
    <nav className="app-nav" aria-label="Sections">
      <button type="button" aria-current={view === 'position' ? 'page' : undefined} onClick={() => onSelect('position')}>
        Financial position
      </button>
      <button type="button" aria-current={view === 'goals' ? 'page' : undefined} onClick={() => onSelect('goals')}>
        Goals
      </button>
      <button
        type="button"
        aria-current={view === 'income-obligations' ? 'page' : undefined}
        onClick={() => onSelect('income-obligations')}
      >
        Income &amp; obligations
      </button>
      <button
        type="button"
        aria-current={view === 'forecasting' ? 'page' : undefined}
        onClick={() => onSelect('forecasting')}
      >
        Forecasting
      </button>
      <button
        type="button"
        aria-current={view === 'plan-vs-actual' ? 'page' : undefined}
        onClick={() => onSelect('plan-vs-actual')}
      >
        Plan vs. actual
      </button>
    </nav>
  );
}

export function App() {
  const [view, setView] = useState<View>('position');
  const config = readHouseholdConfig();

  if (view === 'forecasting') {
    return (
      <>
        <AppNav view={view} onSelect={setView} />
        <ForecastingPage />
      </>
    );
  }

  if (config.status === 'missing') {
    return (
      <>
        <AppNav view={view} onSelect={setView} />
        <div className="page">
          <header className="app-header">
            <h1>Financial position</h1>
          </header>
          <ConfigMissing />
        </div>
      </>
    );
  }

  if (config.status === 'invalid') {
    return (
      <>
        <AppNav view={view} onSelect={setView} />
        <div className="page">
          <header className="app-header">
            <h1>Financial position</h1>
          </header>
          <ConfigInvalid rawValue={config.rawValue} />
        </div>
      </>
    );
  }

  if (view === 'plan-vs-actual') {
    return (
      <>
        <AppNav view={view} onSelect={setView} />
        <PlanVersusActualPage householdId={config.householdId} />
      </>
    );
  }

  return (
    <>
      <AppNav view={view} onSelect={setView} />
      {view === 'goals' && <GoalsPage householdId={config.householdId} />}
      {view === 'income-obligations' && <IncomeObligationsPage householdId={config.householdId} />}
      {view === 'position' && <FinancialPositionPage householdId={config.householdId} />}
    </>
  );
}
