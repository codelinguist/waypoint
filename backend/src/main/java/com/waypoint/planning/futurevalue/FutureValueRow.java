package com.waypoint.planning.futurevalue;

import java.math.BigDecimal;

/**
 * One month of a compound-growth projection.
 *
 * <p>Reconciles as {@code openingBalance + growth + contribution = closingBalance}.
 */
public record FutureValueRow(
        int month,
        BigDecimal openingBalance,
        BigDecimal growth,
        BigDecimal contribution,
        BigDecimal closingBalance
) {
}
