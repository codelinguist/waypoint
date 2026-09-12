// Mirrors backend/src/main/java/com/waypoint/position/web/dto/*.java exactly.
// Every monetary field is an exact decimal string ("1234.56") — never a
// JSON/JS number. See ../money.ts for the presentation-only formatter that
// must be used instead of parseFloat/Number on these fields.

export type AssetType =
  | 'CASH'
  | 'BANK_ACCOUNT'
  | 'PROPERTY'
  | 'INVESTMENT'
  | 'BUSINESS_OWNERSHIP'
  | 'OTHER';

export type LiabilityType =
  | 'CREDIT_CARD'
  | 'MORTGAGE'
  | 'PERSONAL_LOAN'
  | 'BUSINESS_LOAN'
  | 'OTHER';

export type Liquidity = 'LIQUID' | 'RESTRICTED' | 'ILLIQUID';

export type SourceType = 'MANUAL_ENTRY';

export interface PositionAsset {
  id: string;
  name: string;
  assetType: AssetType;
  estimatedValue: string;
  planningValue: string;
  currency: string;
  valuedAt: string;
  liquidity: Liquidity;
  sourceType: SourceType;
}

export interface PositionLiability {
  id: string;
  name: string;
  liabilityType: LiabilityType;
  outstandingBalance: string;
  currency: string;
  balanceAsOf: string;
  sourceType: SourceType;
}

export interface PositionCurrencyTotals {
  currency: string;
  assetTotal: string;
  liabilityTotal: string;
  netWorth: string;
}

export interface FinancialPositionResponse {
  householdId: string;
  householdName: string;
  baseCurrency: string;
  retrievedAt: string;
  assets: PositionAsset[];
  liabilities: PositionLiability[];
  totalsByCurrency: PositionCurrencyTotals[];
}

export interface ApiErrorBody {
  error: 'HOUSEHOLD_NOT_FOUND' | 'MALFORMED_REQUEST' | 'VALIDATION_FAILED' | string;
  message: string;
  details: unknown[];
}

// Mirrors backend/src/main/java/com/waypoint/planning/{cashflow,runway}/web/dto/*.java.
// Unlike the position DTOs above, these are stateless calculators whose DTOs
// declare monetary fields as BigDecimal, which Jackson serializes as a JSON
// number (not a decimal string) with no custom serializer in this module —
// so, unlike money.ts, these are read as `number` and formatted with
// ../calculatorMoney.ts. Both endpoints take a request body only: no
// householdId, no persisted household data read or written.

export interface CashFlowProjectionRequest {
  currency: string;
  startMonth: string;
  startingCash: string;
  monthlyInflow: string;
  monthlyOutflow: string;
  months: number;
}

export interface CashFlowProjectionRow {
  month: string;
  openingCash: number;
  inflow: number;
  outflow: number;
  netCashFlow: number;
  closingCash: number;
}

export type CashFlowProjectionStatus = 'REMAINS_NONNEGATIVE' | 'BECOMES_NEGATIVE';

export interface CashFlowProjectionResponse {
  currency: string;
  startMonth: string;
  startingCash: number;
  monthlyInflow: number;
  monthlyOutflow: number;
  months: number;
  rows: CashFlowProjectionRow[];
  endingCash: number;
  lowestClosingBalance: number;
  lowestClosingBalanceMonth: string;
  firstNegativeMonth: string | null;
  status: CashFlowProjectionStatus;
}

export interface EmergencyFundRunwayRequest {
  availableReserve: string;
  monthlyExpenses: string;
  monthlyNetIncome: string;
  currency: string;
}

export type RunwayStatus = 'FINITE' | 'NO_SHORTFALL';

export interface EmergencyFundRunwayResponse {
  currency: string;
  availableReserve: number;
  monthlyExpenses: number;
  monthlyNetIncome: number;
  monthlyShortfall: number;
  status: RunwayStatus;
  runwayMonths: number | null;
  fullMonthsCovered: number | null;
  modelNote: string;
}

