// Presentation formatting for the goals and goal-contribution-calculator
// endpoints, which serialize money as plain JSON numbers rather than the
// exact-decimal-string convention `money.ts` handles (see api/types.ts's
// comment on FinancialGoal/GoalContributionResult for why). By the time a
// value reaches this module it has already passed through `JSON.parse`, so
// no additional precision is recoverable here — this only formats for
// display and must not be used to format the string-typed position fields.

/** Formats a money number to two fraction digits with thousands grouping, e.g. 1234.5 -> "1,234.50". */
export function formatMoneyNumber(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Formats a percentage number to two fraction digits, e.g. 42.5 -> "42.50%". */
export function formatPercent(value: number): string {
  return `${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
}
