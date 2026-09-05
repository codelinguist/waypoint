package com.waypoint.household;

import java.math.BigDecimal;

/**
 * One currency's explicit, caller-supplied planned asset, liability, and
 * net-worth totals for a {@link PlanVersusActualAnalysis}. This is a
 * disposable analysis input, never a persisted planning entity or a fact.
 */
public record PlannedCurrencyTotals(
        String currency,
        BigDecimal assetTotal,
        BigDecimal liabilityTotal,
        BigDecimal netWorth
) {
}
