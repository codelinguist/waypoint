package com.waypoint.household;

import java.util.List;

/**
 * A read-only, unpersisted comparison of one {@link FinancialSnapshot}'s
 * actual totals against caller-supplied planned totals for the same
 * currencies. Neither the plan nor this result is ever persisted.
 */
public record PlanVersusActualAnalysis(
        FinancialSnapshot snapshot,
        List<CurrencyPlanVersusActual> currencyResults
) {
}
