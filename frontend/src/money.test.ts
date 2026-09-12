import { describe, expect, it } from 'vitest';
import { formatAmount, formatMoneyMagnitude, formatSignedMoney, isZeroMoney, parseMoney } from './money';

describe('money formatting', () => {
  it('groups an ordinary amount with thousands separators', () => {
    expect(formatMoneyMagnitude('1234.56')).toBe('1,234.56');
  });

  it('never loses precision on an amount far beyond Number.MAX_SAFE_INTEGER', () => {
    // 2^53 - 1 = 9,007,199,254,740,991; this value has 20 significant digits.
    expect(formatMoneyMagnitude('99999999999999999.99')).toBe('99,999,999,999,999,999.99');
    expect(formatMoneyMagnitude('100000000041999999.99')).toBe('100,000,000,041,999,999.99');
  });

  it('formats a small amount with no grouping needed', () => {
    expect(formatMoneyMagnitude('0.00')).toBe('0.00');
    expect(formatMoneyMagnitude('50.00')).toBe('50.00');
  });

  it('rejects a value that is not an exact two-decimal string', () => {
    expect(() => parseMoney('1234.5')).toThrow();
    expect(() => parseMoney('1234')).toThrow();
    expect(() => parseMoney('1.2345e10')).toThrow();
    expect(() => parseMoney('NaN')).toThrow();
  });

  it('formats a signed net worth without embedding the sign in the digits', () => {
    const negative = formatSignedMoney('-3200.00');
    expect(negative.negative).toBe(true);
    expect(negative.formatted).toBe('3,200.00');

    const positive = formatSignedMoney('4500.50');
    expect(positive.negative).toBe(false);
    expect(positive.formatted).toBe('4,500.50');
  });

  it('treats zero as non-negative', () => {
    const zero = formatSignedMoney('0.00');
    expect(zero.negative).toBe(false);
  });

  it('detects zero regardless of digit count', () => {
    expect(isZeroMoney('0.00')).toBe(true);
    expect(isZeroMoney('0000.00')).toBe(true);
    expect(isZeroMoney('0.01')).toBe(false);
    expect(isZeroMoney('1.00')).toBe(false);
  });
});

describe('formatAmount (plain JSON-number amounts, e.g. income-streams/obligations)', () => {
  it('groups an ordinary amount with thousands separators and two decimals', () => {
    expect(formatAmount(1234.56)).toBe('1,234.56');
    expect(formatAmount(50000)).toBe('50,000.00');
  });

  it('formats zero', () => {
    expect(formatAmount(0)).toBe('0.00');
  });

  it('rounds to two fractional digits', () => {
    expect(formatAmount(1234.5)).toBe('1,234.50');
    expect(formatAmount(1234.567)).toBe('1,234.57');
  });
});
