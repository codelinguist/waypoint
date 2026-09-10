package com.waypoint.position;

import com.waypoint.household.Asset;
import com.waypoint.household.CurrencyTotals;
import com.waypoint.household.Household;
import com.waypoint.household.Liability;
import java.time.Instant;
import java.util.List;

/**
 * One coherent read of a household's current recorded position: the exact rows
 * the totals were derived from, plus retrieval metadata. Not a valuation
 * assertion and not persisted.
 */
public record FinancialPositionResult(
        Household household,
        List<Asset> assets,
        List<Liability> liabilities,
        List<CurrencyTotals> totalsByCurrency,
        Instant retrievedAt
) {
}
