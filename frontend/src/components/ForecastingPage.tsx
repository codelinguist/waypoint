import { CashFlowProjectionSection } from './CashFlowProjectionSection';
import { EmergencyFundRunwaySection } from './EmergencyFundRunwaySection';

export function ForecastingPage() {
  return (
    <div className="page">
      <header className="app-header">
        <h1>Forecasting</h1>
      </header>
      <p className="status-line">
        Both calculators below are stateless: nothing you enter is saved, and neither reads your recorded financial
        position.
      </p>

      <CashFlowProjectionSection />
      <EmergencyFundRunwaySection />
    </div>
  );
}
