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
  error:
    | 'HOUSEHOLD_NOT_FOUND'
    | 'MALFORMED_REQUEST'
    | 'VALIDATION_FAILED'
    | 'FINANCIAL_SNAPSHOT_NOT_FOUND'
    | string;
  message: string;
  details: unknown[];
}

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/{CurrencyTotalsResponse,
// FinancialSnapshotResponse,FinancialSnapshotSummaryResponse,PlanVersusActual*}.java.
// Like the planning-calculator DTOs above (and unlike the position DTOs), these
// monetary fields are plain BigDecimal with no MoneyFormat conversion, so they
// arrive as JSON numbers — read with ../calculatorMoney.ts, not ../money.ts.
// The one exception is the *request* side (PlannedCurrencyTotalsInput below):
// those fields are sent as trimmed form-input strings, which the backend's
// BigDecimal deserializer accepts directly, exactly like the calculator
// requests above.

export interface SnapshotCurrencyTotals {
  currency: string;
  assetTotal: number;
  liabilityTotal: number;
  netWorth: number;
}

/**
 * One entry from `GET /api/households/{householdId}/financial-snapshots`, used
 * only to populate the plan-vs-actual snapshot picker. The full response also
 * carries householdId, sourceType, and per-record line items — deliberately
 * omitted here since the picker never reads them.
 */
export interface FinancialSnapshotListItem {
  id: string;
  asOfDate: string;
  capturedAt: string;
  totalsByCurrency: SnapshotCurrencyTotals[];
}

/** The snapshot identity embedded in a PlanVersusActualResponse — no totals. */
export interface FinancialSnapshotSummary {
  id: string;
  asOfDate: string;
  capturedAt: string;
}

export interface PlannedCurrencyTotalsInput {
  currency: string;
  assetTotal: string;
  liabilityTotal: string;
  netWorth: string;
}

export interface PlanVersusActualRequest {
  plannedMeasures: PlannedCurrencyTotalsInput[];
}

export type VarianceDirection = 'ABOVE_PLAN' | 'BELOW_PLAN' | 'ON_PLAN';

export interface Variance {
  planned: number;
  actual: number;
  variance: number;
  direction: VarianceDirection;
}

export interface CurrencyPlanVersusActual {
  currency: string;
  assetTotal: Variance;
  liabilityTotal: Variance;
  netWorth: Variance;
}

export interface PlanVersusActualResponse {
  snapshot: FinancialSnapshotSummary;
  currencyResults: CurrencyPlanVersusActual[];
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
