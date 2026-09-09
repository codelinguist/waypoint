package com.waypoint.planning.futurevalue;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic result of a compound-growth projection over explicit, caller-supplied inputs.
 * Nothing in this result is read from or written to canonical household state, and the inputs it
 * echoes are temporary modeling assumptions, not confirmed facts or a promised return.
 *
 * @param currency               normalized (uppercase) three-letter currency code echoed from the request
 * @param startingPrincipal      echoed starting principal
 * @param monthlyContribution    echoed equal monthly contribution, added at each month's end
 * @param annualRatePercentage   echoed nominal annual percentage rate assumption (e.g. {@code 12.00} means 12%)
 * @param projectionMonths       echoed number of projected months
 * @param endingValue            balance after the final projected month
 * @param totalContributed       {@code startingPrincipal + (monthlyContribution * projectionMonths)}
 * @param totalGrowth            {@code endingValue - totalContributed}
 * @param conventions            human-readable statement of the compounding/contribution/rounding conventions applied
 * @param schedule               deterministic, ordered month-by-month schedule
 */
public record FutureValueResult(
        String currency,
        BigDecimal startingPrincipal,
        BigDecimal monthlyContribution,
        BigDecimal annualRatePercentage,
        int projectionMonths,
        BigDecimal endingValue,
        BigDecimal totalContributed,
        BigDecimal totalGrowth,
        String conventions,
        List<FutureValueRow> schedule
) {
}
