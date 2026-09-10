package com.waypoint.position.web.dto;

import com.waypoint.household.CurrencyTotals;

public record PositionCurrencyTotalsResponse(
        String currency,
        String assetTotal,
        String liabilityTotal,
        String netWorth
) {

    public static PositionCurrencyTotalsResponse from(CurrencyTotals totals) {
        return new PositionCurrencyTotalsResponse(
                totals.currency(),
                MoneyFormat.plain(totals.assetTotal()),
                MoneyFormat.plain(totals.liabilityTotal()),
                MoneyFormat.plain(totals.netWorth())
        );
    }
}
