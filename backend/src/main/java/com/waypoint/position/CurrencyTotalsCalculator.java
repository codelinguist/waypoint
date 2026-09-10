package com.waypoint.position;

import com.waypoint.household.Asset;
import com.waypoint.household.CurrencyTotals;
import com.waypoint.household.Liability;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Pure per-currency total calculation over exactly the asset/liability rows
 * returned by a current-position read: no independent totals query, no
 * cross-currency aggregate, no rounding. Mirrors
 * {@code FinancialSnapshotService}'s per-currency reconciliation convention
 * (assetTotal = sum(value), liabilityTotal = sum(value), netWorth =
 * assetTotal - liabilityTotal, missing side is exact zero) without editing or
 * calling into that snapshot-only sibling, since this reads live {@link Asset}/
 * {@link Liability} rows rather than persisted snapshot line items.
 */
final class CurrencyTotalsCalculator {

    private CurrencyTotalsCalculator() {
    }

    static List<CurrencyTotals> compute(List<Asset> assets, List<Liability> liabilities) {
        Map<String, BigDecimal> assetTotals = new TreeMap<>();
        for (Asset asset : assets) {
            assetTotals.merge(asset.getCurrency(), asset.getPlanningValue(), BigDecimal::add);
        }
        Map<String, BigDecimal> liabilityTotals = new TreeMap<>();
        for (Liability liability : liabilities) {
            liabilityTotals.merge(liability.getCurrency(), liability.getOutstandingBalance(), BigDecimal::add);
        }

        TreeSet<String> currencies = new TreeSet<>();
        currencies.addAll(assetTotals.keySet());
        currencies.addAll(liabilityTotals.keySet());

        List<CurrencyTotals> totals = new ArrayList<>();
        for (String currency : currencies) {
            BigDecimal assetTotal = assetTotals.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal liabilityTotal = liabilityTotals.getOrDefault(currency, BigDecimal.ZERO);
            totals.add(new CurrencyTotals(currency, assetTotal, liabilityTotal, assetTotal.subtract(liabilityTotal)));
        }
        return totals;
    }
}
