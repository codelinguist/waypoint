package com.waypoint.household;

import java.util.List;

/**
 * A snapshot header together with the copied line items and the per-currency
 * totals derived from them. Bundled here, rather than navigated as a lazy JPA
 * association, because {@code FinancialSnapshotService} loads it fully within
 * one transaction and hands it to the web layer to render.
 */
public record FinancialSnapshotDetail(
        FinancialSnapshot snapshot,
        List<SnapshotAssetLineItem> assetLineItems,
        List<SnapshotLiabilityLineItem> liabilityLineItems,
        List<CurrencyTotals> totalsByCurrency
) {
}
