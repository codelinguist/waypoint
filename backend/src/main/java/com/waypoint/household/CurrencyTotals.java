package com.waypoint.household;

import java.math.BigDecimal;

/**
 * Deterministic asset, liability, and net-worth totals for one currency
 * within a {@link FinancialSnapshot}. Currencies are never combined.
 */
public record CurrencyTotals(
        String currency,
        BigDecimal assetTotal,
        BigDecimal liabilityTotal,
        BigDecimal netWorth
) {
}
