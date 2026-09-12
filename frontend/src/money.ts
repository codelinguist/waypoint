// Presentation-only exact-decimal-string formatting.
//
// The backend transports every monetary value as a decimal string
// (see docs: agent/product/current-financial-position/api.md) specifically
// because a JavaScript `Number` only has ~15-17 significant decimal digits
// of exact integer precision — parsing a large balance with `Number(...)` or
// `parseFloat(...)` can silently lose cents. This module never converts a
// money string through `Number`. Grouping the integer part uses `BigInt`,
// which has arbitrary precision and does not lose digits; the two-digit
// fractional part is handled as a literal string slice.

const MONEY_PATTERN = /^(-?)(\d+)\.(\d{2})$/;

export interface ParsedMoney {
  negative: boolean;
  integerPart: string;
  fractionPart: string;
}

export function parseMoney(value: string): ParsedMoney {
  const match = MONEY_PATTERN.exec(value);
  if (!match) {
    throw new Error(`Not an exact decimal money string: ${JSON.stringify(value)}`);
  }
  const [, sign, integerPart, fractionPart] = match;
  return { negative: sign === '-', integerPart, fractionPart };
}

/** Groups the integer part with thousands separators. Never touches the fraction digits. */
function groupIntegerPart(integerPart: string): string {
  return BigInt(integerPart).toLocaleString('en-US');
}

/** Formats a money string's magnitude (no sign), e.g. "1234.56" -> "1,234.56". */
export function formatMoneyMagnitude(value: string): string {
  const { integerPart, fractionPart } = parseMoney(value);
  return `${groupIntegerPart(integerPart)}.${fractionPart}`;
}

/**
 * Formats a signed headline amount (net worth) with an explicit, non-color
 * "+"/"−" glyph ahead of the magnitude, per the approved design
 * (agent/ui/financial-position/design-brief.md): sign is never conveyed by
 * color alone. Zero is treated as non-negative ("+0.00").
 */
export function formatSignedMoney(value: string): { negative: boolean; formatted: string } {
  const parsed = parseMoney(value);
  const magnitude = `${groupIntegerPart(parsed.integerPart)}.${parsed.fractionPart}`;
  return { negative: parsed.negative, formatted: magnitude };
}

/** True when a magnitude money string is exactly zero, regardless of sign. */
export function isZeroMoney(value: string): boolean {
  const { integerPart, fractionPart } = parseMoney(value);
  return /^0+$/.test(integerPart) && fractionPart === '00';
}
