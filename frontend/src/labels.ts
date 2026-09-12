import type {
  AssetType,
  CompensationClassification,
  Frequency,
  IncomeCertainty,
  IncomeType,
  LiabilityType,
  Liquidity,
  ObligationType,
  SourceType,
} from './api/types';

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

const INCOME_TYPE_LABELS: Record<IncomeType, string> = {
  SALARY: 'Salary',
  HOURLY_CONTRACT: 'Hourly contract',
  BUSINESS_DISTRIBUTION: 'Business distribution',
  OTHER: 'Other',
};

const OBLIGATION_TYPE_LABELS: Record<ObligationType, string> = {
  HOUSEHOLD_BASELINE: 'Household baseline',
  MORTGAGE: 'Mortgage',
  LOAN_PAYMENT: 'Loan payment',
  INSURANCE: 'Insurance',
  TUITION: 'Tuition',
  TRAVEL_SINKING_FUND: 'Travel sinking fund',
  DISCRETIONARY: 'Discretionary',
  OTHER: 'Other',
};

const FREQUENCY_LABELS: Record<Frequency, string> = {
  HOURLY: 'Hourly',
  WEEKLY: 'Weekly',
  BIWEEKLY: 'Biweekly',
  MONTHLY: 'Monthly',
  ANNUAL: 'Annual',
};

const CERTAINTY_LABELS: Record<IncomeCertainty, string> = {
  CONFIRMED: 'Confirmed',
  EXPECTED: 'Expected',
  VARIABLE: 'Variable',
};

const COMPENSATION_CLASSIFICATION_LABELS: Record<CompensationClassification, string> = {
  GROSS: 'Gross',
  NET: 'Net',
  UNKNOWN: 'Unknown',
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

export function incomeTypeLabel(value: IncomeType): string {
  return INCOME_TYPE_LABELS[value] ?? titleCaseFallback(value);
}

export function obligationTypeLabel(value: ObligationType): string {
  return OBLIGATION_TYPE_LABELS[value] ?? titleCaseFallback(value);
}

export function frequencyLabel(value: Frequency): string {
  return FREQUENCY_LABELS[value] ?? titleCaseFallback(value);
}

export function certaintyLabel(value: IncomeCertainty): string {
  return CERTAINTY_LABELS[value] ?? titleCaseFallback(value);
}

export function compensationClassificationLabel(value: CompensationClassification): string {
  return COMPENSATION_CLASSIFICATION_LABELS[value] ?? titleCaseFallback(value);
}