// Mirrors backend/src/main/java/com/waypoint/scenarios/{purchasereserve,
// incomeinterruption,debtprepayment}/web/dto/*.java. Same stateless,
// no-householdId convention and JSON-number money encoding as the planning
// calculators above — format with ../calculatorMoney.ts, not money.ts.

export interface PurchaseReserveImpactRequest {
  currency: string;
  availableReserve: string;
  purchaseAmount: string;
  monthlyExpenses: string;
  monthlyNetIncome: string;
  minimumReserve: string;
}

export type AfterPurchaseRunwayAvailability = 'AVAILABLE' | 'INSUFFICIENT_CASH';

export interface PurchaseReserveImpactResponse {
  currency: string;
  availableReserve: number;
  purchaseAmount: number;
  monthlyExpenses: number;
  monthlyNetIncome: number;
  minimumReserve: number;
  reserveAfterPurchase: number;
  purchaseFundingGap: number;
  purchaseFitsAvailableCash: boolean;
  baselineReserveFloorGap: number;
  reserveFloorGapAfterPurchase: number;
  reserveMeetsFloorAfterPurchase: boolean;
  beforePurchaseRunway: EmergencyFundRunwayResponse;
  afterPurchaseRunwayAvailability: AfterPurchaseRunwayAvailability;
  afterPurchaseRunway: EmergencyFundRunwayResponse | null;
  modelNote: string;
}

export interface IncomeInterruptionScenarioRequest {
  currency: string;
  openingReserve: string;
  normalMonthlyNetIncome: string;
  interruptedMonthlyNetIncome: string;
  monthlyExpenses: string;
  horizonMonths: number;
  interruptionStartMonth: number;
  interruptionMonths: number;
}

export interface IncomeInterruptionScenarioRow {
  month: number;
  openingCash: number;
  income: number;
  expenses: number;
  netCashFlow: number;
  closingCash: number;
}

export interface IncomeInterruptionScenarioResponse {
  currency: string;
  openingReserve: number;
  normalMonthlyNetIncome: number;
  interruptedMonthlyNetIncome: number;
  monthlyExpenses: number;
  horizonMonths: number;
  interruptionStartMonth: number;
  interruptionMonths: number;
  baselineRows: IncomeInterruptionScenarioRow[];
  scenarioRows: IncomeInterruptionScenarioRow[];
  closingDeltas: number[];
  endingCash: number;
  minimumCash: number;
  firstNegativeMonth: number | null;
  additionalOpeningReserveNeeded: number;
}

export interface DebtPrepaymentComparisonRequest {
  principal: string;
  monthlyInterestRate: string;
  monthlyPayment: string;
  currency: string;
  immediatePrepayment: string;
}

export type DebtAmortizationStatus = 'PAID_OFF' | 'NON_AMORTIZING' | 'HORIZON_LIMIT';

// The backend also returns a full monthly `schedule` on each path, omitted
// here: this view renders only the lifetime comparison, not a month-by-month
// amortization table (see DebtPrepaymentComparisonSection).
export interface DebtPrepaymentPathResponse {
  startingBalance: number;
  status: DebtAmortizationStatus;
  payoffMonths: number | null;
  totalPaid: number;
  totalInterest: number;
  remainingBalance: number;
}

export interface DebtPrepaymentComparisonResponse {
  principal: number;
  monthlyInterestRate: number;
  monthlyPayment: number;
  currency: string;
  immediatePrepayment: number;
  baseline: DebtPrepaymentPathResponse;
  scenario: DebtPrepaymentPathResponse;
  scenarioTotalCashPaid: number;
  lifetimeInterestSaved: number | null;
  payoffMonthsSaved: number | null;
  lifetimeCashSaved: number | null;
  comparisonUnavailableReason: string | null;
}
