import { DebtPrepaymentComparisonSection } from './DebtPrepaymentComparisonSection';
import { IncomeInterruptionScenarioSection } from './IncomeInterruptionScenarioSection';
import { PurchaseReserveImpactSection } from './PurchaseReserveImpactSection';

export function ScenariosPage() {
  return (
    <div className="page">
      <header className="app-header">
        <h1>What-if scenarios</h1>
      </header>
      <p className="status-line">
        All three scenarios below are stateless explorations: nothing you enter is saved, none of them read your
        recorded financial position, and none of their results represent an actual household decision.
      </p>

      <PurchaseReserveImpactSection />
      <IncomeInterruptionScenarioSection />
      <DebtPrepaymentComparisonSection />
    </div>
  );
}
