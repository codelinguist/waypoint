import type { AssetType, LiabilityType, Liquidity, SourceType, VarianceDirection } from './api/types';

const ASSET_TYPE_LABELS: Record<AssetType, string> = {
  CASH: 'Cash',
  BANK_ACCOUNT: 'Bank account',
  PROPERTY: 'Real estate',
  INVESTMENT: 'Investment',
  BUSINESS_OWNERSHIP: 'Business equity',
  OTHER: 'Other',
};

const LIABILITY_TYPE_LABELS: Record<LiabilityType, string> = {
  CREDIT_CARD: 'Credit card',
  MORTGAGE: 'Mortgage',
  PERSONAL_LOAN: 'Personal loan',
  BUSINESS_LOAN: 'Business loan',
  OTHER: 'Other',
};

const LIQUIDITY_LABELS: Record<Liquidity, string> = {
  LIQUID: 'Liquid',
  RESTRICTED: 'Restricted',
  ILLIQUID: 'Illiquid',
};

// Only MANUAL_ENTRY exists today; the fallback below keeps this forward
// compatible with a future imported source type without a frontend change
// being required before the backend adds one.
const SOURCE_TYPE_LABELS: Partial<Record<SourceType, string>> = {
  MANUAL_ENTRY: 'Manual',
};

// Deliberately neutral wording — matches VarianceDirection's backend javadoc:
// whether "above" or "below" plan is favorable depends on the measure and
// household priorities, which this label must not imply either way.
const VARIANCE_DIRECTION_LABELS: Record<VarianceDirection, string> = {
  ABOVE_PLAN: 'Above plan',
  BELOW_PLAN: 'Below plan',
  ON_PLAN: 'On plan',
};

function titleCaseFallback(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function assetTypeLabel(value: AssetType): string {
  return ASSET_TYPE_LABELS[value] ?? titleCaseFallback(value);
}

export function liabilityTypeLabel(value: LiabilityType): string {
  return LIABILITY_TYPE_LABELS[value] ?? titleCaseFallback(value);
}

export function liquidityLabel(value: Liquidity): string {
  return LIQUIDITY_LABELS[value] ?? titleCaseFallback(value);
}

export function sourceTypeLabel(value: SourceType): string {
  return SOURCE_TYPE_LABELS[value] ?? titleCaseFallback(value);
}

export function varianceDirectionLabel(value: VarianceDirection): string {
  return VARIANCE_DIRECTION_LABELS[value] ?? titleCaseFallback(value);
}
