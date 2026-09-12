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
