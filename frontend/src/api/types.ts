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

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/FinancialGoalResponse.java
// and backend/src/main/java/com/waypoint/planning/goalcontribution/web/dto/*.java.
// Unlike PositionAsset/PositionLiability/PositionCurrencyTotals above, these
// two endpoints serialize BigDecimal fields as plain JSON numbers, not exact
// decimal strings (confirmed by agent/product/current-financial-position/api.md:
// "Existing endpoints ... continue to serialize money as JSON numbers"). Use
// ../moneyNumber.ts to format them, not ../money.ts.

export interface FinancialGoal {
  id: string;
  householdId: string;
  name: string;
  targetAmount: number;
  currency: string;
  targetDate: string;
  priority: number;
  currentAmount: number;
  remainingAmount: number;
  progressPercentage: number;
  sourceType: SourceType;
  createdAt: string;
  updatedAt: string;
}

export type GoalContributionStatus = 'ALREADY_FUNDED' | 'CONTRIBUTIONS_REQUIRED';

/**
 * Request body for POST /api/planning/goal-contribution-calculator. Amount
 * fields are sent as decimal strings (the backend's BigDecimal fields accept
 * either a JSON string or number; sending the user's typed string directly
 * avoids ever routing it through a JS `Number`). `contributionMonths` must
 * stay a genuine JSON integer — the backend's WholeNumberDeserializer rejects
 * a string token outright, since it exists specifically to reject anything
 * that isn't already a whole JSON number.
 */
export interface GoalContributionRequestBody {
  currency: string;
  targetAmount: string;
  currentAmount: string;
  contributionMonths: number;
}

