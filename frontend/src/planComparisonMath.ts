// Exact-decimal arithmetic for user-entered planned totals (WAP-23). Mirrors
// money.ts's precision approach (BigInt, never Number) but runs the other
// direction: parsing raw, possibly-invalid form text into integer cents so
// netWorth = assetTotal - liabilityTotal can be computed exactly before the
// value ever passes through a floating-point Number. The backend's own
// PlanVersusActualService rejects a planned netWorth that doesn't exactly
// equal assetTotal minus liabilityTotal, so computing it this way here
// guarantees a submission never fails that check.

const NON_NEGATIVE_DECIMAL = /^\d+(\.\d{1,2})?$/;

/** Parses a non-negative decimal string (at most 2 fraction digits) to integer cents, or null if invalid. */
export function parseNonNegativeCents(value: string): bigint | null {
  const trimmed = value.trim();
  if (!NON_NEGATIVE_DECIMAL.test(trimmed)) return null;
  const [integerPart, fractionPart = ''] = trimmed.split('.');
  const paddedFraction = fractionPart.padEnd(2, '0');
  return BigInt(integerPart) * 100n + BigInt(paddedFraction);
}

/** Formats integer cents back to an exact decimal string ("-1234.56"), the format money.ts's parseMoney expects. */
export function centsToMoneyString(cents: bigint): string {
  const negative = cents < 0n;
  const magnitude = negative ? -cents : cents;
  const integerPart = magnitude / 100n;
  const fractionPart = (magnitude % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${integerPart}.${fractionPart}`;
}
