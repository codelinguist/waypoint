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
  error: 'HOUSEHOLD_NOT_FOUND' | 'MALFORMED_REQUEST' | string;
  message: string;
  details: unknown[];
}

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/{IncomeStreamResponse,ObligationResponse}.java.
// Unlike the position types above, `amount` here is a plain JSON number, not
// a decimal string — this API (agent/product/income-obligations/product-brief.md)
// predates the decimal-string transport convention. Use money.ts's
// `formatAmount` (not `formatMoneyMagnitude`) to present it.

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
