// Presentation-only formatting for the financial-snapshots and
// snapshot-comparison endpoints, which serialize money as plain JSON numbers
// rather than the exact-decimal strings used by /financial-position (see
// ../money.ts and ../api/types.ts). That split is an existing, unchanged
// backend contract, not a new decision made here — this module only formats
// the JS numbers those endpoints already return.

const AMOUNT_FORMAT = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Formats a snapshot money amount's magnitude (no sign), e.g. -1234.5 -> "1,234.50". */
export function formatSnapshotAmount(value: number): string {
  return AMOUNT_FORMAT.format(Math.abs(value));
}

/**
 * Formats a signed snapshot money amount with an explicit, non-color
 * "+"/"−" glyph ahead of the magnitude (see .negative/.positive in
 * index.css) — the same sign convention used for financial position. Zero
 * is treated as non-negative ("+0.00").
 */
export function formatSignedSnapshotAmount(value: number): { negative: boolean; formatted: string } {
  return { negative: value < 0, formatted: AMOUNT_FORMAT.format(Math.abs(value)) };
}
