package com.waypoint.household.web.dto;

import com.waypoint.household.CurrencyTotalsDelta;
import java.math.BigDecimal;

public record CurrencyTotalsDeltaResponse(
        String currency,
        BigDecimal assetTotalDelta,
        BigDecimal liabilityTotalDelta,
        BigDecimal netWorthDelta
) {

    public static CurrencyTotalsDeltaResponse from(CurrencyTotalsDelta delta) {
        return new CurrencyTotalsDeltaResponse(
                delta.currency(), delta.assetTotalDelta(), delta.liabilityTotalDelta(), delta.netWorthDelta());
    }
}
