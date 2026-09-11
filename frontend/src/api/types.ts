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

// Mirrors backend/src/main/java/com/waypoint/household/web/dto/{FinancialSnapshot*,
// SnapshotAssetLineItem,SnapshotLiabilityLineItem,CurrencyTotals,
// CurrencyTotalsDelta}Response.java exactly.
//
// Unlike FinancialPositionResponse above, these endpoints serialize money as
// plain JSON numbers, not exact-decimal strings (an existing, unchanged
// contract — see agent/product/current-financial-position/api.md). Use
// ../snapshotMoney.ts to format these fields, never ../money.ts (which
// throws on a non-string input).

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

export interface SnapshotCurrencyTotals {
  currency: string;
  assetTotal: number;
  liabilityTotal: number;
  netWorth: number;
}

export interface FinancialSnapshot {
  id: string;
  householdId: string;
  asOfDate: string;
  capturedAt: string;
  sourceType: SourceType;
  assetLineItems: SnapshotAssetLineItem[];
  liabilityLineItems: SnapshotLiabilityLineItem[];
  totalsByCurrency: SnapshotCurrencyTotals[];
}

export interface FinancialSnapshotSummary {
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
  earlierSnapshot: FinancialSnapshotSummary;
  laterSnapshot: FinancialSnapshotSummary;
  currencyDeltas: SnapshotCurrencyTotalsDelta[];
}
