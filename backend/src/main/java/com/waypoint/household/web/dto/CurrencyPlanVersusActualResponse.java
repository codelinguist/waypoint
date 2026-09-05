package com.waypoint.household.web.dto;

import com.waypoint.household.CurrencyPlanVersusActual;

public record CurrencyPlanVersusActualResponse(
        String currency,
        VarianceResponse assetTotal,
        VarianceResponse liabilityTotal,
        VarianceResponse netWorth
) {

    public static CurrencyPlanVersusActualResponse from(CurrencyPlanVersusActual result) {
        return new CurrencyPlanVersusActualResponse(
                result.currency(),
                VarianceResponse.from(result.assetTotal()),
                VarianceResponse.from(result.liabilityTotal()),
                VarianceResponse.from(result.netWorth())
        );
    }
}
