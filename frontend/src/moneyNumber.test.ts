import { describe, expect, it } from 'vitest';
import { formatMoneyNumber, formatPercent } from './moneyNumber';

describe('formatMoneyNumber', () => {
  it('groups the integer part and pads to two fraction digits', () => {
    expect(formatMoneyNumber(1234.5)).toBe('1,234.50');
  });

  it('renders zero as a two-decimal amount', () => {
    expect(formatMoneyNumber(0)).toBe('0.00');
  });

  it('keeps the sign for a negative amount (an overfunded goal\'s remaining amount)', () => {
    expect(formatMoneyNumber(-150)).toBe('-150.00');
  });
});

describe('formatPercent', () => {
  it('appends a percent sign and pads to two fraction digits', () => {
    expect(formatPercent(42.5)).toBe('42.50%');
  });

  it('renders a bounded 100 as a two-decimal amount', () => {
    expect(formatPercent(100)).toBe('100.00%');
  });
});
