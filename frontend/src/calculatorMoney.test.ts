import { describe, expect, it } from 'vitest';
import { formatAmountMagnitude, formatSignedAmount, formatYearMonth } from './calculatorMoney';

describe('calculator amount formatting', () => {
  it('groups an ordinary amount with thousands separators', () => {
    expect(formatAmountMagnitude(1234.56)).toBe('1,234.56');
  });

  it('always shows exactly two fraction digits, even for a whole number', () => {
    expect(formatAmountMagnitude(200)).toBe('200.00');
  });

  it('formats a signed amount without embedding the sign in the digits', () => {
    const negative = formatSignedAmount(-200);
    expect(negative.negative).toBe(true);
    expect(negative.formatted).toBe('200.00');

    const positive = formatSignedAmount(800);
    expect(positive.negative).toBe(false);
    expect(positive.formatted).toBe('800.00');
  });

  it('treats zero as non-negative', () => {
    expect(formatSignedAmount(0).negative).toBe(false);
  });

  it('formats a YearMonth string as a short month and year', () => {
    expect(formatYearMonth('2027-01')).toBe('Jan 2027');
    expect(formatYearMonth('2027-12')).toBe('Dec 2027');
  });
});
