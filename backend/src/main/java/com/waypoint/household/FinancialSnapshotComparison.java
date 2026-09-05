package com.waypoint.household;

import java.util.List;

/**
 * A read-only, unpersisted comparison of two {@link FinancialSnapshot}s
 * belonging to the same household. {@code laterSnapshot} is always treated as
 * the end point and {@code earlierSnapshot} as the start point, per the
 * caller's explicit direction — never inferred from {@code asOfDate}.
 */
public record FinancialSnapshotComparison(
        FinancialSnapshot earlierSnapshot,
        FinancialSnapshot laterSnapshot,
        List<CurrencyTotalsDelta> currencyDeltas
) {
}
