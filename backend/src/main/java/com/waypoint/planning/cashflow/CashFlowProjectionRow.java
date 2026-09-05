package com.waypoint.planning.cashflow;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One month of a constant monthly cash-flow projection.
 *
 * <p>Reconciles as {@code openingCash + inflow - outflow = closingCash}, and this row's
 * {@code closingCash} equals the next row's {@code openingCash}.
 */
public record CashFlowProjectionRow(
        YearMonth month,
        BigDecimal openingCash,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal netCashFlow,
        BigDecimal closingCash
) {
}
