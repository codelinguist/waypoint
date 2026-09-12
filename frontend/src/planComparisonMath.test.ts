import { describe, expect, it } from 'vitest';
import { centsToMoneyString, parseNonNegativeCents } from './planComparisonMath';

describe('parseNonNegativeCents', () => {
  it('parses whole numbers, one and two fraction digits', () => {
    expect(parseNonNegativeCents('1000')).toBe(100000n);
    expect(parseNonNegativeCents('1000.5')).toBe(100050n);
    expect(parseNonNegativeCents('1000.50')).toBe(100050n);
    expect(parseNonNegativeCents('0')).toBe(0n);
  });

  it('tolerates surrounding whitespace', () => {
    expect(parseNonNegativeCents('  42.10  ')).toBe(4210n);
  });

  it('rejects a negative value', () => {
    expect(parseNonNegativeCents('-1')).toBeNull();
  });

  it('rejects more than two fraction digits, empty input, and non-numeric text', () => {
    expect(parseNonNegativeCents('1.234')).toBeNull();
    expect(parseNonNegativeCents('')).toBeNull();
    expect(parseNonNegativeCents('abc')).toBeNull();
  });

  it('preserves a large amount exactly, with no floating-point precision loss', () => {
    expect(parseNonNegativeCents('99999999999999999.99')).toBe(9999999999999999999n);
  });
});

describe('centsToMoneyString', () => {
  it('formats positive, negative, and zero cents with a scale-2 fraction', () => {
    expect(centsToMoneyString(100050n)).toBe('1000.50');
    expect(centsToMoneyString(-100050n)).toBe('-1000.50');
    expect(centsToMoneyString(0n)).toBe('0.00');
  });

  it('pads a single-digit cents remainder', () => {
    expect(centsToMoneyString(105n)).toBe('1.05');
    expect(centsToMoneyString(5n)).toBe('0.05');
  });
});

describe('netWorth composition (assetTotal - liabilityTotal)', () => {
  it('computes an exact negative net worth when liabilities exceed assets', () => {
    const asset = parseNonNegativeCents('500.00')!;
    const liability = parseNonNegativeCents('750.25')!;
    expect(centsToMoneyString(asset - liability)).toBe('-250.25');
  });

  it('matches the backend invariant exactly for a large, precision-sensitive amount', () => {
    const asset = parseNonNegativeCents('99999999999999999.99')!;
    const liability = parseNonNegativeCents('0.01')!;
    expect(centsToMoneyString(asset - liability)).toBe('99999999999999999.98');
  });
});
