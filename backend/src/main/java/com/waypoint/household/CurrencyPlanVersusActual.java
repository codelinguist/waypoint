package com.waypoint.household;

/**
 * One currency's plan-versus-actual result: an asset, liability, and
 * net-worth {@link PlanVersusActualVariance} against the caller's explicit
 * plan for that currency.
 */
public record CurrencyPlanVersusActual(
        String currency,
        PlanVersusActualVariance assetTotal,
        PlanVersusActualVariance liabilityTotal,
        PlanVersusActualVariance netWorth
) {
}
