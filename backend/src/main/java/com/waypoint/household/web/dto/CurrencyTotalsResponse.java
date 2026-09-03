package com.waypoint.household.web.dto;

import com.waypoint.household.CurrencyTotals;
import java.math.BigDecimal;

public record CurrencyTotalsResponse(
        String currency,
        BigDecimal assetTotal,
        BigDecimal liabilityTotal,
        BigDecimal netWorth
) {

    public static CurrencyTotalsResponse from(CurrencyTotals totals) {
        return new CurrencyTotalsResponse(
                totals.currency(), totals.assetTotal(), totals.liabilityTotal(), totals.netWorth());
    }
}
