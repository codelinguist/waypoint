import { describe, expect, it } from 'vitest';
import { formatSignedSnapshotAmount, formatSnapshotAmount } from './snapshotMoney';

describe('snapshot money formatting', () => {
  it('groups an ordinary amount with thousands separators', () => {
    expect(formatSnapshotAmount(1234.56)).toBe('1,234.56');
  });

  it('formats a small amount with no grouping needed', () => {
    expect(formatSnapshotAmount(0)).toBe('0.00');
    expect(formatSnapshotAmount(50)).toBe('50.00');
  });

  it('always shows exactly two fraction digits', () => {
    expect(formatSnapshotAmount(1234.5)).toBe('1,234.50');
    expect(formatSnapshotAmount(1234)).toBe('1,234.00');
  });

  it('formats a signed amount without embedding the sign in the digits', () => {
    const negative = formatSignedSnapshotAmount(-3200);
    expect(negative.negative).toBe(true);
    expect(negative.formatted).toBe('3,200.00');

    const positive = formatSignedSnapshotAmount(4500.5);
    expect(positive.negative).toBe(false);
    expect(positive.formatted).toBe('4,500.50');
  });

  it('treats zero as non-negative', () => {
    expect(formatSignedSnapshotAmount(0).negative).toBe(false);
  });
});
