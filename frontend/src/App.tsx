import { useState } from 'react';
import { readHouseholdConfig } from './config';
import { ConfigInvalid, ConfigMissing, ConfigNotFound } from './components/ConfigProblem';
import { CurrencyCard } from './components/CurrencyCard';
import { AddAssetForm } from './components/entry/AddAssetForm';
import { AddLiabilityForm } from './components/entry/AddLiabilityForm';
import { CreateSnapshotForm } from './components/entry/CreateSnapshotForm';
import { ForecastingPage } from './components/ForecastingPage';
import { IncomeObligationsPage } from './components/IncomeObligationsPage';
import { LoadingSkeleton } from './components/LoadingSkeleton';
import { PlanningAssumptionsPage } from './components/PlanningAssumptionsPage';
import { PlanVersusActualPage } from './components/PlanVersusActualPage';
import { ScenariosPage } from './components/ScenariosPage';
import { SnapshotComparisonView } from './components/SnapshotComparisonView';
import { SnapshotList } from './components/SnapshotList';
import { GoalsPage } from './GoalsPage';
import { useFinancialPosition } from './hooks/useFinancialPosition';
import { useFinancialSnapshotDetails } from './hooks/useFinancialSnapshotDetails';
import { formatInstantUtc, formatTimeUtc } from './dates';

type View =
  | 'position'
  | 'goals'
  | 'forecasting'
  | 'plan-vs-actual'
  | 'income-obligations'
  | 'snapshots'
  | 'scenarios'
  | 'assumptions';

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

      <AddAssetForm householdId={householdId} onCreated={refresh} />
      <AddLiabilityForm householdId={householdId} onCreated={refresh} />

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
          <p>Use &ldquo;Add asset&rdquo; or &ldquo;Add liability&rdquo; above to record one.</p>
        </div>
      ) : (
        data.totalsByCurrency.map((totals) => (
          <CurrencyCard
            key={totals.currency}
            totals={totals}
            assets={data.assets.filter((asset) => asset.currency === totals.currency)}
            liabilities={data.liabilities.filter((liability) => liability.currency === totals.currency)}
            dimmed={Boolean(refreshError)}
            householdId={householdId}
            onRecordChanged={refresh}
          />
        ))
      )}
    </div>
  );
}

function SnapshotsPage({ householdId }: { householdId: string }) {
  const { state, retry } = useFinancialSnapshotDetails(householdId);

  if (state.status === 'not-found') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Snapshots</h1>
        </header>
        <ConfigNotFound householdId={state.householdId} />
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Snapshots</h1>
        </header>
        <p className="status-line" role="status" aria-live="polite">
          Loading snapshots&hellip;
        </p>
        <LoadingSkeleton />
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="page">
        <header className="app-header">
          <h1>Snapshots</h1>
        </header>
        <div className="empty-state" role="alert">
          <p>
            <strong>Could not load snapshots.</strong>
          </p>
          <p>{state.message}</p>
          <p>
            <button className="refresh-btn" type="button" onClick={retry}>
              Retry
            </button>
          </p>
        </div>
      </div>
    );
  }

  const { snapshots } = state;

  return (
    <div className="page">
      <header className="app-header">
        <h1>Snapshots</h1>
      </header>

      <CreateSnapshotForm householdId={householdId} onCreated={retry} />

      {snapshots.length === 0 ? (
        <div className="empty-state">
          <p>
            <strong>No financial snapshots are recorded for this household yet.</strong>
          </p>
          <p>Use &ldquo;Create snapshot&rdquo; above to record one.</p>
        </div>
      ) : (
        <>
          <SnapshotList snapshots={snapshots} />
          <SnapshotComparisonView householdId={householdId} snapshots={snapshots} />
        </>
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
      <button type="button" aria-current={view === 'snapshots' ? 'page' : undefined} onClick={() => onSelect('snapshots')}>
        Snapshots
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
      <button
        type="button"
        aria-current={view === 'assumptions' ? 'page' : undefined}
        onClick={() => onSelect('assumptions')}
      >
        Planning assumptions
      </button>
      <button
        type="button"
        aria-current={view === 'scenarios' ? 'page' : undefined}
        onClick={() => onSelect('scenarios')}
      >
        Scenarios
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

  if (view === 'scenarios') {
    return (
      <>
        <AppNav view={view} onSelect={setView} />
        <ScenariosPage />
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
      {view === 'snapshots' && <SnapshotsPage householdId={config.householdId} />}
      {view === 'assumptions' && <PlanningAssumptionsPage householdId={config.householdId} />}
      {view === 'position' && <FinancialPositionPage householdId={config.householdId} />}
    </>
  );
}
