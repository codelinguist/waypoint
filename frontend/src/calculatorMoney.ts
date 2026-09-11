// Presentation-only formatting for the planning-calculator DTOs
// (src/api/types.ts: CashFlowProjection*/EmergencyFundRunway*), whose
// monetary fields arrive as JSON numbers rather than the exact decimal
// strings money.ts formats. See the comment on those types for why: unlike
// the position DTOs, these fields already pass through a JS `Number` during
// `response.json()` before any application code runs, so there is no exact
// string left to preserve by the time this module sees the value.

const AMOUNT = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Formats a magnitude (no sign), e.g. 1234.5 -> "1,234.50". */
export function formatAmountMagnitude(value: number): string {
  return AMOUNT.format(Math.abs(value));
}

/**
 * Formats a signed amount with an explicit "+"/"−" glyph ahead of the
 * magnitude, matching money.ts's `formatSignedMoney` convention: sign is
 * never conveyed by color alone. Zero is treated as non-negative ("+0.00").
 */
export function formatSignedAmount(value: number): { negative: boolean; formatted: string } {
  return { negative: value < 0, formatted: AMOUNT.format(Math.abs(value)) };
}

/** Formats a YearMonth string ("2027-01") as "Jan 2027". */
export function formatYearMonth(yearMonth: string): string {
  const [year, month] = yearMonth.split('-').map(Number);
  return new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', timeZone: 'UTC' }).format(
    new Date(Date.UTC(year, month - 1, 1))
  );
}