export interface GoalContributionResult {
  currency: string;
  targetAmount: number;
  currentAmount: number;
  contributionMonths: number;
  remainingAmount: number;
  monthlyContribution: number;
  totalContributions: number;
  projectedAmount: number;
  amountAboveTarget: number;
  status: GoalContributionStatus;
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

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/{IncomeStreamResponse,ObligationResponse}.java.
// Unlike the position types above, `amount` here is a plain JSON number, not
// a decimal string — this API (agent/product/income-obligations/product-brief.md)
// predates the decimal-string transport convention, same situation as the
// goals/calculator types above. Use ../moneyNumber.ts's `formatMoneyNumber`
// (not ../money.ts) to present it.

export type IncomeType = 'SALARY' | 'HOURLY_CONTRACT' | 'BUSINESS_DISTRIBUTION' | 'OTHER';

export type ObligationType =
  | 'HOUSEHOLD_BASELINE'
  | 'MORTGAGE'
  | 'LOAN_PAYMENT'
  | 'INSURANCE'
  | 'TUITION'
  | 'TRAVEL_SINKING_FUND'
  | 'DISCRETIONARY'
  | 'OTHER';

export type Frequency = 'HOURLY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'ANNUAL';

export type IncomeCertainty = 'CONFIRMED' | 'EXPECTED' | 'VARIABLE';

export type CompensationClassification = 'GROSS' | 'NET' | 'UNKNOWN';

export interface IncomeStream {
  id: string;
  householdId: string;
  name: string;
  incomeType: IncomeType;
  amount: number;
  frequency: Frequency;
  currency: string;
  compensationClassification: CompensationClassification;
  certainty: IncomeCertainty;
  startDate: string;
  endDate: string | null;
  sourceType: SourceType;
  createdAt: string;
  updatedAt: string;
}

export interface Obligation {
  id: string;
  householdId: string;
  name: string;
  obligationType: ObligationType;
  amount: number;
  frequency: Frequency;
  currency: string;
  startDate: string;
  endDate: string | null;
  sourceType: SourceType;
  createdAt: string;
  updatedAt: string;
}

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/{FinancialSnapshotResponse,
// SnapshotAssetLineItem,SnapshotLiabilityLineItem,CurrencyTotals,
// CurrencyTotalsDelta}Response.java exactly. FinancialSnapshotDetail is the
// full-detail counterpart to FinancialSnapshotListItem above (which the
// plan-vs-actual picker uses); this ticket's snapshots list view needs
// sourceType and line items that the picker's narrower type omits, even
// though both types describe the same GET /financial-snapshots response.
//
// These fields serialize as plain JSON numbers, not exact-decimal strings
// (an existing, unchanged contract — see
// agent/product/current-financial-position/api.md). Use ../snapshotMoney.ts
// to format them.

export interface SnapshotAssetLineItem {
  id: string;
  sourceAssetId: string;
  name: string;
  assetType: AssetType;
  currency: string;
  sourceDate: string;
  value: number;
}

export interface SnapshotLiabilityLineItem {
  id: string;
  sourceLiabilityId: string;
  name: string;
  liabilityType: LiabilityType;
  currency: string;
  sourceDate: string;
  value: number;
}

export interface FinancialSnapshotCurrencyTotals {
  currency: string;
  assetTotal: number;
  liabilityTotal: number;
  netWorth: number;
}

export interface FinancialSnapshotDetail {
  id: string;
  householdId: string;
  asOfDate: string;
  capturedAt: string;
  sourceType: SourceType;
  assetLineItems: SnapshotAssetLineItem[];
  liabilityLineItems: SnapshotLiabilityLineItem[];
  totalsByCurrency: FinancialSnapshotCurrencyTotals[];
}

export interface SnapshotComparisonSummary {
  id: string;
  asOfDate: string;
  capturedAt: string;
}

export interface SnapshotCurrencyTotalsDelta {
  currency: string;
  assetTotalDelta: number;
  liabilityTotalDelta: number;
  netWorthDelta: number;
}

export interface FinancialSnapshotComparison {
  earlierSnapshot: SnapshotComparisonSummary;
  laterSnapshot: SnapshotComparisonSummary;
  currencyDeltas: SnapshotCurrencyTotalsDelta[];
}

// Data-entry request/response types (WAP-25). Mirrors
// backend/src/main/java/com/waypoint/household/web/dto/Create*Request.java,
// Update AssetValuationRequest, RecordLiabilityBalanceRequest, and
// backend/src/main/java/com/waypoint/assumption/web/dto/*.java. Every field
// here is a plain create/list DTO with no MoneyFormat conversion, so
// `Asset`/`Liability` monetary fields below are plain JSON numbers — the one
// exception is `AssetValuationState`, which the backend does serialize with
// MoneyFormat (see AssetValuationStateResponse.java) as exact decimal
// strings, matching PositionAsset's convention.

export interface CreateAssetRequest {
  name: string;
  assetType: AssetType;
  estimatedValue: string;
  planningValue: string;
  currency: string;
  valuedAt: string;
  liquidity: Liquidity;
}

export interface Asset {
  id: string;
  householdId: string;
  name: string;
  assetType: AssetType;
  estimatedValue: number;
  planningValue: number;
  currency: string;
  valuedAt: string;
  liquidity: Liquidity;
  sourceType: SourceType;
  createdAt: string;
  updatedAt: string;
}

/** The asset's current recorded valuation plus its opaque revision, for a correction's conditional submission. */
export interface AssetValuationState {
  assetId: string;
  householdId: string;
  estimatedValue: string;
  planningValue: string;
  currency: string;
  valuedAt: string;
  sourceType: SourceType;
  revision: number;
}

export interface UpdateAssetValuationRequest {
  estimatedValue: string;
  planningValue: string;
  valuedAt: string;
  reason: string;
  expectedRevision: number;
}

export interface CreateLiabilityRequest {
  name: string;
  liabilityType: LiabilityType;
  outstandingBalance: string;
  currency: string;
  balanceAsOf: string;
}

export interface Liability {
  id: string;
  householdId: string;
  name: string;
  liabilityType: LiabilityType;
  outstandingBalance: number;
  currency: string;
  balanceAsOf: string;
  sourceType: SourceType;
  createdAt: string;
  updatedAt: string;
  revision: number;
}

export interface RecordLiabilityBalanceRequest {
  outstandingBalance: string;
  balanceAsOf: string;
  reason: string;
  expectedRevision: number;
}

export interface CreateIncomeStreamRequest {
  name: string;
  incomeType: IncomeType;
  amount: string;
  frequency: Frequency;
  currency: string;
  compensationClassification: CompensationClassification;
  certainty: IncomeCertainty;
  startDate: string;
  endDate: string | null;
}

export interface CreateObligationRequest {
  name: string;
  obligationType: ObligationType;
  amount: string;
  frequency: Frequency;
  currency: string;
  startDate: string;
  endDate: string | null;
}

export interface CreateGoalRequest {
  name: string;
  targetAmount: string;
  currency: string;
  targetDate: string;
  priority: number;
  currentAmount: string;
}

export interface CreateSnapshotRequest {
  asOfDate: string;
}

export interface CreatePlanningAssumptionRequest {
  name: string;
  value: string;
  valueType: string;
  notes: string | null;
  effectiveFrom: string;
  effectiveUntil: string | null;
  reviewDate: string;
}

export interface PlanningAssumption {
  id: string;
  householdId: string;
  name: string;
  value: string;
  valueType: string;
  notes: string | null;
  effectiveFrom: string;
  effectiveUntil: string | null;
  reviewDate: string;
  sourceType: SourceType;
  supersededBy: string | null;
  createdAt: string;
}
